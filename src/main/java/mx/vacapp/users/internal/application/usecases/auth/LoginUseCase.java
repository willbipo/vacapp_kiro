package mx.vacapp.users.internal.application.usecases.auth;

import mx.vacapp.users.internal.application.usecases.commands.AuthResult;
import mx.vacapp.users.internal.application.usecases.commands.LoginCommand;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.UserStatus;
import mx.vacapp.users.internal.domain.model.exceptions.AccountLockedException;
import mx.vacapp.users.internal.domain.model.exceptions.InactiveAccountException;
import mx.vacapp.users.internal.domain.model.exceptions.InvalidCredentialsException;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import mx.vacapp.users.internal.infrastructure.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caso de uso: Autenticar usuario con email y contraseña.
 * <p>
 * Este caso de uso implementa el flujo completo de autenticación:
 * <ul>
 *   <li>Buscar usuario por email</li>
 *   <li>Validar contraseña con BCrypt</li>
 *   <li>Verificar estado de la cuenta (ACTIVE, INACTIVE, LOCKED)</li>
 *   <li>Implementar rate limiting por IP (5 intentos por minuto)</li>
 *   <li>Implementar bloqueo de cuenta (5 intentos fallidos en 15 min)</li>
 *   <li>Generar token JWT con 24h de expiración</li>
 *   <li>Registrar auditoría del intento (exitoso o fallido)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Seguridad:</strong> Los mensajes de error no revelan si el email existe o no,
 * siempre retornan "Credenciales inválidas" para evitar enumeración de usuarios.
 * </p>
 * <p>
 * <strong>Rate Limiting:</strong> Se limitan los intentos de login por IP (5 por minuto)
 * y por email (5 intentos fallidos en 15 minutos antes de bloquear la cuenta).
 * </p>
 *
 * @see LoginCommand
 * @see AuthResult
 */
@Service
public class LoginUseCase {
    
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    // Rate limiting por IP: Map<IP, List<Instant de intentos>>
    private final Map<String, RateLimitData> ipRateLimitMap = new ConcurrentHashMap<>();
    
    // Seguimiento de intentos fallidos por email: Map<email, FailedAttemptsData>
    private final Map<String, FailedAttemptsData> failedAttemptsMap = new ConcurrentHashMap<>();
    
    // Constantes de configuración
    private static final int MAX_ATTEMPTS_PER_MINUTE_PER_IP = 5;
    private static final int MAX_FAILED_ATTEMPTS_PER_EMAIL = 5;
    private static final long RATE_LIMIT_WINDOW_MILLIS = 60_000; // 1 minuto
    private static final long ACCOUNT_LOCK_WINDOW_MILLIS = 15 * 60_000; // 15 minutos
    
    /**
     * Constructor con inyección de dependencias.
     *
     * @param userRepository repositorio de usuarios
     * @param auditRepository repositorio de auditoría
     * @param passwordEncoder encoder BCrypt para validar contraseñas
     * @param jwtTokenProvider proveedor de tokens JWT
     */
    public LoginUseCase(
            UserRepository userRepository,
            AuditRepository auditRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    /**
     * Ejecuta el caso de uso de login.
     * <p>
     * Valida credenciales, verifica estado de cuenta, aplica rate limiting,
     * genera JWT y registra auditoría.
     * </p>
     *
     * @param command comando con email, password, clientIp, userAgent
     * @return AuthResult con token JWT y datos del usuario
     * @throws InvalidCredentialsException si las credenciales son incorrectas
     * @throws InactiveAccountException si la cuenta está inactiva o bloqueada
     * @throws AccountLockedException si se excede el rate limit por IP
     */
    @Transactional(readOnly = true)
    public AuthResult execute(LoginCommand command) {
        // 1. Validar rate limiting por IP (5 intentos por minuto)
        checkIpRateLimit(command.clientIp());
        
        // 2. Validar formato de email (ya validado por Bean Validation en DTO)
        String email = command.email().toLowerCase().trim();
        
        // 3. Verificar si la cuenta está bloqueada temporalmente por intentos fallidos
        checkAccountLock(email);
        
        // 4. Buscar usuario por email
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                // Registrar intento fallido sin revelar que el email no existe
                auditRepository.logAuthentication(email, false, command.clientIp(), command.userAgent());
                recordFailedAttempt(email);
                return new InvalidCredentialsException("Credenciales inválidas");
            });
        
        // 5. Validar contraseña
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            // Registrar intento fallido
            auditRepository.logAuthentication(email, false, command.clientIp(), command.userAgent());
            recordFailedAttempt(email);
            throw new InvalidCredentialsException("Credenciales inválidas");
        }
        
        // 6. Verificar estado de la cuenta
        if (user.getStatus() == UserStatus.INACTIVE) {
            auditRepository.logAuthentication(email, false, command.clientIp(), command.userAgent());
            throw new InactiveAccountException("Cuenta inactiva");
        }
        
        if (user.getStatus() == UserStatus.LOCKED) {
            auditRepository.logAuthentication(email, false, command.clientIp(), command.userAgent());
            // Verificar si el bloqueo ha expirado
            FailedAttemptsData attemptsData = failedAttemptsMap.get(email);
            if (attemptsData != null) {
                long minutesRemaining = getRemainingLockMinutes(attemptsData.firstFailedAttempt);
                throw new AccountLockedException(
                    String.format("Cuenta bloqueada temporalmente por seguridad, intente en %d minutos", minutesRemaining)
                );
            }
            throw new AccountLockedException("Cuenta bloqueada temporalmente");
        }
        
        // 7. Login exitoso - limpiar intentos fallidos
        clearFailedAttempts(email);
        
        // 8. Generar JWT token
        String token = jwtTokenProvider.generateToken(user);
        
        // 9. Registrar autenticación exitosa
        auditRepository.logAuthentication(email, true, command.clientIp(), command.userAgent());
        
        // 10. Retornar resultado
        return new AuthResult(
            token,
            user.getUserId(),
            user.getEmail(),
            user.getName(),
            user.getRole().getValue(),
            user.getTenantId()
        );
    }
    
    /**
     * Verifica el rate limiting por IP.
     * <p>
     * Limita a 5 intentos de login por minuto desde la misma IP.
     * Si se excede el límite, lanza AccountLockedException.
     * </p>
     *
     * @param clientIp la dirección IP del cliente
     * @throws AccountLockedException si se excede el límite
     */
    private void checkIpRateLimit(String clientIp) {
        Instant now = Instant.now();
        
        // Obtener o crear datos de rate limit para esta IP
        RateLimitData rateLimitData = ipRateLimitMap.computeIfAbsent(
            clientIp,
            k -> new RateLimitData()
        );
        
        // Limpiar intentos antiguos (fuera de la ventana de 1 minuto)
        rateLimitData.attempts.removeIf(attempt -> 
            now.toEpochMilli() - attempt.toEpochMilli() > RATE_LIMIT_WINDOW_MILLIS
        );
        
        // Verificar si se excede el límite
        if (rateLimitData.attempts.size() >= MAX_ATTEMPTS_PER_MINUTE_PER_IP) {
            long oldestAttempt = rateLimitData.attempts.get(0).toEpochMilli();
            long secondsRemaining = (RATE_LIMIT_WINDOW_MILLIS - (now.toEpochMilli() - oldestAttempt)) / 1000;
            throw new AccountLockedException(
                String.format("Demasiados intentos, intente en %d segundos", secondsRemaining)
            );
        }
        
        // Registrar este intento
        rateLimitData.attempts.add(now);
    }
    
    /**
     * Verifica si una cuenta está bloqueada temporalmente por intentos fallidos.
     * <p>
     * Si la cuenta ha tenido 5 intentos fallidos en los últimos 15 minutos,
     * está bloqueada temporalmente. Si han pasado más de 15 minutos desde
     * el primer intento fallido, se limpia el bloqueo.
     * </p>
     *
     * @param email el email de la cuenta a verificar
     * @throws AccountLockedException si la cuenta está bloqueada y el período no ha expirado
     */
    private void checkAccountLock(String email) {
        FailedAttemptsData attemptsData = failedAttemptsMap.get(email);
        
        if (attemptsData == null) {
            return; // No hay intentos fallidos registrados
        }
        
        Instant now = Instant.now();
        long timeSinceFirstAttempt = now.toEpochMilli() - attemptsData.firstFailedAttempt.toEpochMilli();
        
        // Si han pasado más de 15 minutos desde el primer intento, limpiar el bloqueo
        if (timeSinceFirstAttempt > ACCOUNT_LOCK_WINDOW_MILLIS) {
            failedAttemptsMap.remove(email);
            return;
        }
        
        // Si hay 5 o más intentos fallidos dentro de la ventana de 15 minutos, bloquear
        if (attemptsData.count >= MAX_FAILED_ATTEMPTS_PER_EMAIL) {
            long minutesRemaining = getRemainingLockMinutes(attemptsData.firstFailedAttempt);
            throw new AccountLockedException(
                String.format("Cuenta bloqueada temporalmente por seguridad, intente en %d minutos", minutesRemaining)
            );
        }
    }
    
    /**
     * Registra un intento fallido de autenticación para un email.
     * <p>
     * Si se alcanzan 5 intentos fallidos en 15 minutos, la cuenta
     * se bloquea temporalmente.
     * </p>
     *
     * @param email el email de la cuenta
     */
    private void recordFailedAttempt(String email) {
        Instant now = Instant.now();
        
        FailedAttemptsData attemptsData = failedAttemptsMap.computeIfAbsent(
            email,
            k -> new FailedAttemptsData(now)
        );
        
        // Si han pasado más de 15 minutos desde el primer intento, reiniciar contador
        long timeSinceFirstAttempt = now.toEpochMilli() - attemptsData.firstFailedAttempt.toEpochMilli();
        if (timeSinceFirstAttempt > ACCOUNT_LOCK_WINDOW_MILLIS) {
            attemptsData.count = 1;
            attemptsData.firstFailedAttempt = now;
        } else {
            attemptsData.count++;
        }
    }
    
    /**
     * Limpia los intentos fallidos registrados para un email.
     * <p>
     * Se invoca cuando el login es exitoso.
     * </p>
     *
     * @param email el email de la cuenta
     */
    private void clearFailedAttempts(String email) {
        failedAttemptsMap.remove(email);
    }
    
    /**
     * Calcula los minutos restantes de bloqueo para una cuenta.
     *
     * @param firstFailedAttempt timestamp del primer intento fallido
     * @return minutos restantes de bloqueo
     */
    private long getRemainingLockMinutes(Instant firstFailedAttempt) {
        Instant now = Instant.now();
        long timeSinceFirstAttempt = now.toEpochMilli() - firstFailedAttempt.toEpochMilli();
        long remainingMillis = ACCOUNT_LOCK_WINDOW_MILLIS - timeSinceFirstAttempt;
        return Math.max(1, remainingMillis / 60_000); // Al menos 1 minuto
    }
    
    /**
     * Clase interna para almacenar datos de rate limiting por IP.
     */
    private static class RateLimitData {
        final java.util.List<Instant> attempts = new java.util.ArrayList<>();
    }
    
    /**
     * Clase interna para almacenar datos de intentos fallidos por email.
     */
    private static class FailedAttemptsData {
        int count;
        Instant firstFailedAttempt;
        
        FailedAttemptsData(Instant firstFailedAttempt) {
            this.count = 1;
            this.firstFailedAttempt = firstFailedAttempt;
        }
    }
}

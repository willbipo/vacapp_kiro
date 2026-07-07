package mx.vacapp.users.internal.application.usecases.auth;

import io.jsonwebtoken.Claims;
import mx.vacapp.users.internal.infrastructure.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: Validar token JWT y extraer información del usuario.
 * <p>
 * Este caso de uso proporciona funcionalidades para:
 * <ul>
 *   <li>Validar la firma y expiración de un token JWT</li>
 *   <li>Extraer información del usuario (userId, email, tenantId, roles)</li>
 *   <li>Verificar la integridad del token sin acceder a la base de datos</li>
 * </ul>
 * </p>
 * <p>
 * Este caso de uso es stateless y no requiere acceso a base de datos,
 * lo que permite validación rápida de tokens en cada request HTTP.
 * </p>
 * <p>
 * <strong>Uso típico:</strong> El JwtAuthenticationFilter utiliza este caso de uso
 * para validar el token en cada request y establecer el contexto de seguridad.
 * </p>
 *
 * @see JwtTokenProvider
 */
@Service
public class ValidateTokenUseCase {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * Constructor con inyección de dependencias.
     *
     * @param jwtTokenProvider proveedor de tokens JWT
     */
    public ValidateTokenUseCase(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    /**
     * Valida un token JWT verificando su firma, expiración y estructura.
     * <p>
     * Realiza las siguientes validaciones:
     * <ul>
     *   <li>Estructura correcta (header.payload.signature)</li>
     *   <li>Firma HMAC-SHA256 válida con la clave secreta</li>
     *   <li>Token no expirado (con tolerancia de 60 segundos)</li>
     *   <li>Presencia de claims obligatorios: sub, exp, iat, roles</li>
     * </ul>
     * </p>
     * <p>
     * Este método es eficiente y no requiere acceso a base de datos,
     * lo que lo hace ideal para validación en cada request.
     * </p>
     *
     * @param token el token JWT a validar
     * @return true si el token es válido, false en caso contrario
     */
    public boolean isValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        return jwtTokenProvider.validateToken(token.trim());
    }
    
    /**
     * Extrae el userId (UUID) del token JWT.
     * <p>
     * El userId está almacenado en el claim 'sub' (subject) del token.
     * </p>
     * <p>
     * <strong>Precondición:</strong> El token debe ser válido. Si el token es inválido,
     * este método retornará null.
     * </p>
     *
     * @param token el token JWT válido
     * @return el UUID del usuario, o null si el token es inválido
     */
    public UUID extractUserId(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        return jwtTokenProvider.extractUserId(token.trim());
    }
    
    /**
     * Extrae el tenantId (UUID) del token JWT.
     * <p>
     * El tenantId puede ser null para usuarios SaaS (super_admin, support).
     * Para usuarios de negocio (admin, manager, veterinarian, worker),
     * siempre estará presente.
     * </p>
     * <p>
     * <strong>Precondición:</strong> El token debe ser válido. Si el token es inválido,
     * este método retornará null.
     * </p>
     *
     * @param token el token JWT válido
     * @return el UUID del tenant, o null si es usuario SaaS o token inválido
     */
    public UUID extractTenantId(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        return jwtTokenProvider.extractTenantId(token.trim());
    }
    
    /**
     * Extrae los roles del token JWT.
     * <p>
     * Los roles están almacenados como un array de strings en el claim 'roles'.
     * En la implementación actual, cada usuario tiene exactamente un rol.
     * </p>
     * <p>
     * <strong>Precondición:</strong> El token debe ser válido. Si el token es inválido,
     * este método retornará una lista vacía.
     * </p>
     *
     * @param token el token JWT válido
     * @return lista de roles del usuario (típicamente con un único elemento),
     *         o lista vacía si el token es inválido
     */
    public List<String> extractRoles(String token) {
        if (token == null || token.trim().isEmpty()) {
            return List.of();
        }
        
        return jwtTokenProvider.extractRoles(token.trim());
    }
    
    /**
     * Extrae el email del token JWT.
     * <p>
     * El email está almacenado en el claim 'email' del token.
     * </p>
     * <p>
     * <strong>Precondición:</strong> El token debe ser válido. Si el token es inválido,
     * este método retornará null.
     * </p>
     *
     * @param token el token JWT válido
     * @return el email del usuario, o null si el token es inválido
     */
    public String extractEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        Claims claims = jwtTokenProvider.extractClaims(token.trim());
        if (claims == null) {
            return null;
        }
        
        return claims.get("email", String.class);
    }
    
    /**
     * Extrae todos los claims del token JWT.
     * <p>
     * Este método proporciona acceso completo a todos los datos del token,
     * útil para casos avanzados donde se necesita información adicional.
     * </p>
     * <p>
     * <strong>Precondición:</strong> El token debe ser válido. Si el token es inválido,
     * este método retornará null.
     * </p>
     *
     * @param token el token JWT válido
     * @return objeto Claims con todos los datos del token, o null si es inválido
     */
    public Claims extractAllClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        return jwtTokenProvider.extractClaims(token.trim());
    }
    
    /**
     * Resultado de validación de token con todos los datos extraídos.
     * <p>
     * Este record encapsula toda la información relevante del token
     * en una estructura inmutable y type-safe.
     * </p>
     *
     * @param valid indica si el token es válido
     * @param userId UUID del usuario (null si inválido)
     * @param email email del usuario (null si inválido)
     * @param tenantId UUID del tenant (null para usuarios SaaS o si inválido)
     * @param roles lista de roles del usuario (vacía si inválido)
     */
    public record TokenValidationResult(
        boolean valid,
        UUID userId,
        String email,
        UUID tenantId,
        List<String> roles
    ) {}
    
    /**
     * Valida un token JWT y extrae toda su información en una única operación.
     * <p>
     * Este método de conveniencia combina la validación y extracción de datos
     * en una sola llamada, útil cuando se necesita toda la información del token.
     * </p>
     * <p>
     * Es más eficiente que llamar individualmente a cada método de extracción,
     * ya que parsea el token una única vez.
     * </p>
     *
     * @param token el token JWT a validar y procesar
     * @return TokenValidationResult con todos los datos del token
     */
    public TokenValidationResult validateAndExtract(String token) {
        if (token == null || token.trim().isEmpty()) {
            return new TokenValidationResult(false, null, null, null, List.of());
        }
        
        String trimmedToken = token.trim();
        
        // Validar el token
        boolean valid = jwtTokenProvider.validateToken(trimmedToken);
        
        if (!valid) {
            return new TokenValidationResult(false, null, null, null, List.of());
        }
        
        // Extraer todos los datos en una única operación
        Claims claims = jwtTokenProvider.extractClaims(trimmedToken);
        
        if (claims == null) {
            return new TokenValidationResult(false, null, null, null, List.of());
        }
        
        UUID userId = jwtTokenProvider.extractUserId(trimmedToken);
        String email = claims.get("email", String.class);
        UUID tenantId = jwtTokenProvider.extractTenantId(trimmedToken);
        List<String> roles = jwtTokenProvider.extractRoles(trimmedToken);
        
        return new TokenValidationResult(valid, userId, email, tenantId, roles);
    }
}

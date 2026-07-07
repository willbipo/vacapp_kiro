package mx.vacapp.users.internal.application.usecases.user;

import mx.vacapp.users.internal.application.usecases.commands.UserResult;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Caso de uso: Listar usuarios con paginación.
 * <p>
 * Este caso de uso implementa la lógica de negocio para obtener una lista
 * paginada de usuarios del tenant actual, cumpliendo con:
 * </p>
 * <ul>
 *   <li>Paginación con máximo 50 usuarios por página (Requirement 6.1)</li>
 *   <li>Filtrado automático por tenant_id (Requirement 5.2)</li>
 *   <li>Retornar datos completos excepto passwordHash (Requirement 6.2)</li>
 * </ul>
 * <p>
 * El repositorio aplica filtrado automático por tenant_id del contexto de seguridad.
 * La paginación es 0-indexed (primera página es 0).
 * </p>
 * <p>
 * Metadatos de paginación (page, size, total) deben ser calculados y retornados
 * por el controlador usando el método count() del repositorio.
 * </p>
 *
 * @see UserResult
 */
@Service
public class ListUsersUseCase {
    
    /**
     * Tamaño máximo de página permitido según Requirements 6.1.
     * Garantiza rendimiento óptimo y previene sobrecarga del servidor.
     */
    private static final int MAX_PAGE_SIZE = 50;
    
    /**
     * Tamaño de página por defecto si no se especifica.
     */
    private static final int DEFAULT_PAGE_SIZE = 20;
    
    private final UserRepository userRepository;
    
    /**
     * Constructor con inyección de dependencias.
     *
     * @param userRepository repositorio de usuarios
     */
    public ListUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Ejecuta la obtención de una lista paginada de usuarios.
     * <p>
     * Valida los parámetros de paginación, aplica límites de seguridad,
     * y retorna la lista de usuarios del tenant actual.
     * </p>
     * <p>
     * El repositorio aplica automáticamente el filtro WHERE tenant_id = :currentTenantId
     * basado en el contexto de seguridad establecido por el JwtAuthenticationFilter.
     * </p>
     *
     * @param page número de página a recuperar (0-indexed, debe ser >= 0)
     * @param size número de usuarios por página (debe ser > 0 y <= 50)
     * @return lista de UserResult de la página solicitada, puede estar vacía
     * @throws IllegalArgumentException si page < 0 o size <= 0 o size > 50
     */
    @Transactional(readOnly = true)
    public List<UserResult> execute(int page, int size) {
        // 1. Validar parámetros de paginación
        if (page < 0) {
            throw new IllegalArgumentException("El número de página debe ser >= 0");
        }
        
        if (size <= 0) {
            throw new IllegalArgumentException("El tamaño de página debe ser > 0");
        }
        
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                String.format("El tamaño de página no puede exceder %d (máximo permitido)", MAX_PAGE_SIZE)
            );
        }
        
        // 2. Obtener usuarios del repositorio con paginación
        // El repositorio aplica filtrado automático por tenant_id del TenantContext
        List<User> users = userRepository.findAll(page, size);
        
        // 3. Convertir entidades de dominio a DTOs (sin passwordHash)
        // Stream API para transformación funcional
        return users.stream()
            .map(UserResult::fromDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene el total de usuarios en el tenant actual.
     * <p>
     * Este método es utilizado por los controladores para calcular metadatos
     * de paginación (total de páginas, total de registros, etc.).
     * </p>
     * <p>
     * El repositorio aplica filtrado automático por tenant_id.
     * </p>
     *
     * @return el número total de usuarios en el tenant actual (>= 0)
     */
    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }
}

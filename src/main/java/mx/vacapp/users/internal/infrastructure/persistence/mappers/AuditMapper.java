package mx.vacapp.users.internal.infrastructure.persistence.mappers;

import mx.vacapp.users.internal.infrastructure.persistence.entities.AuthLogEntity;
import mx.vacapp.users.internal.infrastructure.persistence.entities.UserAuditEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapper para facilitar la creación de entidades de auditoría.
 * <p>
 * Esta clase proporciona métodos de utilidad para construir instancias de entidades
 * de auditoría (UserAuditEntity y AuthLogEntity) de forma consistente y segura.
 * </p>
 * <p>
 * A diferencia de UserMapper, este mapper no transforma entre capas de dominio e infraestructura,
 * ya que las entidades de auditoría son exclusivamente de infraestructura y no tienen
 * contrapartes en la capa de dominio.
 * </p>
 * <p>
 * Propósitos:
 * <ul>
 *   <li>Centralizar la creación de registros de auditoría</li>
 *   <li>Garantizar que todos los campos obligatorios se establecen correctamente</li>
 *   <li>Aplicar validaciones básicas antes de persistir</li>
 *   <li>Generar UUIDs y timestamps automáticamente</li>
 * </ul>
 * </p>
 * <p>
 * Nota: Como las entidades usan solo @NoArgsConstructor disponible en tiempo de compilación,
 * este mapper construye las entidades creando nuevas instancias y poblando campos
 * individualmente, en lugar de usar constructores con argumentos.
 * </p>
 */
@Component
public class AuditMapper {
    
    /**
     * Crea una nueva entidad de auditoría de usuario (UserAuditEntity).
     * <p>
     * Este método construye un registro de auditoría para operaciones sobre usuarios
     * (CREATE, UPDATE, DEACTIVATE). Genera automáticamente un audit_id único y
     * establece el timestamp actual.
     * </p>
     *
     * @param userId el UUID del usuario que fue modificado
     * @param modifiedBy el UUID del usuario que realizó la modificación
     * @param operationType el tipo de operación (CREATE, UPDATE, DEACTIVATE)
     * @param oldValues JSON con los valores anteriores del usuario (null para CREATE)
     * @param newValues JSON con los valores nuevos del usuario
     * @return una nueva instancia de UserAuditEntity lista para persistir
     * @throws IllegalArgumentException si userId, modifiedBy, operationType o newValues son null
     */
    public UserAuditEntity createUserAudit(
            UUID userId,
            UUID modifiedBy,
            String operationType,
            String oldValues,
            String newValues) {
        
        // Validaciones
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }
        if (modifiedBy == null) {
            throw new IllegalArgumentException("modifiedBy no puede ser null");
        }
        if (operationType == null || operationType.isBlank()) {
            throw new IllegalArgumentException("operationType no puede ser null o vacío");
        }
        if (newValues == null || newValues.isBlank()) {
            throw new IllegalArgumentException("newValues no puede ser null o vacío");
        }
        
        // Crear manualmente la entidad utilizando constructor simple
        // y métodos de acceso generados por Lombok @Data
        UserAuditEntity entity = new UserAuditEntity();
        entity.setAuditId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setTimestamp(Instant.now());
        entity.setModifiedBy(modifiedBy);
        entity.setOperationType(operationType.toUpperCase());
        entity.setOldValues(oldValues);
        entity.setNewValues(newValues);
        
        return entity;
    }
    
    /**
     * Crea una nueva entidad de log de autenticación (AuthLogEntity).
     * <p>
     * Este método construye un registro de auditoría para intentos de autenticación
     * (login). Genera automáticamente un log_id único y establece el timestamp actual.
     * </p>
     *
     * @param email el email utilizado en el intento de autenticación
     * @param success true si la autenticación fue exitosa, false si falló
     * @param clientIp la dirección IP del cliente
     * @param userAgent el User-Agent del navegador/cliente
     * @return una nueva instancia de AuthLogEntity lista para persistir
     * @throws IllegalArgumentException si email, clientIp o userAgent son null/vacíos
     */
    public AuthLogEntity createAuthLog(
            String email,
            boolean success,
            String clientIp,
            String userAgent) {
        
        // Validaciones
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email no puede ser null o vacío");
        }
        if (clientIp == null || clientIp.isBlank()) {
            throw new IllegalArgumentException("clientIp no puede ser null o vacío");
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("userAgent no puede ser null o vacío");
        }
        
        // Truncar userAgent si excede el límite de la base de datos (500 chars)
        String truncatedUserAgent = userAgent.length() > 500 
            ? userAgent.substring(0, 500) 
            : userAgent;
        
        // Normalizar email a lowercase para consistencia en búsquedas
        String normalizedEmail = email.toLowerCase().trim();
        
        // Crear manualmente la entidad utilizando constructor simple
        // y métodos de acceso generados por Lombok @Data
        AuthLogEntity entity = new AuthLogEntity();
        entity.setLogId(UUID.randomUUID());
        entity.setEmail(normalizedEmail);
        entity.setTimestamp(Instant.now());
        entity.setSuccess(success);
        entity.setClientIp(clientIp.trim());
        entity.setUserAgent(truncatedUserAgent);
        
        return entity;
    }
}

package mx.vacapp.users.internal.domain.repository;

import java.util.UUID;

/**
 * Puerto de salida para operaciones de auditoría.
 * 
 * <p>Esta interfaz define los métodos necesarios para registrar eventos de auditoría
 * relacionados con la gestión de usuarios y autenticación en el sistema Vacapp.
 * Todos los logs de auditoría se retienen por un mínimo de 2 años para cumplir
 * con los requisitos de trazabilidad y cumplimiento normativo.</p>
 * 
 * <p>Esta es una interfaz pura de dominio sin anotaciones de Spring o JPA.
 * La implementación concreta se encuentra en la capa de infraestructura.</p>
 */
public interface AuditRepository {
    
    /**
     * Registra un intento de autenticación en el sistema.
     * 
     * <p>Se debe invocar este método cada vez que un usuario intenta autenticarse,
     * ya sea exitoso o fallido. Esto permite rastrear patrones de acceso, detectar
     * intentos de acceso no autorizado y cumplir con requisitos de auditoría de seguridad.</p>
     * 
     * <p>Los logs de autenticación se retienen por un mínimo de 2 años.</p>
     * 
     * @param email Email del usuario que intenta autenticarse
     * @param success true si la autenticación fue exitosa, false si falló
     * @param clientIp Dirección IP del cliente que realiza el intento de autenticación
     * @param userAgent User-Agent del navegador o cliente que realiza la petición
     */
    void logAuthentication(String email, boolean success, String clientIp, String userAgent);
    
    /**
     * Registra la creación de un nuevo usuario en el sistema.
     * 
     * <p>Se debe invocar este método inmediatamente después de que un nuevo usuario
     * sea creado exitosamente en la base de datos. Permite mantener un historial
     * completo de cuándo y quién creó cada usuario en el sistema.</p>
     * 
     * <p>Los logs de creación se retienen por un mínimo de 2 años.</p>
     * 
     * @param userId UUID del usuario que fue creado
     * @param createdBy UUID del usuario que ejecutó la operación de creación
     * @param oldValues Valores anteriores (típicamente null o cadena vacía para creaciones)
     * @param newValues Representación JSON o texto de los valores del nuevo usuario
     */
    void logUserCreation(UUID userId, UUID createdBy, String oldValues, String newValues);
    
    /**
     * Registra la actualización de datos de un usuario existente.
     * 
     * <p>Se debe invocar este método cada vez que se modifiquen campos de un usuario
     * (nombre, email, teléfono, rol, etc.). Permite rastrear todos los cambios realizados
     * a los datos de usuarios a lo largo del tiempo para auditoría y cumplimiento.</p>
     * 
     * <p>Los logs de actualización se retienen por un mínimo de 2 años.</p>
     * 
     * @param userId UUID del usuario que fue actualizado
     * @param updatedBy UUID del usuario que ejecutó la operación de actualización
     * @param oldValues Representación JSON o texto de los valores anteriores del usuario
     * @param newValues Representación JSON o texto de los nuevos valores del usuario
     */
    void logUserUpdate(UUID userId, UUID updatedBy, String oldValues, String newValues);
    
    /**
     * Registra la desactivación de un usuario en el sistema.
     * 
     * <p>Se debe invocar este método cuando un usuario es marcado como inactivo.
     * La desactivación es una operación crítica que requiere trazabilidad completa,
     * incluyendo quién la ejecutó y por qué motivo.</p>
     * 
     * <p>Los logs de desactivación se retienen por un mínimo de 2 años.</p>
     * 
     * @param userId UUID del usuario que fue desactivado
     * @param deactivatedBy UUID del usuario que ejecutó la operación de desactivación
     * @param reason Motivo textual de la desactivación (ej: "Fin de contrato", "Solicitud del usuario")
     */
    void logUserDeactivation(UUID userId, UUID deactivatedBy, String reason);
}

package mx.vacapp.cattle.internal.domain.repository;

import mx.vacapp.cattle.internal.domain.model.Animal;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Puerto de salida para persistencia de auditoría de ganado (Repository Port Pattern).
 * 
 * Esta es una interfaz pura de dominio sin anotaciones de Spring/JPA.
 * Define el contrato para operaciones de auditoría sobre cambios en el inventario
 * de ganado sin acoplarse a ninguna tecnología específica
 * (Clean Architecture / Hexagonal Architecture).
 * 
 * La implementación concreta estará en la capa de infraestructura
 * (internal/infrastructure/persistence/impl/CattleAuditRepositoryImpl.java)
 * 
 * IMPORTANTE: Registra todas las operaciones CREATE, UPDATE, CHANGE_STATUS,
 * MOVE_PASTURE sobre animales para garantizar trazabilidad completa y
 * cumplir con requisitos de auditoría.
 */
public interface CattleAuditRepository {
    
    /**
     * Registra un evento de auditoría genérico.
     * 
     * Este método es el núcleo del sistema de auditoría. Los métodos helper
     * específicos (logAnimalCreation, logAnimalUpdate, etc.) delegan en este método.
     * 
     * @param audit registro de auditoría completo
     * @throws IllegalArgumentException si el audit es null
     */
    void log(CattleAudit audit);
    
    /**
     * Registra la creación de un animal con su estado inicial completo.
     * 
     * @param animalId UUID del animal creado
     * @param animal entidad de dominio Animal con todos los datos iniciales
     * @param createdBy UUID del usuario que creó el animal
     * @throws IllegalArgumentException si algún parámetro requerido es null
     */
    void logAnimalCreation(UUID animalId, Animal animal, UUID createdBy);
    
    /**
     * Registra la actualización de datos de un animal.
     * 
     * Compara valores antiguos vs nuevos y registra solo los campos modificados
     * en formato JSON para auditoría eficiente.
     * 
     * @param animalId UUID del animal actualizado
     * @param oldValues mapa de campos anteriores (clave: nombre del campo, valor: valor anterior)
     * @param newValues mapa de campos nuevos (clave: nombre del campo, valor: valor nuevo)
     * @param modifiedBy UUID del usuario que realizó la actualización
     * @param tenantId UUID del tenant al que pertenece el animal
     * @throws IllegalArgumentException si algún parámetro requerido es null
     */
    void logAnimalUpdate(UUID animalId, Map<String, Object> oldValues, 
                         Map<String, Object> newValues, UUID modifiedBy, UUID tenantId);
    
    /**
     * Registra el cambio de estado de un animal.
     * 
     * Estados típicos: Activa → Vendida, Activa → Preñada, Preñada → Activa,
     * Activa → Muerta, etc.
     * 
     * @param animalId UUID del animal
     * @param oldStatus estado anterior (valor del enum CattleStatus)
     * @param newStatus estado nuevo (valor del enum CattleStatus)
     * @param modifiedBy UUID del usuario que realizó el cambio
     * @param reason razón opcional del cambio (max 500 caracteres)
     * @throws IllegalArgumentException si algún parámetro requerido es null
     */
    void logStatusChange(UUID animalId, String oldStatus, String newStatus, 
                         UUID modifiedBy, String reason);
    
    /**
     * Registra el movimiento de un animal entre potreros.
     * 
     * @param animalId UUID del animal movido
     * @param oldPotreroId UUID del potrero origen (puede ser null si es el primer potrero)
     * @param newPotreroId UUID del potrero destino
     * @param modifiedBy UUID del usuario que realizó el movimiento
     * @throws IllegalArgumentException si algún parámetro requerido es null
     */
    void logMovement(UUID animalId, UUID oldPotreroId, UUID newPotreroId, UUID modifiedBy);
    
    /**
     * Obtiene el historial de auditoría de un animal específico.
     * 
     * Retorna todos los registros de auditoría ordenados cronológicamente
     * de más reciente a más antiguo (timestamp DESC).
     * 
     * @param animalId UUID del animal
     * @param offset índice inicial para paginación (0-based)
     * @param limit cantidad máxima de registros a retornar
     * @return lista paginada de registros de auditoría
     * @throws IllegalArgumentException si offset < 0 o limit < 1
     */
    List<CattleAudit> findByAnimalId(UUID animalId, int offset, int limit);
    
    /**
     * Obtiene registros de auditoría filtrados por tipo de operación.
     * 
     * Útil para consultas específicas como "todas las ventas" o "todos los movimientos".
     * 
     * @param operationType tipo de operación (CREATE, UPDATE, CHANGE_STATUS, MOVE_PASTURE)
     * @param tenantId UUID del tenant
     * @param offset índice inicial para paginación (0-based)
     * @param limit cantidad máxima de registros a retornar
     * @return lista paginada de registros de auditoría
     * @throws IllegalArgumentException si algún parámetro requerido es null o límites inválidos
     */
    List<CattleAudit> findByOperationType(AuditOperationType operationType, UUID tenantId, 
                                          int offset, int limit);
    
    /**
     * Record que representa un registro de auditoría de ganado.
     * 
     * Record inmutable que transporta datos de auditoría entre capas.
     * Este record NO contiene anotaciones JPA - es una estructura pura de dominio.
     */
    record CattleAudit(
        UUID auditId,
        UUID animalId,
        AuditOperationType operationType,
        Instant timestamp,
        UUID modifiedBy,
        UUID tenantId,
        String oldValues,
        String newValues,
        String reason
    ) {}
    
    /**
     * Enum que representa los tipos de operaciones auditadas.
     * 
     * Este enum es parte de la capa de dominio y debe usarse en lugar de
     * strings mágicos para garantizar type-safety.
     */
    enum AuditOperationType {
        CREATE("create"),
        UPDATE("update"),
        CHANGE_STATUS("change_status"),
        MOVE_PASTURE("move_pasture"),
        DELETE("delete");
        
        private final String value;
        
        AuditOperationType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        /**
         * Convierte un string a AuditOperationType.
         * 
         * @param value valor del enum (case-insensitive)
         * @return el enum correspondiente
         * @throws IllegalArgumentException si el valor no es válido
         */
        public static AuditOperationType fromValue(String value) {
            if (value == null) {
                throw new IllegalArgumentException("Operation type value cannot be null");
            }
            
            for (AuditOperationType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            
            throw new IllegalArgumentException("Invalid operation type: " + value);
        }
    }
}

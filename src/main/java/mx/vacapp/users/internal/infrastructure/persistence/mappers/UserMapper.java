package mx.vacapp.users.internal.infrastructure.persistence.mappers;

import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.UserStatus;
import mx.vacapp.users.internal.infrastructure.persistence.entities.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper para transformar entre la capa de dominio (User) y la capa de persistencia (UserEntity).
 * <p>
 * Esta clase es responsable de convertir objetos User del dominio puro a entidades JPA UserEntity
 * y viceversa, garantizando la separación entre las capas de dominio e infraestructura
 * según los principios de Clean Architecture.
 * </p>
 * <p>
 * Las transformaciones incluyen:
 * <ul>
 *   <li>Conversión de enums Role ↔ String</li>
 *   <li>Conversión de enums UserStatus ↔ String</li>
 *   <li>Mapeo directo de UUIDs e Instants</li>
 *   <li>Mapeo de todos los campos de auditoría</li>
 * </ul>
 * </p>
 */
@Component
public class UserMapper {
    
    /**
     * Convierte una entidad de dominio User a una entidad JPA UserEntity.
     * <p>
     * Este método transforma un objeto User del dominio (POJO puro sin anotaciones JPA)
     * a un UserEntity que puede ser persistido por Spring Data JPA.
     * </p>
     * <p>
     * Transformaciones aplicadas:
     * <ul>
     *   <li>Role enum → String usando role.getValue()</li>
     *   <li>UserStatus enum → String usando status.getValue()</li>
     *   <li>Todos los demás campos se copian directamente (UUIDs, Strings, Instants)</li>
     * </ul>
     * </p>
     *
     * @param user la entidad de dominio User a convertir (no debe ser null)
     * @return una nueva instancia de UserEntity con todos los campos mapeados
     * @throws NullPointerException si user es null
     */
    public UserEntity toEntity(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User no puede ser null");
        }
        
        UserEntity entity = new UserEntity();
        entity.setUserId(user.getUserId());
        entity.setEmail(user.getEmail());
        entity.setName(user.getName());
        entity.setPhone(user.getPhone());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRole(user.getRole().getValue());           // Enum → String
        entity.setStatus(user.getStatus().getValue());       // Enum → String
        entity.setTenantId(user.getTenantId());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setCreatedBy(user.getCreatedBy());
        entity.setUpdatedBy(user.getUpdatedBy());
        
        return entity;
    }
    
    /**
     * Convierte una entidad JPA UserEntity a una entidad de dominio User.
     * <p>
     * Este método transforma un UserEntity recuperado de la base de datos
     * a un objeto User del dominio puro, listo para ser usado por los casos de uso.
     * </p>
     * <p>
     * Transformaciones aplicadas:
     * <ul>
     *   <li>String → Role enum usando Role.fromString()</li>
     *   <li>String → UserStatus enum usando UserStatus.fromString()</li>
     *   <li>Todos los demás campos se copian directamente (UUIDs, Strings, Instants)</li>
     * </ul>
     * </p>
     *
     * @param entity la entidad JPA UserEntity a convertir (no debe ser null)
     * @return una nueva instancia de User construida mediante el Builder
     * @throws IllegalArgumentException si entity es null, o si los valores de role o status no son válidos
     */
    public User toDomain(UserEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("UserEntity no puede ser null");
        }
        
        return new User.Builder()
            .userId(entity.getUserId())
            .email(entity.getEmail())
            .name(entity.getName())
            .phone(entity.getPhone())
            .passwordHash(entity.getPasswordHash())
            .role(Role.fromString(entity.getRole()))           // String → Enum
            .status(UserStatus.fromString(entity.getStatus())) // String → Enum
            .tenantId(entity.getTenantId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}

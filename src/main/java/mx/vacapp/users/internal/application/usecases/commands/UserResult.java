package mx.vacapp.users.internal.application.usecases.commands;

import mx.vacapp.users.internal.domain.model.User;
import java.util.UUID;

/**
 * Resultado inmutable que representa los datos de un usuario.
 * Record utilizado para retornar información de usuario desde casos de uso hacia controladores.
 * No incluye el passwordHash por seguridad.
 */
public record UserResult(
    UUID userId,
    String email,
    String name,
    String phone,
    String role,
    String status,
    UUID tenantId,
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
    /**
     * Método factory para crear un UserResult desde una entidad de dominio User.
     * Convierte el objeto de dominio en un DTO inmutable para la capa de aplicación.
     *
     * @param user la entidad de dominio User
     * @return un nuevo UserResult con los datos del usuario
     */
    public static UserResult fromDomain(User user) {
        return new UserResult(
            user.getUserId(),
            user.getEmail(),
            user.getName(),
            user.getPhone(),
            user.getRole().getValue(),
            user.getStatus().name(), // Use name() for uppercase to match OpenAPI spec
            user.getTenantId(),
            user.getCreatedAt().toString(),
            user.getUpdatedAt().toString(),
            user.getCreatedBy(),
            user.getUpdatedBy()
        );
    }
}

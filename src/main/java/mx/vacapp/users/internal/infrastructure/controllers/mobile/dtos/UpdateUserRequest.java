package mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO de solicitud para actualizar un usuario existente.
 * Todos los campos son opcionales (validaciones no requieren @NotNull).
 */
public record UpdateUserRequest(
    
    @Email(message = "Debe proporcionar un email válido")
    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    String email,
    
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String name,
    
    @Size(min = 7, max = 20, message = "El teléfono debe tener entre 7 y 20 dígitos")
    String phone,
    
    @Size(max = 50, message = "El rol no puede exceder 50 caracteres")
    String role
) {}

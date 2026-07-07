package mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de solicitud para autenticación de usuario.
 * Record inmutable con validaciones Bean Validation.
 */
public record LoginRequest(
    
    @NotNull(message = "El email es obligatorio")
    @Email(message = "Debe proporcionar un email válido")
    @Size(max = 254, message = "El email no puede exceder 254 caracteres")
    String email,
    
    @NotNull(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    String password
) {}

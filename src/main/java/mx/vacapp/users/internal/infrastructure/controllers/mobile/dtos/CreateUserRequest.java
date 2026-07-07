package mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de solicitud para crear un nuevo usuario.
 * Record inmutable con validaciones Bean Validation.
 */
public record CreateUserRequest(
    
    @NotNull(message = "El email es obligatorio")
    @Email(message = "Debe proporcionar un email válido")
    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    String email,
    
    @NotNull(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String name,
    
    @Size(min = 7, max = 20, message = "El teléfono debe tener entre 7 y 20 dígitos")
    String phone,
    
    @NotNull(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,128}$",
        message = "La contraseña debe tener 8-128 caracteres, una mayúscula, minúscula, dígito y carácter especial"
    )
    String password,
    
    @Size(max = 50, message = "El rol no puede exceder 50 caracteres")
    String role
) {}

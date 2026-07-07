package mx.vacapp.users.internal.infrastructure.controllers.web.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para formulario web de inicio de sesión.
 * Utilizado por controladores Thymeleaf para capturar datos del formulario de login.
 * Este DTO es inmutable y contiene validaciones Bean Validation.
 */
public record LoginFormDto(
    
    @NotNull(message = "El email es obligatorio")
    @Email(message = "Debe proporcionar un email válido")
    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    String email,
    
    @NotNull(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    String password,
    
    /**
     * Campo opcional para la funcionalidad "recordarme".
     * Representa una casilla de verificación en el formulario de login.
     */
    boolean rememberMe
) {}
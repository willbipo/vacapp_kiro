package mx.vacapp.geography.internal.infrastructure.controllers.web.dtos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de formulario para crear/editar una sección en la interfaz web.
 * Incluye validaciones Bean Validation para validación del lado del servidor.
 */
public record SeccionFormDto(
    
    UUID seccionId, // Nullable para creación, presente para edición
    
    @NotNull(message = "El rancho es obligatorio")
    UUID ranchoId,
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String nombre,
    
    @NotNull(message = "La superficie es obligatoria")
    @DecimalMin(value = "0.01", message = "La superficie debe ser mayor que 0")
    @DecimalMax(value = "999999999.99", message = "La superficie no puede exceder 999,999,999 m²")
    @Digits(integer = 9, fraction = 2, message = "La superficie debe tener máximo 9 dígitos enteros y 2 decimales")
    BigDecimal superficie,
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    String descripcion
) {
    
    /**
     * Constructor para crear formulario vacío.
     */
    public SeccionFormDto() {
        this(null, null, "", BigDecimal.ZERO, "");
    }
    
    /**
     * Constructor para edición con valores iniciales.
     */
    public SeccionFormDto(UUID seccionId, UUID ranchoId, String nombre, BigDecimal superficie, String descripcion) {
        this.seccionId = seccionId;
        this.ranchoId = ranchoId;
        this.nombre = nombre != null ? nombre.trim() : "";
        this.superficie = superficie != null ? superficie : BigDecimal.ZERO;
        this.descripcion = descripcion != null ? descripcion.trim() : "";
    }
}

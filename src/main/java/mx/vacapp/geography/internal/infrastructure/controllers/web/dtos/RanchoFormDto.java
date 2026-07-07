package mx.vacapp.geography.internal.infrastructure.controllers.web.dtos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de formulario para crear/editar un rancho en la interfaz web.
 * Incluye validaciones Bean Validation para validación del lado del servidor.
 */
public record RanchoFormDto(
    
    UUID ranchoId, // Nullable para creación, presente para edición
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String nombre,
    
    @NotNull(message = "La superficie total es obligatoria")
    @DecimalMin(value = "0.01", message = "La superficie debe ser mayor que 0")
    @DecimalMax(value = "999999999.99", message = "La superficie no puede exceder 999,999,999 m²")
    @Digits(integer = 9, fraction = 2, message = "La superficie debe tener máximo 9 dígitos enteros y 2 decimales")
    BigDecimal superficieTotal,
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    String descripcion
) {
    
    /**
     * Constructor para crear formulario vacío.
     */
    public RanchoFormDto() {
        this(null, "", BigDecimal.ZERO, "");
    }
    
    /**
     * Constructor para edición con valores iniciales.
     */
    public RanchoFormDto(UUID ranchoId, String nombre, BigDecimal superficieTotal, String descripcion) {
        this.ranchoId = ranchoId;
        this.nombre = nombre != null ? nombre.trim() : "";
        this.superficieTotal = superficieTotal != null ? superficieTotal : BigDecimal.ZERO;
        this.descripcion = descripcion != null ? descripcion.trim() : "";
    }
}

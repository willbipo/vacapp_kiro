package mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO Request para crear un nuevo potrero.
 * El potrero puede vincularse directamente a un rancho (seccionId = null)
 * o a una sección (seccionId != null).
 */
public record CreatePotreroRequest(
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String nombre,
    
    @DecimalMin(value = "0.01", message = "La superficie debe ser mayor que 0")
    @DecimalMax(value = "999999999", message = "La superficie debe ser menor o igual a 999,999,999")
    BigDecimal superficie,
    
    @NotNull(message = "El rancho es obligatorio")
    UUID ranchoId,
    
    UUID seccionId, // Nullable - opcional si vinculado directamente al rancho
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    String descripcion
) {
}

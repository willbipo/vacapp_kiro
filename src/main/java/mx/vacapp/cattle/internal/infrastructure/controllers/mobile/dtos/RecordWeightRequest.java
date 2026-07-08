package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import mx.vacapp.cattle.internal.application.usecases.commands.RecordWeightCommand;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de entrada (Request) para registrar un nuevo peso de un animal.
 * <p>
 * Este Record representa los datos que llegan desde la API REST móvil para registrar
 * un nuevo peso en el historial del animal. Incluye validaciones Bean Validation
 * para garantizar la integridad de los datos antes de procesarlos en la capa de aplicación.
 * </p>
 * 
 * <h2>Validaciones Aplicadas:</h2>
 * <ul>
 *   <li><strong>pesoKg</strong>: NOT NULL, debe ser mayor que cero (positivo)</li>
 *   <li><strong>fechaPesaje</strong>: NOT NULL, debe ser fecha pasada o presente (no futura)</li>
 *   <li><strong>notas</strong>: Opcional, max 500 caracteres</li>
 * </ul>
 * 
 * <h2>Flujo de Uso:</h2>
 * <pre>
 * 1. Cliente móvil envía POST /api/v1/cattle/{animalId}/weights con JSON
 * 2. Spring valida automáticamente con @Valid
 * 3. Controller extrae animalId del path parameter
 * 4. Controller mapea RecordWeightRequest → RecordWeightCommand usando toCommand(animalId, recordedBy)
 * 5. RecordWeightUseCase procesa el comando
 * </pre>
 * 
 * <h2>Ejemplo JSON:</h2>
 * <pre>
 * {
 *   "pesoKg": 450.50,
 *   "fechaPesaje": "2024-03-15",
 *   "notas": "Pesaje rutinario, animal en buen estado"
 * }
 * </pre>
 * 
 * <h2>Validaciones de Negocio (realizadas en el UseCase):</h2>
 * <ul>
 *   <li>El animal debe existir y pertenecer al tenant del usuario</li>
 *   <li>El animal NO puede tener status Vendida o Muerta</li>
 *   <li>La fecha de pesaje no puede ser futura</li>
 * </ul>
 * 
 * @param pesoKg Peso del animal en kilogramos (debe ser mayor que cero)
 * @param fechaPesaje Fecha en que se realizó el pesaje (no puede ser futura)
 * @param notas Observaciones libres sobre el pesaje (opcional, max 500 caracteres)
 * 
 * @see RecordWeightCommand
 * @see mx.vacapp.cattle.internal.application.usecases.weight.RecordWeightUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record RecordWeightRequest(
    
    @NotNull(message = "El peso es obligatorio")
    @Positive(message = "El peso debe ser mayor que cero")
    BigDecimal pesoKg,
    
    @NotNull(message = "La fecha de pesaje es obligatoria")
    @PastOrPresent(message = "La fecha de pesaje no puede ser futura")
    LocalDate fechaPesaje,
    
    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    String notas
    
) {
    
    /**
     * Convierte este DTO de Request a un comando de aplicación.
     * <p>
     * Este método transforma el DTO de infraestructura (RecordWeightRequest) en un comando
     * de la capa de aplicación (RecordWeightCommand), añadiendo los campos de contexto
     * que no vienen en el body del request pero son necesarios para el caso de uso:
     * </p>
     * 
     * <h3>Campos Adicionales:</h3>
     * <ul>
     *   <li><strong>animalId</strong>: Se extrae del path parameter de la URL (/api/v1/cattle/{animalId}/weights)</li>
     *   <li><strong>recordedBy</strong>: Se extrae del usuario autenticado actual (SecurityContext)</li>
     *   <li><strong>tenantId</strong>: Se extrae del contexto de seguridad (JWT) - multi-tenancy (agregado en el controller)</li>
     * </ul>
     * 
     * <h3>Uso en el Controller:</h3>
     * <pre>
     * RecordWeightCommand command = request.toCommand(animalId, recordedBy);
     * weightResult = recordWeightUseCase.execute(command);
     * </pre>
     * 
     * <h3>Nota Importante:</h3>
     * <p>
     * El animalId proviene del path parameter (no del body) porque estamos registrando
     * un peso para un animal específico cuya URL es /api/v1/cattle/{animalId}/weights.
     * Esto sigue el principio RESTful de recursos anidados.
     * </p>
     * 
     * @param animalId UUID del animal al que se registra el peso (extraído del path parameter)
     * @param recordedBy UUID del usuario que registra el peso (extraído del JWT)
     * @return RecordWeightCommand listo para ser procesado por el caso de uso (sin tenantId, se agrega en el controller)
     * 
     * @see RecordWeightCommand
     * @see mx.vacapp.cattle.internal.application.usecases.weight.RecordWeightUseCase
     */
    public RecordWeightCommand toCommand(UUID animalId, UUID recordedBy) {
        // Nota: tenantId se debe agregar en el controller desde el SecurityContext
        // Este método crea el comando base con los datos del request y los parámetros del controller
        return new RecordWeightCommand(
            animalId,
            pesoKg,
            fechaPesaje,
            notas,
            recordedBy,
            null  // tenantId debe ser asignado por el controller desde el contexto de seguridad
        );
    }
}

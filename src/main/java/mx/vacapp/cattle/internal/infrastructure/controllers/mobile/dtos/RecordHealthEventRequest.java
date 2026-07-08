package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import mx.vacapp.cattle.internal.application.usecases.commands.RecordHealthEventCommand;
import mx.vacapp.cattle.internal.domain.model.HealthEventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de entrada (Request) para registrar un evento de salud de un animal.
 * <p>
 * Este Record representa los datos que llegan desde la API REST móvil para registrar
 * eventos de salud como vacunaciones, desparasitaciones, tratamientos médicos, diagnósticos,
 * cirugías y revisiones. Incluye validaciones Bean Validation para garantizar la integridad
 * de los datos antes de procesarlos en la capa de aplicación.
 * </p>
 * 
 * <h2>Validaciones Aplicadas:</h2>
 * <ul>
 *   <li><strong>tipoEvento</strong>: NOT NULL (Vacunacion, Desparasitacion, Tratamiento, Diagnostico, Cirugia, Revision, Birth)</li>
 *   <li><strong>fecha</strong>: NOT NULL, debe ser fecha pasada o presente (no futura)</li>
 *   <li><strong>descripcion</strong>: NOT NULL, max 500 caracteres</li>
 *   <li><strong>medicamento</strong>: Opcional, max 200 caracteres</li>
 *   <li><strong>dosis</strong>: Opcional, max 100 caracteres</li>
 *   <li><strong>veterinario</strong>: Opcional, max 200 caracteres</li>
 *   <li><strong>proximaFecha</strong>: Opcional, debe ser fecha futura si se proporciona</li>
 *   <li><strong>costo</strong>: Opcional, debe ser >= 0 si se proporciona</li>
 *   <li><strong>observaciones</strong>: Opcional, max 1000 caracteres</li>
 * </ul>
 * 
 * <h2>Flujo de Uso:</h2>
 * <pre>
 * 1. Cliente móvil envía POST /api/v1/cattle/{animalId}/health con JSON
 * 2. Spring valida automáticamente con @Valid
 * 3. Controller mapea RecordHealthEventRequest → RecordHealthEventCommand usando toCommand()
 * 4. RecordHealthEventUseCase procesa el comando
 * </pre>
 * 
 * <h2>Ejemplo JSON (Vacunación):</h2>
 * <pre>
 * {
 *   "tipoEvento": "Vacunacion",
 *   "fecha": "2024-01-15",
 *   "descripcion": "Vacunación contra brucelosis",
 *   "medicamento": "Brucelosis Cepa 19",
 *   "dosis": "2ml subcutánea",
 *   "veterinario": "Dr. Juan Pérez",
 *   "proximaFecha": "2025-01-15",
 *   "costo": 350.00,
 *   "observaciones": "Animal respondió bien a la vacuna, sin reacciones adversas"
 * }
 * </pre>
 * 
 * <h2>Ejemplo JSON (Tratamiento):</h2>
 * <pre>
 * {
 *   "tipoEvento": "Tratamiento",
 *   "fecha": "2024-02-20",
 *   "descripcion": "Tratamiento antibiótico por infección respiratoria",
 *   "medicamento": "Oxitetraciclina LA",
 *   "dosis": "20mg/kg IM durante 5 días",
 *   "veterinario": "Dra. María González",
 *   "costo": 850.00,
 *   "observaciones": "Mejoría significativa al tercer día. Completar tratamiento"
 * }
 * </pre>
 * 
 * @param tipoEvento Tipo de evento de salud (requerido)
 * @param fecha Fecha en que ocurrió el evento (requerido, no puede ser futura)
 * @param descripcion Descripción del evento de salud (requerido, max 500 caracteres)
 * @param medicamento Nombre del medicamento o producto utilizado (opcional, max 200 caracteres)
 * @param dosis Dosis del medicamento administrado (opcional, max 100 caracteres)
 * @param veterinario Nombre del veterinario que atendió el evento (opcional, max 200 caracteres)
 * @param proximaFecha Fecha de próxima aplicación o seguimiento (opcional, debe ser futura)
 * @param costo Costo económico del evento en pesos (opcional, debe ser >= 0)
 * @param observaciones Observaciones adicionales sobre el evento (opcional, max 1000 caracteres)
 * 
 * @see RecordHealthEventCommand
 * @see mx.vacapp.cattle.internal.application.usecases.health.RecordHealthEventUseCase
 * @see mx.vacapp.cattle.internal.domain.model.HealthEvent
 * @see mx.vacapp.cattle.internal.domain.model.HealthEventType
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record RecordHealthEventRequest(
    
    @NotNull(message = "El tipo de evento es obligatorio")
    HealthEventType tipoEvento,
    
    @NotNull(message = "La fecha del evento es obligatoria")
    @PastOrPresent(message = "La fecha del evento no puede ser futura")
    LocalDate fecha,
    
    @NotNull(message = "La descripción es obligatoria")
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    String descripcion,
    
    @Size(max = 200, message = "El nombre del medicamento no puede exceder 200 caracteres")
    String medicamento,
    
    @Size(max = 100, message = "La dosis no puede exceder 100 caracteres")
    String dosis,
    
    @Size(max = 200, message = "El nombre del veterinario no puede exceder 200 caracteres")
    String veterinario,
    
    @Future(message = "La próxima fecha debe ser futura")
    LocalDate proximaFecha,
    
    @PositiveOrZero(message = "El costo debe ser mayor o igual a cero")
    BigDecimal costo,
    
    @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres")
    String observaciones
    
) {
    
    /**
     * Convierte este DTO de Request a un comando de aplicación.
     * <p>
     * Este método transforma el DTO de infraestructura (RecordHealthEventRequest) en un comando
     * de la capa de aplicación (RecordHealthEventCommand), añadiendo los campos de contexto
     * de seguridad (animalId y recordedBy) que se extraen del path param y SecurityContext.
     * </p>
     * 
     * <h3>Campos Contextuales:</h3>
     * <ul>
     *   <li><strong>animalId</strong>: Se extrae del path param /api/v1/cattle/{animalId}/health</li>
     *   <li><strong>recordedBy</strong>: Se extrae del usuario autenticado actual (JWT)</li>
     * </ul>
     * 
     * <h3>Uso:</h3>
     * <pre>
     * RecordHealthEventCommand command = request.toCommand(animalId, recordedBy);
     * healthEventResult = recordHealthEventUseCase.execute(command);
     * </pre>
     * 
     * @param animalId UUID del animal al que se registra el evento (extraído del path param)
     * @param recordedBy UUID del usuario que registra el evento (extraído del JWT)
     * @return RecordHealthEventCommand listo para ser procesado por el caso de uso
     * 
     * @see RecordHealthEventCommand
     * @see mx.vacapp.cattle.internal.application.usecases.health.RecordHealthEventUseCase
     */
    public RecordHealthEventCommand toCommand(UUID animalId, UUID recordedBy) {
        return new RecordHealthEventCommand(
            animalId,
            tipoEvento,
            fecha,
            descripcion,
            medicamento,
            dosis,
            veterinario,
            proximaFecha,
            costo,
            observaciones,
            recordedBy
        );
    }
}

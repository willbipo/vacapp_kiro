package mx.vacapp.cattle.internal.application.usecases.commands;

import mx.vacapp.cattle.internal.domain.model.HealthEvent;
import mx.vacapp.cattle.internal.domain.model.HealthEventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resultado inmutable que representa los datos completos de un evento de salud.
 * <p>
 * Record utilizado para retornar información de un evento de salud desde casos de uso hacia controladores.
 * Incluye todos los campos de la entidad de dominio HealthEvent.
 * </p>
 * 
 * <h2>Campos:</h2>
 * <ul>
 *   <li>{@code eventId} - Identificador único del evento de salud</li>
 *   <li>{@code animalId} - Identificador del animal</li>
 *   <li>{@code tipoEvento} - Tipo de evento (vaccination, treatment, birth, diagnosis)</li>
 *   <li>{@code fecha} - Fecha en que ocurrió el evento</li>
 *   <li>{@code descripcion} - Descripción del evento</li>
 *   <li>{@code medicamento} - Nombre del medicamento aplicado (opcional)</li>
 *   <li>{@code dosis} - Dosis del medicamento (opcional)</li>
 *   <li>{@code veterinario} - Nombre del veterinario que realizó el evento (opcional)</li>
 *   <li>{@code proximaFecha} - Fecha programada para próximo evento relacionado (opcional)</li>
 *   <li>{@code costo} - Costo económico del evento (opcional)</li>
 *   <li>{@code observaciones} - Observaciones adicionales del evento (opcional)</li>
 *   <li>{@code recordedAt} - Timestamp de cuando se registró el evento</li>
 *   <li>{@code recordedBy} - Usuario que registró el evento</li>
 * </ul>
 * 
 * <h2>Tipos de Eventos:</h2>
 * <ul>
 *   <li><b>vaccination</b>: Vacunación del animal</li>
 *   <li><b>treatment</b>: Tratamiento médico</li>
 *   <li><b>birth</b>: Evento de parto (el animal es la madre)</li>
 *   <li><b>diagnosis</b>: Diagnóstico veterinario</li>
 * </ul>
 * 
 * @param eventId Identificador único del evento de salud
 * @param animalId Identificador del animal
 * @param tipoEvento Tipo de evento (HealthEventType)
 * @param fecha Fecha en que ocurrió el evento
 * @param descripcion Descripción del evento
 * @param medicamento Nombre del medicamento aplicado (opcional)
 * @param dosis Dosis del medicamento (opcional)
 * @param veterinario Nombre del veterinario (opcional)
 * @param proximaFecha Fecha programada para próximo evento (opcional)
 * @param costo Costo económico del evento (opcional)
 * @param observaciones Observaciones adicionales (opcional)
 * @param recordedAt Timestamp de registro
 * @param recordedBy Usuario que registró el evento
 * 
 * @see HealthEvent
 * @see mx.vacapp.cattle.internal.application.usecases.health.RecordHealthEventUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record HealthEventResult(
    UUID eventId,
    UUID animalId,
    HealthEventType tipoEvento,
    LocalDate fecha,
    String descripcion,
    String medicamento,
    String dosis,
    String veterinario,
    LocalDate proximaFecha,
    BigDecimal costo,
    String observaciones,
    LocalDateTime recordedAt,
    UUID recordedBy
) {
    /**
     * Método factory para crear un HealthEventResult desde una entidad de dominio HealthEvent.
     * <p>
     * Convierte el objeto de dominio en un DTO inmutable para la capa de aplicación.
     * Todos los campos del evento son mapeados directamente, incluyendo los opcionales que pueden ser null.
     * </p>
     *
     * @param healthEvent la entidad de dominio HealthEvent
     * @return un nuevo HealthEventResult con todos los datos del evento
     * @throws NullPointerException si healthEvent es null
     */
    public static HealthEventResult fromDomain(HealthEvent healthEvent) {
        if (healthEvent == null) {
            throw new NullPointerException("healthEvent no puede ser null");
        }
        
        return new HealthEventResult(
            healthEvent.getEventId(),
            healthEvent.getAnimalId(),
            healthEvent.getTipoEvento(),
            healthEvent.getFecha(),
            healthEvent.getDescripcion(),
            healthEvent.getMedicamento(),
            healthEvent.getDosis(),
            healthEvent.getVeterinario(),
            healthEvent.getProximaFecha(),
            healthEvent.getCosto(),
            healthEvent.getObservaciones(),
            healthEvent.getRecordedAt(),
            healthEvent.getRecordedBy()
        );
    }
}

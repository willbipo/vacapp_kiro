package mx.vacapp.cattle.internal.application.usecases.commands;

import mx.vacapp.cattle.internal.domain.model.HealthEventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Comando que encapsula todos los datos necesarios para registrar un evento de salud de un animal.
 * <p>
 * Este comando es utilizado como entrada para el caso de uso {@code RecordHealthEventUseCase}.
 * Contiene todos los campos requeridos y opcionales para el registro de eventos de salud:
 * vacunaciones, desparasitaciones, tratamientos médicos, diagnósticos, cirugías y revisiones.
 * </p>
 * 
 * <h2>Campos Requeridos:</h2>
 * <ul>
 *   <li>{@code animalId} - Identificador del animal al que se registra el evento</li>
 *   <li>{@code tipoEvento} - Tipo de evento (Vacunacion, Desparasitacion, Tratamiento, Diagnostico, Cirugia, Revision)</li>
 *   <li>{@code fecha} - Fecha en que ocurrió el evento</li>
 *   <li>{@code descripcion} - Descripción del evento</li>
 *   <li>{@code recordedBy} - Identificador del usuario que registra el evento (extraído del contexto de seguridad)</li>
 * </ul>
 * 
 * <h2>Campos Opcionales:</h2>
 * <ul>
 *   <li>{@code medicamento} - Nombre del medicamento utilizado (opcional)</li>
 *   <li>{@code dosis} - Dosis del medicamento (opcional)</li>
 *   <li>{@code veterinario} - Nombre del veterinario que atendió el evento (opcional)</li>
 *   <li>{@code proximaFecha} - Fecha de próxima aplicación o seguimiento (opcional)</li>
 *   <li>{@code costo} - Costo económico del evento (opcional, debe ser >= 0)</li>
 *   <li>{@code observaciones} - Observaciones adicionales sobre el evento (opcional)</li>
 * </ul>
 * 
 * <h2>Tipos de Eventos:</h2>
 * <ul>
 *   <li><b>Vacunacion</b>: Vacunación del animal (preventivo)</li>
 *   <li><b>Desparasitacion</b>: Desparasitación del animal</li>
 *   <li><b>Tratamiento</b>: Tratamiento médico (curativo)</li>
 *   <li><b>Diagnostico</b>: Diagnóstico veterinario (evaluación de salud)</li>
 *   <li><b>Cirugia</b>: Intervención quirúrgica</li>
 *   <li><b>Revision</b>: Revisión general de salud</li>
 * </ul>
 * 
 * <h2>Validaciones:</h2>
 * <p>Las validaciones de negocio se realizan en el caso de uso:</p>
 * <ul>
 *   <li>El animal debe existir y pertenecer al tenant del usuario</li>
 *   <li>La fecha del evento no puede ser futura</li>
 *   <li>Si costo está presente, debe ser mayor o igual a cero</li>
 *   <li>La descripción es obligatoria</li>
 * </ul>
 * 
 * @param animalId Identificador del animal al que se registra el evento (NOT NULL)
 * @param tipoEvento Tipo de evento de salud (NOT NULL)
 * @param fecha Fecha en que ocurrió el evento (NOT NULL, no puede ser futura)
 * @param descripcion Descripción del evento (NOT NULL)
 * @param medicamento Nombre del medicamento utilizado (opcional)
 * @param dosis Dosis del medicamento (opcional)
 * @param veterinario Nombre del veterinario que atendió el evento (opcional)
 * @param proximaFecha Fecha de próxima aplicación o seguimiento (opcional)
 * @param costo Costo económico del evento (opcional, >= 0)
 * @param observaciones Observaciones adicionales sobre el evento (opcional)
 * @param recordedBy Identificador del usuario que registra el evento (NOT NULL)
 * 
 * @see mx.vacapp.cattle.internal.application.usecases.health.RecordHealthEventUseCase
 * @see mx.vacapp.cattle.internal.domain.model.HealthEvent
 * @see mx.vacapp.cattle.internal.domain.model.HealthEventType
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record RecordHealthEventCommand(
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
    UUID recordedBy
) {
}

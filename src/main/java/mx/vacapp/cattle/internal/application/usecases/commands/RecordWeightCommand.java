package mx.vacapp.cattle.internal.application.usecases.commands;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Comando que encapsula todos los datos necesarios para registrar un peso de un animal.
 * <p>
 * Este comando es utilizado como entrada para el caso de uso {@code RecordWeightUseCase}.
 * Contiene todos los campos requeridos y opcionales para el registro de un nuevo peso
 * en el historial del animal.
 * </p>
 * 
 * <h2>Campos Requeridos:</h2>
 * <ul>
 *   <li>{@code animalId} - Identificador del animal al que se registra el peso</li>
 *   <li>{@code pesoKg} - Peso en kilogramos (debe ser mayor que cero)</li>
 *   <li>{@code fechaPesaje} - Fecha en que se realizó el pesaje</li>
 *   <li>{@code recordedBy} - Identificador del usuario que registra el peso (extraído del contexto de seguridad)</li>
 *   <li>{@code tenantId} - Identificador del tenant (multi-tenancy)</li>
 * </ul>
 * 
 * <h2>Campos Opcionales:</h2>
 * <ul>
 *   <li>{@code notas} - Observaciones libres sobre el pesaje (máx 1000 caracteres)</li>
 * </ul>
 * 
 * <h2>Validaciones:</h2>
 * <p>Las validaciones de negocio se realizan en el caso de uso:</p>
 * <ul>
 *   <li>El animal debe existir y pertenecer al tenant del usuario</li>
 *   <li>El animal NO puede tener status Vendida o Muerta</li>
 *   <li>El peso debe ser mayor que cero</li>
 *   <li>La fecha de pesaje no puede ser futura</li>
 * </ul>
 * 
 * <h2>Proceso:</h2>
 * <ol>
 *   <li>Validar que el animal existe y pertenece al tenant</li>
 *   <li>Validar que el animal no está vendido o muerto</li>
 *   <li>Validar que el peso es mayor que cero</li>
 *   <li>Validar que la fecha de pesaje no es futura</li>
 *   <li>Crear el registro de peso</li>
 *   <li>Calcular ganancia diaria si existe un peso anterior</li>
 * </ol>
 * 
 * @param animalId Identificador del animal al que se registra el peso
 * @param pesoKg Peso en kilogramos (debe ser mayor que cero)
 * @param fechaPesaje Fecha en que se realizó el pesaje (no puede ser futura)
 * @param notas Observaciones libres sobre el pesaje (opcional)
 * @param recordedBy Identificador del usuario que registra el peso
 * @param tenantId Identificador del tenant (multi-tenancy)
 * 
 * @see mx.vacapp.cattle.internal.application.usecases.weight.RecordWeightUseCase
 * @see mx.vacapp.cattle.internal.domain.model.WeightRecord
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record RecordWeightCommand(
    UUID animalId,
    BigDecimal pesoKg,
    LocalDate fechaPesaje,
    String notas,
    UUID recordedBy,
    UUID tenantId
) {
}

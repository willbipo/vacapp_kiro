package mx.vacapp.cattle.internal.application.usecases.commands;

import mx.vacapp.cattle.internal.domain.model.WeightRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resultado inmutable que representa los datos de un registro de peso.
 * <p>
 * Record utilizado para retornar información de un peso desde casos de uso hacia controladores.
 * Incluye todos los campos del dominio WeightRecord más los campos calculados ganancia_diaria
 * y diasDesdeUltimoPesaje.
 * </p>
 * 
 * <h2>Campos:</h2>
 * <ul>
 *   <li>{@code weightId} - Identificador único del registro de peso</li>
 *   <li>{@code animalId} - Identificador del animal</li>
 *   <li>{@code pesoKg} - Peso en kilogramos</li>
 *   <li>{@code fechaPesaje} - Fecha del pesaje</li>
 *   <li>{@code ganancia_diaria} - Ganancia diaria en kg/día (calculada desde el peso anterior)</li>
 *   <li>{@code diasDesdeUltimoPesaje} - Días transcurridos desde el pesaje anterior</li>
 *   <li>{@code notas} - Observaciones opcionales</li>
 *   <li>{@code recordedAt} - Timestamp de creación del registro</li>
 *   <li>{@code recordedBy} - Usuario que creó el registro</li>
 * </ul>
 * 
 * <h2>Cálculo de Ganancia Diaria:</h2>
 * <p>
 * La ganancia diaria se calcula como:
 * <pre>
 * ganancia_diaria = (peso_actual - peso_anterior) / días_entre_pesajes
 * </pre>
 * </p>
 * <p>
 * Si no existe un peso anterior, ganancia_diaria y diasDesdeUltimoPesaje serán {@code null}.
 * </p>
 * 
 * @param weightId Identificador único del registro de peso
 * @param animalId Identificador del animal
 * @param pesoKg Peso en kilogramos
 * @param fechaPesaje Fecha del pesaje
 * @param ganancia_diaria Ganancia diaria en kg/día (null si es el primer peso)
 * @param diasDesdeUltimoPesaje Días transcurridos desde el último pesaje (null si es el primer peso)
 * @param notas Observaciones opcionales
 * @param recordedAt Timestamp de creación del registro
 * @param recordedBy Usuario que creó el registro
 * 
 * @see WeightRecord
 * @see mx.vacapp.cattle.internal.application.usecases.weight.RecordWeightUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record WeightResult(
    UUID weightId,
    UUID animalId,
    BigDecimal pesoKg,
    LocalDate fechaPesaje,
    BigDecimal ganancia_diaria,
    Integer diasDesdeUltimoPesaje,
    String notas,
    LocalDateTime recordedAt,
    UUID recordedBy
) {
    /**
     * Método factory para crear un WeightResult desde una entidad de dominio WeightRecord.
     * <p>
     * Convierte el objeto de dominio en un DTO inmutable para la capa de aplicación.
     * La ganancia diaria y los días desde el último pesaje deben ser calculados externamente
     * y pasados como parámetros.
     * </p>
     *
     * @param weightRecord la entidad de dominio WeightRecord
     * @param ganancia ganancia diaria calculada (puede ser null si es el primer peso)
     * @param dias días transcurridos desde el último pesaje (puede ser null si es el primer peso)
     * @return un nuevo WeightResult con los datos del peso y los cálculos
     */
    public static WeightResult fromDomain(WeightRecord weightRecord, BigDecimal ganancia, Integer dias) {
        return new WeightResult(
            weightRecord.getWeightId(),
            weightRecord.getAnimalId(),
            weightRecord.getPesoKg(),
            weightRecord.getFechaPesaje(),
            ganancia,
            dias,
            weightRecord.getNotas(),
            LocalDateTime.ofInstant(weightRecord.getCreatedAt(), java.time.ZoneId.systemDefault()),
            weightRecord.getCreatedBy()
        );
    }
    
    /**
     * Método factory para crear un WeightResult desde una entidad de dominio WeightRecord
     * sin ganancia diaria ni días calculados.
     * <p>
     * Útil para el primer peso registrado de un animal, donde no hay peso anterior para comparar.
     * </p>
     *
     * @param weightRecord la entidad de dominio WeightRecord
     * @return un nuevo WeightResult con ganancia_diaria = null y diasDesdeUltimoPesaje = null
     */
    public static WeightResult fromDomain(WeightRecord weightRecord) {
        return fromDomain(weightRecord, null, null);
    }
}

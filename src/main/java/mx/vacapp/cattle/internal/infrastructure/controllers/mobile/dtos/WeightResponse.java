package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import mx.vacapp.cattle.internal.application.usecases.commands.WeightResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida (Response) para representar un registro de peso de un animal bovino.
 * <p>
 * Este Record representa los datos que se retornan desde la API REST móvil cuando se consulta
 * o registra un peso de un animal. Incluye el peso registrado, fecha de pesaje, y la ganancia
 * diaria calculada comparada con el peso anterior del mismo animal.
 * </p>
 * 
 * <h2>Campos Incluidos:</h2>
 * <ul>
 *   <li><strong>weightId</strong>: Identificador único del registro de peso (UUID)</li>
 *   <li><strong>animalId</strong>: Identificador del animal (UUID)</li>
 *   <li><strong>pesoKg</strong>: Peso registrado en kilogramos (BigDecimal)</li>
 *   <li><strong>fechaPesaje</strong>: Fecha en que se realizó el pesaje (LocalDate)</li>
 *   <li><strong>ganancia_diaria</strong>: Ganancia diaria en kg/día calculada desde el peso anterior (opcional)</li>
 *   <li><strong>diasDesdeUltimoPesaje</strong>: Días transcurridos desde el último peso registrado (opcional)</li>
 *   <li><strong>notas</strong>: Observaciones o notas adicionales del pesaje (opcional)</li>
 *   <li><strong>recordedAt</strong>: Timestamp de cuándo se creó el registro (LocalDateTime)</li>
 * </ul>
 * 
 * <h2>Cálculo de Ganancia Diaria:</h2>
 * <p>
 * La ganancia diaria se calcula como:
 * <pre>
 * ganancia_diaria = (peso_actual - peso_anterior) / días_entre_pesajes
 * </pre>
 * Si este es el primer peso registrado del animal, tanto ganancia_diaria como diasDesdeUltimoPesaje
 * serán {@code null}.
 * </p>
 * 
 * <h2>Flujo de Uso:</h2>
 * <pre>
 * 1. UseCase retorna WeightResult con ganancia calculada
 * 2. Controller convierte a WeightResponse usando fromResult()
 * 3. Spring serializa a JSON y retorna al cliente móvil
 * </pre>
 * 
 * <h2>Ejemplo JSON:</h2>
 * <pre>
 * {
 *   "weightId": "550e8400-e29b-41d4-a716-446655440000",
 *   "animalId": "660e8400-e29b-41d4-a716-446655440001",
 *   "pesoKg": 385.50,
 *   "fechaPesaje": "2024-01-15",
 *   "ganancia_diaria": 0.85,
 *   "diasDesdeUltimoPesaje": 30,
 *   "notas": "Animal en buena condición",
 *   "recordedAt": "2024-01-15T14:30:00"
 * }
 * </pre>
 * 
 * <h2>Caso Primer Peso (sin peso anterior):</h2>
 * <pre>
 * {
 *   "weightId": "550e8400-e29b-41d4-a716-446655440000",
 *   "animalId": "660e8400-e29b-41d4-a716-446655440001",
 *   "pesoKg": 350.00,
 *   "fechaPesaje": "2023-12-15",
 *   "ganancia_diaria": null,
 *   "diasDesdeUltimoPesaje": null,
 *   "notas": "Primer peso al ingresar al rancho",
 *   "recordedAt": "2023-12-15T10:00:00"
 * }
 * </pre>
 * 
 * @param weightId Identificador único del registro de peso
 * @param animalId Identificador del animal al que pertenece el peso
 * @param pesoKg Peso registrado en kilogramos (debe ser > 0)
 * @param fechaPesaje Fecha en que se realizó el pesaje (no puede ser futura)
 * @param ganancia_diaria Ganancia diaria en kg/día desde el peso anterior (null si es el primer peso)
 * @param diasDesdeUltimoPesaje Días transcurridos desde el último pesaje (null si es el primer peso)
 * @param notas Observaciones opcionales sobre el pesaje (máximo 2000 caracteres)
 * @param recordedAt Timestamp de creación del registro en el sistema
 * 
 * @see WeightResult
 * @see mx.vacapp.cattle.internal.application.usecases.weight.RecordWeightUseCase
 * @see mx.vacapp.cattle.internal.application.usecases.weight.GetWeightHistoryUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record WeightResponse(
    UUID weightId,
    UUID animalId,
    BigDecimal pesoKg,
    LocalDate fechaPesaje,
    BigDecimal ganancia_diaria,
    Integer diasDesdeUltimoPesaje,
    String notas,
    LocalDateTime recordedAt
) {
    
    /**
     * Método factory para crear un WeightResponse desde un WeightResult de la capa de aplicación.
     * <p>
     * Este método transforma el resultado de un caso de uso (WeightResult) en un DTO de respuesta
     * de infraestructura (WeightResponse) listo para ser serializado a JSON y retornado al cliente móvil.
     * </p>
     * 
     * <h3>Conversión de Campos:</h3>
     * <ul>
     *   <li>Todos los campos se mapean directamente del WeightResult al WeightResponse</li>
     *   <li>El campo recordedBy del WeightResult no se incluye en la respuesta (información interna)</li>
     *   <li>La ganancia_diaria y diasDesdeUltimoPesaje pueden ser null si es el primer peso</li>
     *   <li>El recordedAt se incluye para trazabilidad del registro</li>
     * </ul>
     * 
     * <h3>Uso en Controller:</h3>
     * <pre>
     * WeightResult result = recordWeightUseCase.execute(command);
     * WeightResponse response = WeightResponse.fromResult(result);
     * return ResponseEntity.status(201).body(response);
     * </pre>
     * 
     * <h3>Uso en Listado de Pesos:</h3>
     * <pre>
     * List&lt;WeightResult&gt; results = getWeightHistoryUseCase.execute(animalId);
     * List&lt;WeightResponse&gt; responses = results.stream()
     *     .map(WeightResponse::fromResult)
     *     .toList();
     * return ResponseEntity.ok(responses);
     * </pre>
     * 
     * @param result WeightResult retornado desde la capa de aplicación
     * @return WeightResponse listo para ser serializado a JSON
     * @throws NullPointerException si result es null
     * 
     * @see WeightResult
     * @see WeightResult#fromDomain(mx.vacapp.cattle.internal.domain.model.WeightRecord, BigDecimal, Integer)
     */
    public static WeightResponse fromResult(WeightResult result) {
        return new WeightResponse(
            result.weightId(),
            result.animalId(),
            result.pesoKg(),
            result.fechaPesaje(),
            result.ganancia_diaria(),
            result.diasDesdeUltimoPesaje(),
            result.notas(),
            result.recordedAt()
        );
    }
}

package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import mx.vacapp.cattle.internal.application.usecases.commands.PastureHistoryResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * DTO de salida (Response) para historial de movimientos de animales entre potreros.
 * <p>
 * Este Record representa los datos que se retornan desde la API REST móvil cuando se consulta
 * el historial de movimientos de un animal entre diferentes potreros. Incluye información
 * del potrero, fechas de entrada/salida, y días de permanencia calculados.
 * </p>
 * 
 * <h2>Campos:</h2>
 * <ul>
 *   <li><strong>historyId</strong>: Identificador único del registro de historial</li>
 *   <li><strong>animalId</strong>: Identificador del animal</li>
 *   <li><strong>potreroId</strong>: Identificador del potrero</li>
 *   <li><strong>potreroNombre</strong>: Nombre del potrero para mostrar en UI</li>
 *   <li><strong>fechaIngreso</strong>: Fecha de entrada al potrero</li>
 *   <li><strong>fechaSalida</strong>: Fecha de salida del potrero (null = ubicación actual)</li>
 *   <li><strong>diasEnPotrero</strong>: Días de permanencia calculados</li>
 *   <li><strong>movedBy</strong>: Usuario que registró el movimiento</li>
 *   <li><strong>createdAt</strong>: Timestamp de creación del registro</li>
 * </ul>
 * 
 * <h2>Ubicación Actual vs Histórico:</h2>
 * <p>
 * Un registro con fechaSalida = null representa la ubicación actual del animal.
 * Los registros con fechaSalida != null representan ubicaciones históricas.
 * </p>
 * 
 * <h2>Flujo de Uso:</h2>
 * <pre>
 * 1. GetMovementHistoryUseCase retorna List&lt;PastureHistoryResult&gt;
 * 2. Controller mapea cada PastureHistoryResult → PastureHistoryResponse usando fromResult()
 * 3. API REST retorna JSON con historial completo de movimientos
 * </pre>
 * 
 * <h2>Ejemplo JSON:</h2>
 * <pre>
 * {
 *   "historyId": "550e8400-e29b-41d4-a716-446655440000",
 *   "animalId": "550e8400-e29b-41d4-a716-446655440001",
 *   "potreroId": "550e8400-e29b-41d4-a716-446655440002",
 *   "potreroNombre": "Potrero Norte",
 *   "fechaIngreso": "2024-01-15",
 *   "fechaSalida": "2024-03-20",
 *   "diasEnPotrero": 65,
 *   "movedBy": "550e8400-e29b-41d4-a716-446655440003",
 *   "createdAt": "2024-01-15T10:30:00"
 * }
 * </pre>
 * 
 * @param historyId Identificador único del registro de historial
 * @param animalId Identificador del animal
 * @param potreroId Identificador del potrero
 * @param potreroNombre Nombre del potrero
 * @param fechaIngreso Fecha de entrada al potrero
 * @param fechaSalida Fecha de salida del potrero (null si es ubicación actual)
 * @param diasEnPotrero Días de permanencia en el potrero
 * @param movedBy Usuario que registró el movimiento
 * @param createdAt Timestamp de creación del registro
 * 
 * @see PastureHistoryResult
 * @see mx.vacapp.cattle.internal.application.usecases.movement.GetMovementHistoryUseCase
 * @see mx.vacapp.cattle.internal.infrastructure.controllers.mobile.MovementRestController
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record PastureHistoryResponse(
    UUID historyId,
    UUID animalId,
    UUID potreroId,
    String potreroNombre,
    LocalDate fechaIngreso,
    LocalDate fechaSalida,
    Integer diasEnPotrero,
    UUID movedBy,
    LocalDateTime createdAt
) {
    
    /**
     * Factory method para crear un PastureHistoryResponse desde un PastureHistoryResult.
     * <p>
     * Convierte el resultado de la capa de aplicación en un DTO de infraestructura
     * para ser serializado y enviado a través de la API REST.
     * </p>
     * 
     * <h3>Conversiones de Tipos:</h3>
     * <ul>
     *   <li>Instant → LocalDate: Se convierte usando la zona horaria del sistema</li>
     *   <li>Instant → LocalDateTime: Se convierte usando la zona horaria del sistema</li>
     *   <li>diasPermanencia → diasEnPotrero: Renombrado para mayor claridad en API</li>
     *   <li>createdBy → movedBy: Renombrado para reflejar semántica de movimiento</li>
     * </ul>
     * 
     * <h3>Uso:</h3>
     * <pre>
     * List&lt;PastureHistoryResult&gt; results = getMovementHistoryUseCase.execute(animalId);
     * List&lt;PastureHistoryResponse&gt; responses = results.stream()
     *     .map(result -&gt; PastureHistoryResponse.fromResult(result, potreroNombre))
     *     .toList();
     * </pre>
     * 
     * @param result el resultado de la capa de aplicación
     * @param potreroNombre el nombre del potrero (obtenido de geographic-control module)
     * @return un nuevo PastureHistoryResponse con los datos convertidos
     * 
     * @see PastureHistoryResult
     */
    public static PastureHistoryResponse fromResult(PastureHistoryResult result, String potreroNombre) {
        return new PastureHistoryResponse(
            result.historyId(),
            result.animalId(),
            result.potreroId(),
            potreroNombre,
            convertToLocalDate(result.fechaEntrada()),
            convertToLocalDate(result.fechaSalida()),
            result.diasPermanencia(),
            result.createdBy(),
            convertToLocalDateTime(result.createdAt())
        );
    }
    
    /**
     * Convierte Instant a LocalDate usando la zona horaria del sistema.
     * 
     * @param instant el timestamp a convertir (puede ser null)
     * @return LocalDate correspondiente, o null si instant es null
     */
    private static LocalDate convertToLocalDate(java.time.Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }
    
    /**
     * Convierte Instant a LocalDateTime usando la zona horaria del sistema.
     * 
     * @param instant el timestamp a convertir (no puede ser null)
     * @return LocalDateTime correspondiente
     */
    private static LocalDateTime convertToLocalDateTime(java.time.Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * Verifica si este registro representa la ubicación actual del animal.
     * 
     * @return true si fechaSalida es null (animal aún está en este potrero)
     */
    public boolean isCurrentLocation() {
        return this.fechaSalida == null;
    }
}

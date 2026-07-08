package mx.vacapp.cattle.internal.application.usecases.commands;

import mx.vacapp.cattle.internal.domain.model.PastureHistory;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Resultado inmutable que representa los datos de un registro de historial de potrero.
 * <p>
 * Record utilizado para retornar información de historial de movimientos desde casos de uso hacia controladores.
 * Incluye todos los campos de la entidad de dominio PastureHistory más el campo calculado diasPermanencia.
 * </p>
 * 
 * <h2>Campos:</h2>
 * <ul>
 *   <li>{@code historyId} - Identificador único del registro de historial</li>
 *   <li>{@code animalId} - Identificador del animal</li>
 *   <li>{@code potreroId} - Identificador del potrero</li>
 *   <li>{@code fechaEntrada} - Timestamp de entrada al potrero</li>
 *   <li>{@code fechaSalida} - Timestamp de salida del potrero (null = ubicación actual)</li>
 *   <li>{@code diasPermanencia} - Días de permanencia en el potrero (calculado)</li>
 *   <li>{@code createdAt} - Timestamp de creación del registro</li>
 *   <li>{@code createdBy} - Usuario que creó el registro</li>
 * </ul>
 * 
 * <h2>Cálculo de Días de Permanencia:</h2>
 * <p>
 * El campo diasPermanencia se calcula de la siguiente manera:
 * <ul>
 *   <li>Si fechaSalida != null: DATEDIFF(fechaSalida, fechaEntrada)</li>
 *   <li>Si fechaSalida = null: DATEDIFF(NOW(), fechaEntrada) - ubicación actual</li>
 * </ul>
 * </p>
 * <p>
 * Un registro con fechaSalida = null representa la ubicación actual del animal.
 * </p>
 * 
 * @param historyId Identificador único del registro de historial
 * @param animalId Identificador del animal
 * @param potreroId Identificador del potrero
 * @param fechaEntrada Timestamp de entrada al potrero
 * @param fechaSalida Timestamp de salida del potrero (null = ubicación actual)
 * @param diasPermanencia Días de permanencia en el potrero
 * @param createdAt Timestamp de creación del registro
 * @param createdBy Usuario que creó el registro
 * 
 * @see PastureHistory
 * @see mx.vacapp.cattle.internal.application.usecases.movement.MoveAnimalUseCase
 * @see mx.vacapp.cattle.internal.application.usecases.movement.GetMovementHistoryUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record PastureHistoryResult(
    UUID historyId,
    UUID animalId,
    UUID potreroId,
    Instant fechaEntrada,
    Instant fechaSalida,
    Integer diasPermanencia,
    Instant createdAt,
    UUID createdBy
) {
    /**
     * Método factory para crear un PastureHistoryResult desde una entidad de dominio PastureHistory.
     * <p>
     * Convierte el objeto de dominio en un DTO inmutable para la capa de aplicación.
     * El campo diasPermanencia se calcula automáticamente:
     * <ul>
     *   <li>Si fechaSalida != null: días entre fechaEntrada y fechaSalida</li>
     *   <li>Si fechaSalida = null: días entre fechaEntrada y NOW() (ubicación actual)</li>
     * </ul>
     * </p>
     *
     * @param pastureHistory la entidad de dominio PastureHistory
     * @return un nuevo PastureHistoryResult con los datos del historial y diasPermanencia calculado
     */
    public static PastureHistoryResult fromDomain(PastureHistory pastureHistory) {
        Integer diasPermanencia = calculateDiasPermanencia(
            pastureHistory.getFechaEntrada(),
            pastureHistory.getFechaSalida()
        );
        
        return new PastureHistoryResult(
            pastureHistory.getHistoryId(),
            pastureHistory.getAnimalId(),
            pastureHistory.getPotreroId(),
            pastureHistory.getFechaEntrada(),
            pastureHistory.getFechaSalida(),
            diasPermanencia,
            pastureHistory.getCreatedAt(),
            pastureHistory.getCreatedBy()
        );
    }
    
    /**
     * Calcula los días de permanencia entre dos timestamps.
     * <p>
     * Si fechaSalida es null, calcula hasta el momento actual (ubicación actual del animal).
     * </p>
     *
     * @param fechaEntrada timestamp de entrada al potrero
     * @param fechaSalida timestamp de salida del potrero (null = ubicación actual)
     * @return días de permanencia calculados
     */
    private static Integer calculateDiasPermanencia(Instant fechaEntrada, Instant fechaSalida) {
        if (fechaEntrada == null) {
            return null;
        }
        
        Instant endDate = (fechaSalida != null) ? fechaSalida : Instant.now();
        long dias = Duration.between(fechaEntrada, endDate).toDays();
        
        return (int) dias;
    }
    
    /**
     * Verifica si este registro representa la ubicación actual del animal.
     * 
     * @return true si fechaSalida es null (animal aún está en este potrero)
     */
    public boolean isCurrent() {
        return this.fechaSalida == null;
    }
}

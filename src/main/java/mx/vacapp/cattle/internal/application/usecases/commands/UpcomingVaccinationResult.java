package mx.vacapp.cattle.internal.application.usecases.commands;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Result record para información de vacunaciones próximas de un animal.
 * 
 * <p>Este record representa una vacunación próxima calculada basándose en
 * el historial de vacunaciones del animal y las fechas programadas (proximaFecha)
 * registradas en eventos de salud de tipo VACUNACION.</p>
 * 
 * <h2>Campos:</h2>
 * <ul>
 *   <li><b>healthEventId:</b> UUID del evento de vacunación original</li>
 *   <li><b>animalId:</b> UUID del animal vacunado</li>
 *   <li><b>nombreVacuna:</b> Nombre de la vacuna (extraído del campo descripcion)</li>
 *   <li><b>ultimaFecha:</b> Fecha de la última aplicación de esta vacuna</li>
 *   <li><b>proximaFecha:</b> Fecha programada para la próxima aplicación</li>
 *   <li><b>diasRestantes:</b> Días que faltan hasta la próxima vacunación (puede ser negativo si está vencida)</li>
 * </ul>
 * 
 * <h2>Cálculo de diasRestantes:</h2>
 * <p>diasRestantes = DAYS_BETWEEN(HOY, proximaFecha)</p>
 * <ul>
 *   <li>Positivo: faltan X días para la vacunación</li>
 *   <li>Cero: vacunación programada para hoy</li>
 *   <li>Negativo: vacunación vencida hace X días</li>
 * </ul>
 * 
 * @param healthEventId UUID del evento de vacunación original
 * @param animalId UUID del animal
 * @param nombreVacuna nombre de la vacuna
 * @param ultimaFecha fecha de la última aplicación
 * @param proximaFecha fecha programada para la próxima aplicación
 * @param diasRestantes días hasta la próxima vacunación (negativo si está vencida)
 * 
 * @see mx.vacapp.cattle.internal.application.usecases.health.GetUpcomingVaccinationsUseCase
 * @see mx.vacapp.cattle.internal.domain.model.HealthEvent
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record UpcomingVaccinationResult(
    UUID healthEventId,
    UUID animalId,
    String nombreVacuna,
    LocalDate ultimaFecha,
    LocalDate proximaFecha,
    Integer diasRestantes
) {
    
    /**
     * Crea un UpcomingVaccinationResult calculando automáticamente diasRestantes.
     * 
     * @param healthEventId UUID del evento de vacunación
     * @param animalId UUID del animal
     * @param nombreVacuna nombre de la vacuna
     * @param ultimaFecha fecha de la última aplicación
     * @param proximaFecha fecha de la próxima aplicación
     * @return nuevo UpcomingVaccinationResult con diasRestantes calculados
     */
    public static UpcomingVaccinationResult of(
        UUID healthEventId,
        UUID animalId,
        String nombreVacuna,
        LocalDate ultimaFecha,
        LocalDate proximaFecha
    ) {
        LocalDate today = LocalDate.now();
        int diasRestantes = (int) ChronoUnit.DAYS.between(today, proximaFecha);
        
        return new UpcomingVaccinationResult(
            healthEventId,
            animalId,
            nombreVacuna,
            ultimaFecha,
            proximaFecha,
            diasRestantes
        );
    }
    
    /**
     * Verifica si esta vacunación está vencida (fecha pasó).
     * 
     * @return true si diasRestantes < 0 (vacunación vencida)
     */
    public boolean isVencida() {
        return diasRestantes < 0;
    }
    
    /**
     * Verifica si esta vacunación es para hoy.
     * 
     * @return true si diasRestantes == 0
     */
    public boolean isHoy() {
        return diasRestantes == 0;
    }
    
    /**
     * Verifica si esta vacunación es próxima (dentro de N días).
     * 
     * @param days cantidad de días
     * @return true si 0 <= diasRestantes <= days
     */
    public boolean isProximaEn(int days) {
        return diasRestantes >= 0 && diasRestantes <= days;
    }
}

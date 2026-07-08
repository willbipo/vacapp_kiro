package mx.vacapp.cattle.internal.domain.model;

import java.time.LocalDate;
import java.time.Period;

/**
 * Value Object para cálculos de edad del animal.
 * Clase estática que calcula la edad de un animal bovino en meses completos.
 */
public final class AgeCalculator {
    
    // Constructor privado para prevenir instanciación
    private AgeCalculator() {
        throw new UnsupportedOperationException("AgeCalculator es una clase de utilidad y no debe ser instanciada");
    }
    
    /**
     * Calcula edad en meses desde fecha de nacimiento hasta fecha actual.
     * Retorna la edad en meses completos (años * 12 + meses).
     * 
     * @param fechaNacimiento fecha de nacimiento del animal (no puede ser null)
     * @param fechaActual fecha actual de referencia (no puede ser null)
     * @return edad en meses completos
     * @throws IllegalArgumentException si fechaNacimiento es posterior a fechaActual
     * @throws IllegalArgumentException si algún parámetro es null
     */
    public static int calculateMonths(LocalDate fechaNacimiento, LocalDate fechaActual) {
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("Fecha de nacimiento no puede ser null");
        }
        if (fechaActual == null) {
            throw new IllegalArgumentException("Fecha actual no puede ser null");
        }
        if (fechaNacimiento.isAfter(fechaActual)) {
            throw new IllegalArgumentException("Fecha de nacimiento no puede ser futura");
        }
        
        Period period = Period.between(fechaNacimiento, fechaActual);
        return period.getYears() * 12 + period.getMonths();
    }
}

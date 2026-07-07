package mx.vacapp.geography.internal.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Value object que proporciona métodos estáticos para cálculos de superficie.
 * 
 * Esta clase encapsula la lógica de negocio para:
 * - Calcular superficie disponible
 * - Calcular porcentaje de uso
 * - Validar que suma de superficies no excede el total
 */
public final class SurfaceCalculator {
    
    // Constantes para cálculos
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final int SCALE_SUPERFICIE = 2;  // 2 decimales para superficies
    private static final int SCALE_PORCENTAJE = 2;  // 2 decimales para porcentajes
    
    // Constructor privado para prevenir instanciación
    private SurfaceCalculator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Calcula la superficie disponible restando la superficie usada del total.
     * 
     * @param superficieTotal superficie total del contenedor
     * @param superficieUsada superficie ya utilizada por hijos
     * @return superficie disponible (total - usada)
     */
    public static BigDecimal calculateAvailable(BigDecimal superficieTotal, BigDecimal superficieUsada) {
        if (superficieTotal == null || superficieUsada == null) {
            return BigDecimal.ZERO;
        }
        return superficieTotal.subtract(superficieUsada)
                             .setScale(SCALE_SUPERFICIE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calcula el porcentaje de uso de superficie.
     * Formula: (superficie_usada / superficie_total) * 100
     * 
     * @param superficieTotal superficie total del contenedor
     * @param superficieUsada superficie ya utilizada
     * @return porcentaje de uso con 2 decimales (0.00 - 100.00)
     */
    public static BigDecimal calculateUsagePercentage(BigDecimal superficieTotal, BigDecimal superficieUsada) {
        if (superficieTotal == null || superficieTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (superficieUsada == null) {
            return BigDecimal.ZERO;
        }
        
        return superficieUsada.divide(superficieTotal, SCALE_PORCENTAJE + 2, RoundingMode.HALF_UP)
                             .multiply(CIEN)
                             .setScale(SCALE_PORCENTAJE, RoundingMode.HALF_UP);
    }
    
    /**
     * Valida que la suma de superficies de una lista no exceda el total permitido.
     * 
     * @param superficieTotal superficie total del contenedor
     * @param superficies lista de superficies a sumar
     * @return true si la suma es <= total, false en caso contrario
     */
    public static boolean validateSurfaceSum(BigDecimal superficieTotal, List<BigDecimal> superficies) {
        if (superficieTotal == null || superficies == null || superficies.isEmpty()) {
            return true;
        }
        
        BigDecimal suma = superficies.stream()
                                    .filter(s -> s != null)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return suma.compareTo(superficieTotal) <= 0;
    }
    
    /**
     * Valida que agregar una nueva superficie no excederá el total.
     * 
     * @param superficieTotal superficie total del contenedor
     * @param superficieUsadaActual superficie ya utilizada
     * @param nuevaSuperficie superficie que se quiere agregar
     * @return true si (usada + nueva) <= total, false en caso contrario
     */
    public static boolean validateAddition(BigDecimal superficieTotal, 
                                          BigDecimal superficieUsadaActual, 
                                          BigDecimal nuevaSuperficie) {
        if (superficieTotal == null || nuevaSuperficie == null) {
            return false;
        }
        if (superficieUsadaActual == null) {
            superficieUsadaActual = BigDecimal.ZERO;
        }
        
        BigDecimal sumaTotal = superficieUsadaActual.add(nuevaSuperficie);
        return sumaTotal.compareTo(superficieTotal) <= 0;
    }
    
    /**
     * Calcula cuánta superficie queda disponible después de sumar una lista de superficies.
     * 
     * @param superficieTotal superficie total del contenedor
     * @param superficies lista de superficies ya utilizadas
     * @return superficie disponible, o 0 si el resultado es negativo
     */
    public static BigDecimal calculateRemainingAfterSum(BigDecimal superficieTotal, List<BigDecimal> superficies) {
        if (superficieTotal == null) {
            return BigDecimal.ZERO;
        }
        if (superficies == null || superficies.isEmpty()) {
            return superficieTotal;
        }
        
        BigDecimal suma = superficies.stream()
                                    .filter(s -> s != null)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal disponible = superficieTotal.subtract(suma);
        
        // Si el resultado es negativo, retornar 0
        return disponible.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : disponible;
    }
    
    /**
     * Valida que una superficie sea válida (mayor que 0).
     * 
     * @param superficie superficie a validar
     * @return true si es válida (> 0)
     */
    public static boolean isValidSurface(BigDecimal superficie) {
        return superficie != null && superficie.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Redondea una superficie a 2 decimales con redondeo CEILING (hacia arriba).
     * Útil para cálculos de porcentajes donde queremos ser conservadores.
     * 
     * @param superficie superficie a redondear
     * @return superficie redondeada
     */
    public static BigDecimal roundUpSurface(BigDecimal superficie) {
        if (superficie == null) {
            return BigDecimal.ZERO;
        }
        return superficie.setScale(SCALE_SUPERFICIE, RoundingMode.CEILING);
    }
}

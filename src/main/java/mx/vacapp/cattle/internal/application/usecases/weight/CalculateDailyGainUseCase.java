package mx.vacapp.cattle.internal.application.usecases.weight;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Caso de uso utilitario para calcular la ganancia diaria de peso entre dos pesajes consecutivos.
 * 
 * <p>Este caso de uso implementa el cálculo de ganancia diaria (kg/día) utilizando la fórmula:</p>
 * <pre>
 * ganancia_diaria = (pesoActual - pesoAnterior) / diasEntrePesajes
 * </pre>
 * 
 * <p>El resultado se redondea a 2 decimales usando HALF_UP para precisión en reportes.</p>
 * 
 * <h2>Casos de Uso:</h2>
 * <ul>
 *   <li>Calcular ganancia diaria al registrar un nuevo peso (RecordWeightUseCase)</li>
 *   <li>Obtener historial de pesos con ganancia diaria calculada (GetWeightHistoryUseCase)</li>
 *   <li>Generar reportes de crecimiento y engorda de animales</li>
 * </ul>
 * 
 * <h2>Validaciones:</h2>
 * <ul>
 *   <li><b>diasEntrePesajes = 0:</b> Lanza IllegalArgumentException (no se puede dividir entre 0)</li>
 *   <li><b>pesoAnterior es null:</b> Retorna null (indica que es el primer peso del animal)</li>
 *   <li><b>Pesos o días negativos:</b> Permite valores negativos para representar pérdida de peso,
 *       pero los casos de uso deben validar que los pesos sean positivos antes de llamar este método</li>
 * </ul>
 * 
 * <h2>Ejemplo de Uso:</h2>
 * <pre>
 * // Animal pesó 250 kg hace 30 días, ahora pesa 265 kg
 * BigDecimal ganancia = calculateDailyGain(
 *     new BigDecimal("265.00"),  // pesoActual
 *     new BigDecimal("250.00"),  // pesoAnterior
 *     30                          // diasEntrePesajes
 * );
 * // ganancia = 0.50 kg/día
 * </pre>
 * 
 * @see mx.vacapp.cattle.internal.application.usecases.weight.RecordWeightUseCase
 * @see mx.vacapp.cattle.internal.application.usecases.weight.GetWeightHistoryUseCase
 * @see mx.vacapp.cattle.internal.domain.model.WeightRecord
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@Slf4j
public class CalculateDailyGainUseCase {
    
    /**
     * Calcula la ganancia diaria de peso entre dos pesajes consecutivos.
     * 
     * <p>Fórmula: ganancia_diaria = (pesoActual - pesoAnterior) / diasEntrePesajes</p>
     * <p>El resultado se redondea a 2 decimales usando HALF_UP.</p>
     * 
     * @param pesoActual Peso actual del animal en kilogramos (no debe ser null)
     * @param pesoAnterior Peso anterior del animal en kilogramos (puede ser null si es el primer peso)
     * @param diasEntrePesajes Número de días transcurridos entre los dos pesajes (debe ser > 0)
     * @return Ganancia diaria en kg/día redondeada a 2 decimales, o null si pesoAnterior es null
     * @throws IllegalArgumentException si diasEntrePesajes es 0 (no se puede dividir entre 0)
     * @throws NullPointerException si pesoActual es null
     */
    public BigDecimal calculateDailyGain(BigDecimal pesoActual, BigDecimal pesoAnterior, Integer diasEntrePesajes) {
        log.debug("Calculando ganancia diaria: pesoActual={}, pesoAnterior={}, dias={}", 
                  pesoActual, pesoAnterior, diasEntrePesajes);
        
        // Validar parámetros requeridos
        if (pesoActual == null) {
            log.error("pesoActual es null");
            throw new NullPointerException("El peso actual no puede ser null");
        }
        
        // Si no hay peso anterior, es el primer peso del animal (Requirement 7.5)
        if (pesoAnterior == null) {
            log.debug("pesoAnterior es null, retornando null (primer peso del animal)");
            return null;
        }
        
        // Si diasEntrePesajes es 0, no se puede calcular ganancia diaria
        if (diasEntrePesajes == null || diasEntrePesajes == 0) {
            log.error("diasEntrePesajes es 0, no se puede dividir entre 0");
            throw new IllegalArgumentException("Los días entre pesajes no pueden ser 0");
        }
        
        // Calcular diferencia de peso
        BigDecimal diferenciaPeso = pesoActual.subtract(pesoAnterior);
        
        // Calcular ganancia diaria: (pesoActual - pesoAnterior) / diasEntrePesajes
        BigDecimal ganancia = diferenciaPeso.divide(
            new BigDecimal(diasEntrePesajes),
            2,  // 2 decimales
            RoundingMode.HALF_UP  // Redondeo estándar
        );
        
        log.debug("Ganancia diaria calculada: {} kg/día", ganancia);
        
        return ganancia;
    }
}

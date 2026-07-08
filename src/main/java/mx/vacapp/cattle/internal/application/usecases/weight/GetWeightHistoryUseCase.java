package mx.vacapp.cattle.internal.application.usecases.weight;

import lombok.RequiredArgsConstructor;
import mx.vacapp.cattle.internal.application.usecases.commands.WeightResult;
import mx.vacapp.cattle.internal.domain.model.WeightRecord;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.WeightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para obtener el historial completo de pesos de un animal.
 * <p>
 * Este caso de uso recupera todos los registros de peso de un animal específico,
 * ordenados por fecha de pesaje de más reciente a más antiguo (DESC).
 * Para cada par de pesos consecutivos, calcula la ganancia diaria basada en la diferencia
 * de peso y los días transcurridos entre pesajes.
 * </p>
 * 
 * <h2>Entrada:</h2>
 * <ul>
 *   <li>{@code animalId} - UUID del animal</li>
 * </ul>
 * 
 * <h2>Flujo:</h2>
 * <ol>
 *   <li>Validar que el animal existe en el repositorio</li>
 *   <li>Obtener todos los registros de peso del animal desde WeightRepository</li>
 *   <li>Para cada par de pesos consecutivos, calcular ganancia_diaria:
 *       <pre>ganancia_diaria = (peso_actual - peso_anterior) / dias_entre_pesajes</pre>
 *   </li>
 *   <li>Ordenar los resultados por fechaPesaje DESC (más reciente primero)</li>
 * </ol>
 * 
 * <h2>Salida:</h2>
 * <p>
 * Lista de {@link WeightResult} con ganancia_diaria calculada para cada registro.
 * El registro más reciente tendrá ganancia_diaria = null ya que no hay peso posterior.
 * </p>
 * 
 * <h2>Validaciones:</h2>
 * <ul>
 *   <li>El animal debe existir en el sistema</li>
 *   <li>El animal debe pertenecer al tenant del contexto (validado por AnimalRepository)</li>
 * </ul>
 * 
 * <h2>Cálculo de Ganancia Diaria:</h2>
 * <p>
 * La ganancia diaria se calcula comparando cada peso con el peso anterior (cronológicamente).
 * Por ejemplo, si los pesos están ordenados DESC:
 * </p>
 * <pre>
 * Peso 3 (más reciente): 450 kg - ganancia_diaria = null (no hay peso posterior)
 * Peso 2: 430 kg - ganancia_diaria = (450 - 430) / días_entre_peso2_y_peso3
 * Peso 1 (más antiguo): 400 kg - ganancia_diaria = (430 - 400) / días_entre_peso1_y_peso2
 * </pre>
 * 
 * <h2>Excepción:</h2>
 * <ul>
 *   <li>{@link AnimalNotFoundException} - Si el animal no existe o no pertenece al tenant</li>
 * </ul>
 * 
 * @see WeightResult
 * @see WeightRecord
 * @see WeightRepository
 * @see AnimalRepository
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
public class GetWeightHistoryUseCase {
    
    private final AnimalRepository animalRepository;
    private final WeightRepository weightRepository;
    
    /**
     * Ejecuta el caso de uso para obtener el historial de pesos de un animal.
     * 
     * @param animalId UUID del animal cuyo historial de pesos se desea obtener
     * @return lista de WeightResult ordenada por fechaPesaje DESC con ganancia_diaria calculada
     * @throws AnimalNotFoundException si el animal no existe o no pertenece al tenant
     */
    @Transactional(readOnly = true)
    public List<WeightResult> execute(UUID animalId) {
        // 1. Validar que el animal existe
        animalRepository.findById(animalId)
            .orElseThrow(() -> new AnimalNotFoundException("Animal no encontrado"));
        
        // 2. Obtener todos los registros de peso del animal ordenados por fecha DESC
        List<WeightRecord> weightRecords = weightRepository.findByAnimalId(animalId);
        
        // Si no hay pesos registrados, retornar lista vacía
        if (weightRecords.isEmpty()) {
            return List.of();
        }
        
        // 3. Calcular ganancia_diaria para cada par de pesos consecutivos
        List<WeightResult> results = new ArrayList<>();
        
        for (int i = 0; i < weightRecords.size(); i++) {
            WeightRecord currentWeight = weightRecords.get(i);
            
            // El peso más reciente (índice 0) no tiene ganancia diaria porque no hay peso posterior
            if (i == 0) {
                results.add(WeightResult.fromDomain(currentWeight, null, null));
            } else {
                // Comparar con el peso anterior (cronológicamente)
                WeightRecord previousWeight = weightRecords.get(i - 1);
                
                // Calcular ganancia diaria
                BigDecimal ganancia = calculateDailyGain(previousWeight, currentWeight);
                int dias = calculateDaysBetween(currentWeight, previousWeight);
                
                results.add(WeightResult.fromDomain(currentWeight, ganancia, dias));
            }
        }
        
        // 4. Los resultados ya están ordenados por fechaPesaje DESC
        return results;
    }
    
    /**
     * Calcula la ganancia diaria entre dos pesos consecutivos.
     * <p>
     * Fórmula: ganancia_diaria = (peso_posterior - peso_anterior) / días_entre_pesajes
     * </p>
     * <p>
     * El resultado se redondea a 2 decimales usando HALF_UP (redondeo comercial).
     * </p>
     * 
     * @param anteriorWeight peso anterior (cronológicamente, fecha más antigua)
     * @param posteriorWeight peso posterior (cronológicamente, fecha más reciente)
     * @return ganancia diaria en kg/día, redondeada a 2 decimales
     */
    private BigDecimal calculateDailyGain(WeightRecord anteriorWeight, WeightRecord posteriorWeight) {
        BigDecimal diferenciaPeso = posteriorWeight.getPesoKg().subtract(anteriorWeight.getPesoKg());
        long dias = ChronoUnit.DAYS.between(anteriorWeight.getFechaPesaje(), posteriorWeight.getFechaPesaje());
        
        // Evitar división por cero (en caso de que se registren dos pesos en el mismo día)
        if (dias == 0) {
            return BigDecimal.ZERO;
        }
        
        // ganancia_diaria = diferencia_peso / días
        return diferenciaPeso.divide(BigDecimal.valueOf(dias), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calcula los días transcurridos entre dos fechas de pesaje.
     * 
     * @param anteriorWeight peso anterior (cronológicamente, fecha más antigua)
     * @param posteriorWeight peso posterior (cronológicamente, fecha más reciente)
     * @return número de días entre los dos pesajes
     */
    private int calculateDaysBetween(WeightRecord anteriorWeight, WeightRecord posteriorWeight) {
        return (int) ChronoUnit.DAYS.between(anteriorWeight.getFechaPesaje(), posteriorWeight.getFechaPesaje());
    }
}

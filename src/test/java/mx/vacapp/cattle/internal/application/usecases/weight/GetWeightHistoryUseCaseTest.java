package mx.vacapp.cattle.internal.application.usecases.weight;

import mx.vacapp.cattle.internal.application.usecases.commands.WeightResult;
import mx.vacapp.cattle.internal.domain.model.*;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.WeightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para GetWeightHistoryUseCase.
 * <p>
 * Valida la recuperación del historial de pesos de un animal con cálculo
 * de ganancia diaria entre pesos consecutivos.
 * </p>
 * 
 * <h2>Casos de Prueba:</h2>
 * <ul>
 *   <li>Recuperación exitosa de historial de pesos con ganancia diaria calculada</li>
 *   <li>Historial con un solo peso (sin ganancia diaria)</li>
 *   <li>Historial con múltiples pesos ordenados correctamente</li>
 *   <li>Animal sin historial de pesos (lista vacía)</li>
 *   <li>Animal no encontrado (AnimalNotFoundException)</li>
 *   <li>Validación de orden cronológico DESC</li>
 *   <li>Cálculo correcto de ganancia diaria positiva y negativa</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class GetWeightHistoryUseCaseTest {
    
    @Mock
    private AnimalRepository animalRepository;
    
    @Mock
    private WeightRepository weightRepository;
    
    @InjectMocks
    private GetWeightHistoryUseCase getWeightHistoryUseCase;
    
    private UUID animalId;
    private UUID tenantId;
    private UUID ranchoId;
    private UUID createdBy;
    private Animal testAnimal;
    
    @BeforeEach
    void setUp() {
        animalId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        ranchoId = UUID.randomUUID();
        createdBy = UUID.randomUUID();
        
        // Crear animal de prueba
        LocalDate fechaNacimiento = LocalDate.now().minusMonths(12);
        testAnimal = Animal.create(
            "TEST001",
            Sex.MACHO,
            Breed.ANGUS,
            fechaNacimiento,
            CattleType.ENGORDA,
            ranchoId,
            tenantId,
            createdBy
        );
        
        testAnimal = new Animal.Builder()
            .from(testAnimal)
            .animalId(animalId)
            .build();
    }
    
    @Test
    void execute_shouldReturnEmptyList_whenAnimalHasNoWeightRecords() {
        // Given
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(weightRepository.findByAnimalId(animalId)).thenReturn(List.of());
        
        // When
        List<WeightResult> results = getWeightHistoryUseCase.execute(animalId);
        
        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        verify(animalRepository).findById(animalId);
        verify(weightRepository).findByAnimalId(animalId);
    }
    
    @Test
    void execute_shouldReturnSingleWeight_withoutGanancia_whenOnlyOneWeightExists() {
        // Given
        BigDecimal peso = new BigDecimal("350.00");
        LocalDate fechaPesaje = LocalDate.now().minusDays(10);
        
        WeightRecord weight = WeightRecord.create(
            animalId,
            peso,
            fechaPesaje,
            "Primer pesaje",
            createdBy
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(weightRepository.findByAnimalId(animalId)).thenReturn(List.of(weight));
        
        // When
        List<WeightResult> results = getWeightHistoryUseCase.execute(animalId);
        
        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        
        WeightResult result = results.get(0);
        assertEquals(peso, result.pesoKg());
        assertEquals(fechaPesaje, result.fechaPesaje());
        assertNull(result.ganancia_diaria(), "Primer peso no debe tener ganancia diaria");
        assertNull(result.diasDesdeUltimoPesaje(), "Primer peso no debe tener días calculados");
        
        verify(animalRepository).findById(animalId);
        verify(weightRepository).findByAnimalId(animalId);
    }
    
    @Test
    void execute_shouldCalculateGananciaDiaria_whenMultipleWeightsExist() {
        // Given: 3 pesos en orden DESC (más reciente primero)
        LocalDate fecha1 = LocalDate.now().minusDays(60);  // Más antiguo
        LocalDate fecha2 = LocalDate.now().minusDays(30);  // Medio
        LocalDate fecha3 = LocalDate.now();                // Más reciente
        
        BigDecimal peso1 = new BigDecimal("300.00");  // Peso inicial
        BigDecimal peso2 = new BigDecimal("330.00");  // +30 kg en 30 días = 1.00 kg/día
        BigDecimal peso3 = new BigDecimal("360.00");  // +30 kg en 30 días = 1.00 kg/día
        
        WeightRecord weight1 = WeightRecord.create(animalId, peso1, fecha1, "Peso inicial", createdBy);
        WeightRecord weight2 = WeightRecord.create(animalId, peso2, fecha2, "Peso intermedio", createdBy);
        WeightRecord weight3 = WeightRecord.create(animalId, peso3, fecha3, "Peso actual", createdBy);
        
        // Repository retorna en orden DESC (más reciente primero)
        List<WeightRecord> weights = List.of(weight3, weight2, weight1);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(weightRepository.findByAnimalId(animalId)).thenReturn(weights);
        
        // When
        List<WeightResult> results = getWeightHistoryUseCase.execute(animalId);
        
        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        
        // Peso más reciente (índice 0): sin ganancia diaria
        WeightResult result0 = results.get(0);
        assertEquals(peso3, result0.pesoKg());
        assertEquals(fecha3, result0.fechaPesaje());
        assertNull(result0.ganancia_diaria());
        assertNull(result0.diasDesdeUltimoPesaje());
        
        // Peso intermedio (índice 1): ganancia = (360 - 330) / 30 = 1.00 kg/día
        WeightResult result1 = results.get(1);
        assertEquals(peso2, result1.pesoKg());
        assertEquals(fecha2, result1.fechaPesaje());
        assertNotNull(result1.ganancia_diaria());
        assertEquals(new BigDecimal("1.00"), result1.ganancia_diaria());
        assertEquals(30, result1.diasDesdeUltimoPesaje());
        
        // Peso más antiguo (índice 2): ganancia = (330 - 300) / 30 = 1.00 kg/día
        WeightResult result2 = results.get(2);
        assertEquals(peso1, result2.pesoKg());
        assertEquals(fecha1, result2.fechaPesaje());
        assertNotNull(result2.ganancia_diaria());
        assertEquals(new BigDecimal("1.00"), result2.ganancia_diaria());
        assertEquals(30, result2.diasDesdeUltimoPesaje());
        
        verify(animalRepository).findById(animalId);
        verify(weightRepository).findByAnimalId(animalId);
    }
    
    @Test
    void execute_shouldCalculateNegativeGanancia_whenAnimalLosesWeight() {
        // Given: Animal que pierde peso
        LocalDate fecha1 = LocalDate.now().minusDays(20);
        LocalDate fecha2 = LocalDate.now();
        
        BigDecimal peso1 = new BigDecimal("400.00");
        BigDecimal peso2 = new BigDecimal("380.00");  // Perdió 20 kg en 20 días = -1.00 kg/día
        
        WeightRecord weight1 = WeightRecord.create(animalId, peso1, fecha1, "Peso anterior", createdBy);
        WeightRecord weight2 = WeightRecord.create(animalId, peso2, fecha2, "Peso actual", createdBy);
        
        List<WeightRecord> weights = List.of(weight2, weight1);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(weightRepository.findByAnimalId(animalId)).thenReturn(weights);
        
        // When
        List<WeightResult> results = getWeightHistoryUseCase.execute(animalId);
        
        // Then
        assertEquals(2, results.size());
        
        // Peso más reciente
        WeightResult result0 = results.get(0);
        assertEquals(peso2, result0.pesoKg());
        assertNull(result0.ganancia_diaria());
        
        // Peso anterior: ganancia negativa = (380 - 400) / 20 = -1.00 kg/día
        WeightResult result1 = results.get(1);
        assertEquals(peso1, result1.pesoKg());
        assertNotNull(result1.ganancia_diaria());
        assertEquals(new BigDecimal("-1.00"), result1.ganancia_diaria());
        assertEquals(20, result1.diasDesdeUltimoPesaje());
    }
    
    @Test
    void execute_shouldHandleZeroDaysDifference_whenTwoWeightsOnSameDay() {
        // Given: Dos pesos registrados el mismo día
        LocalDate sameFecha = LocalDate.now();
        
        BigDecimal peso1 = new BigDecimal("350.00");
        BigDecimal peso2 = new BigDecimal("350.50");
        
        WeightRecord weight1 = WeightRecord.create(animalId, peso1, sameFecha, "Pesaje mañana", createdBy);
        WeightRecord weight2 = WeightRecord.create(animalId, peso2, sameFecha, "Pesaje tarde", createdBy);
        
        List<WeightRecord> weights = List.of(weight2, weight1);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(weightRepository.findByAnimalId(animalId)).thenReturn(weights);
        
        // When
        List<WeightResult> results = getWeightHistoryUseCase.execute(animalId);
        
        // Then
        assertEquals(2, results.size());
        
        // Segundo peso: ganancia = 0 (división por cero manejada)
        WeightResult result1 = results.get(1);
        assertEquals(peso1, result1.pesoKg());
        assertNotNull(result1.ganancia_diaria());
        assertEquals(BigDecimal.ZERO, result1.ganancia_diaria());
        assertEquals(0, result1.diasDesdeUltimoPesaje());
    }
    
    @Test
    void execute_shouldRoundGananciaDiariaToTwoDecimals() {
        // Given: Ganancia que requiere redondeo
        LocalDate fecha1 = LocalDate.now().minusDays(7);
        LocalDate fecha2 = LocalDate.now();
        
        BigDecimal peso1 = new BigDecimal("300.00");
        BigDecimal peso2 = new BigDecimal("305.00");  // +5 kg en 7 días = 0.714285... kg/día
        
        WeightRecord weight1 = WeightRecord.create(animalId, peso1, fecha1, null, createdBy);
        WeightRecord weight2 = WeightRecord.create(animalId, peso2, fecha2, null, createdBy);
        
        List<WeightRecord> weights = List.of(weight2, weight1);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(weightRepository.findByAnimalId(animalId)).thenReturn(weights);
        
        // When
        List<WeightResult> results = getWeightHistoryUseCase.execute(animalId);
        
        // Then
        WeightResult result1 = results.get(1);
        assertNotNull(result1.ganancia_diaria());
        // Debe redondear a 2 decimales: 5 / 7 = 0.71 kg/día
        assertEquals(new BigDecimal("0.71"), result1.ganancia_diaria());
    }
    
    @Test
    void execute_shouldThrowAnimalNotFoundException_whenAnimalDoesNotExist() {
        // Given
        when(animalRepository.findById(animalId)).thenReturn(Optional.empty());
        
        // When & Then
        AnimalNotFoundException exception = assertThrows(
            AnimalNotFoundException.class,
            () -> getWeightHistoryUseCase.execute(animalId)
        );
        
        assertEquals("Animal no encontrado", exception.getMessage());
        verify(animalRepository).findById(animalId);
        verifyNoInteractions(weightRepository);
    }
    
    @Test
    void execute_shouldReturnWeightsInCorrectOrder_DESC() {
        // Given: 5 pesos en diferentes fechas
        List<WeightRecord> weights = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            LocalDate fecha = LocalDate.now().minusDays(40 - (i * 10));
            BigDecimal peso = new BigDecimal(300 + (i * 10));
            WeightRecord weight = WeightRecord.create(
                animalId,
                peso,
                fecha,
                "Pesaje " + (i + 1),
                createdBy
            );
            weights.add(weight);
        }
        
        // weights ya está en orden DESC (más reciente primero)
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(weightRepository.findByAnimalId(animalId)).thenReturn(weights);
        
        // When
        List<WeightResult> results = getWeightHistoryUseCase.execute(animalId);
        
        // Then
        assertEquals(5, results.size());
        
        // Verificar que están ordenados por fecha DESC
        for (int i = 0; i < results.size() - 1; i++) {
            LocalDate fechaActual = results.get(i).fechaPesaje();
            LocalDate fechaSiguiente = results.get(i + 1).fechaPesaje();
            assertTrue(
                fechaActual.isAfter(fechaSiguiente) || fechaActual.isEqual(fechaSiguiente),
                "Los pesos deben estar ordenados por fecha DESC"
            );
        }
    }
    
    @Test
    void execute_shouldIncludeAllWeightRecordFields() {
        // Given
        LocalDate fecha = LocalDate.now().minusDays(15);
        BigDecimal peso = new BigDecimal("375.50");
        String notas = "Pesaje con observaciones importantes";
        
        WeightRecord weight = WeightRecord.create(
            animalId,
            peso,
            fecha,
            notas,
            createdBy
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(weightRepository.findByAnimalId(animalId)).thenReturn(List.of(weight));
        
        // When
        List<WeightResult> results = getWeightHistoryUseCase.execute(animalId);
        
        // Then
        assertEquals(1, results.size());
        WeightResult result = results.get(0);
        
        assertNotNull(result.weightId());
        assertEquals(animalId, result.animalId());
        assertEquals(peso, result.pesoKg());
        assertEquals(fecha, result.fechaPesaje());
        assertEquals(notas, result.notas());
        assertNotNull(result.recordedAt());
        assertEquals(createdBy, result.recordedBy());
    }
}

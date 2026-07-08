package mx.vacapp.cattle.internal.application.usecases.weight;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para CalculateDailyGainUseCase.
 * Valida el cálculo de ganancia diaria de peso entre dos pesajes consecutivos.
 */
class CalculateDailyGainUseCaseTest {
    
    private CalculateDailyGainUseCase calculateDailyGainUseCase;
    
    @BeforeEach
    void setUp() {
        calculateDailyGainUseCase = new CalculateDailyGainUseCase();
    }
    
    @Test
    void calculateDailyGain_shouldReturnCorrectValue_whenWeightIncreased() {
        // Given: Animal ganó 15 kg en 30 días
        BigDecimal pesoActual = new BigDecimal("265.00");
        BigDecimal pesoAnterior = new BigDecimal("250.00");
        Integer diasEntrePesajes = 30;
        
        // When
        BigDecimal ganancia = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual, pesoAnterior, diasEntrePesajes
        );
        
        // Then: ganancia = (265 - 250) / 30 = 15 / 30 = 0.50 kg/día
        assertNotNull(ganancia);
        assertEquals(new BigDecimal("0.50"), ganancia);
    }
    
    @Test
    void calculateDailyGain_shouldReturnNegativeValue_whenWeightDecreased() {
        // Given: Animal perdió 10 kg en 20 días
        BigDecimal pesoActual = new BigDecimal("240.00");
        BigDecimal pesoAnterior = new BigDecimal("250.00");
        Integer diasEntrePesajes = 20;
        
        // When
        BigDecimal ganancia = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual, pesoAnterior, diasEntrePesajes
        );
        
        // Then: ganancia = (240 - 250) / 20 = -10 / 20 = -0.50 kg/día
        assertNotNull(ganancia);
        assertEquals(new BigDecimal("-0.50"), ganancia);
    }
    
    @Test
    void calculateDailyGain_shouldRoundToTwoDecimals_whenResultHasMoreDecimals() {
        // Given: Resultado con muchos decimales
        BigDecimal pesoActual = new BigDecimal("260.00");
        BigDecimal pesoAnterior = new BigDecimal("250.00");
        Integer diasEntrePesajes = 7;
        
        // When
        BigDecimal ganancia = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual, pesoAnterior, diasEntrePesajes
        );
        
        // Then: ganancia = (260 - 250) / 7 = 10 / 7 = 1.428571... redondeado a 1.43
        assertNotNull(ganancia);
        assertEquals(new BigDecimal("1.43"), ganancia);
        assertEquals(2, ganancia.scale(), "Debe tener exactamente 2 decimales");
    }
    
    @Test
    void calculateDailyGain_shouldReturnNull_whenPesoAnteriorIsNull() {
        // Given: Primer peso del animal (no hay peso anterior)
        BigDecimal pesoActual = new BigDecimal("250.00");
        BigDecimal pesoAnterior = null;
        Integer diasEntrePesajes = 30;
        
        // When
        BigDecimal ganancia = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual, pesoAnterior, diasEntrePesajes
        );
        
        // Then: Debe retornar null porque es el primer peso
        assertNull(ganancia, "Debe retornar null cuando no hay peso anterior");
    }
    
    @Test
    void calculateDailyGain_shouldThrowException_whenDiasEntrePesajesIsZero() {
        // Given: Dos pesajes el mismo día (0 días de diferencia)
        BigDecimal pesoActual = new BigDecimal("250.00");
        BigDecimal pesoAnterior = new BigDecimal("249.00");
        Integer diasEntrePesajes = 0;
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> calculateDailyGainUseCase.calculateDailyGain(
                pesoActual, pesoAnterior, diasEntrePesajes
            )
        );
        
        assertEquals("Los días entre pesajes no pueden ser 0", exception.getMessage());
    }
    
    @Test
    void calculateDailyGain_shouldThrowException_whenDiasEntrePesajesIsNull() {
        // Given: Días null
        BigDecimal pesoActual = new BigDecimal("250.00");
        BigDecimal pesoAnterior = new BigDecimal("249.00");
        Integer diasEntrePesajes = null;
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> calculateDailyGainUseCase.calculateDailyGain(
                pesoActual, pesoAnterior, diasEntrePesajes
            )
        );
        
        assertEquals("Los días entre pesajes no pueden ser 0", exception.getMessage());
    }
    
    @Test
    void calculateDailyGain_shouldThrowException_whenPesoActualIsNull() {
        // Given: Peso actual null
        BigDecimal pesoActual = null;
        BigDecimal pesoAnterior = new BigDecimal("250.00");
        Integer diasEntrePesajes = 30;
        
        // When & Then
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> calculateDailyGainUseCase.calculateDailyGain(
                pesoActual, pesoAnterior, diasEntrePesajes
            )
        );
        
        assertEquals("El peso actual no puede ser null", exception.getMessage());
    }
    
    @Test
    void calculateDailyGain_shouldHandleZeroGain_whenWeightIsUnchanged() {
        // Given: Peso no cambió
        BigDecimal pesoActual = new BigDecimal("250.00");
        BigDecimal pesoAnterior = new BigDecimal("250.00");
        Integer diasEntrePesajes = 30;
        
        // When
        BigDecimal ganancia = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual, pesoAnterior, diasEntrePesajes
        );
        
        // Then: ganancia = (250 - 250) / 30 = 0 / 30 = 0.00
        assertNotNull(ganancia);
        assertEquals(new BigDecimal("0.00"), ganancia);
    }
    
    @Test
    void calculateDailyGain_shouldHandleLargeWeightGain() {
        // Given: Animal ganó mucho peso (50 kg en 60 días)
        BigDecimal pesoActual = new BigDecimal("350.00");
        BigDecimal pesoAnterior = new BigDecimal("300.00");
        Integer diasEntrePesajes = 60;
        
        // When
        BigDecimal ganancia = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual, pesoAnterior, diasEntrePesajes
        );
        
        // Then: ganancia = (350 - 300) / 60 = 50 / 60 = 0.83 kg/día
        assertNotNull(ganancia);
        assertEquals(new BigDecimal("0.83"), ganancia);
    }
    
    @Test
    void calculateDailyGain_shouldHandleLargeWeightLoss() {
        // Given: Animal perdió mucho peso (enfermedad)
        BigDecimal pesoActual = new BigDecimal("200.00");
        BigDecimal pesoAnterior = new BigDecimal("250.00");
        Integer diasEntrePesajes = 30;
        
        // When
        BigDecimal ganancia = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual, pesoAnterior, diasEntrePesajes
        );
        
        // Then: ganancia = (200 - 250) / 30 = -50 / 30 = -1.67 kg/día
        assertNotNull(ganancia);
        assertEquals(new BigDecimal("-1.67"), ganancia);
    }
    
    @Test
    void calculateDailyGain_shouldHandleSmallWeightChange() {
        // Given: Cambio muy pequeño de peso
        BigDecimal pesoActual = new BigDecimal("250.50");
        BigDecimal pesoAnterior = new BigDecimal("250.00");
        Integer diasEntrePesajes = 7;
        
        // When
        BigDecimal ganancia = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual, pesoAnterior, diasEntrePesajes
        );
        
        // Then: ganancia = (250.5 - 250) / 7 = 0.5 / 7 = 0.07 kg/día
        assertNotNull(ganancia);
        assertEquals(new BigDecimal("0.07"), ganancia);
    }
    
    @Test
    void calculateDailyGain_shouldHandleOneDayInterval() {
        // Given: Pesajes en días consecutivos
        BigDecimal pesoActual = new BigDecimal("251.00");
        BigDecimal pesoAnterior = new BigDecimal("250.00");
        Integer diasEntrePesajes = 1;
        
        // When
        BigDecimal ganancia = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual, pesoAnterior, diasEntrePesajes
        );
        
        // Then: ganancia = (251 - 250) / 1 = 1.00 kg/día
        assertNotNull(ganancia);
        assertEquals(new BigDecimal("1.00"), ganancia);
    }
    
    @Test
    void calculateDailyGain_shouldHandleLongInterval() {
        // Given: Pesajes separados por varios meses (180 días)
        BigDecimal pesoActual = new BigDecimal("400.00");
        BigDecimal pesoAnterior = new BigDecimal("250.00");
        Integer diasEntrePesajes = 180;
        
        // When
        BigDecimal ganancia = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual, pesoAnterior, diasEntrePesajes
        );
        
        // Then: ganancia = (400 - 250) / 180 = 150 / 180 = 0.83 kg/día
        assertNotNull(ganancia);
        assertEquals(new BigDecimal("0.83"), ganancia);
    }
    
    @Test
    void calculateDailyGain_shouldUseHalfUpRounding() {
        // Given: Caso que prueba redondeo HALF_UP
        // 10 / 3 = 3.333... debe redondear a 3.33 (redondea hacia abajo)
        // 14 / 3 = 4.666... debe redondear a 4.67 (redondea hacia arriba)
        BigDecimal pesoActual1 = new BigDecimal("260.00");
        BigDecimal pesoAnterior1 = new BigDecimal("250.00");
        
        BigDecimal pesoActual2 = new BigDecimal("264.00");
        BigDecimal pesoAnterior2 = new BigDecimal("250.00");
        
        Integer dias = 3;
        
        // When
        BigDecimal ganancia1 = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual1, pesoAnterior1, dias
        );
        BigDecimal ganancia2 = calculateDailyGainUseCase.calculateDailyGain(
            pesoActual2, pesoAnterior2, dias
        );
        
        // Then
        assertEquals(new BigDecimal("3.33"), ganancia1, "10/3 = 3.333... debe redondear a 3.33");
        assertEquals(new BigDecimal("4.67"), ganancia2, "14/3 = 4.666... debe redondear a 4.67");
    }
}

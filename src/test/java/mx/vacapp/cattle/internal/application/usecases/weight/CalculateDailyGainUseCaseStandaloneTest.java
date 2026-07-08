package mx.vacapp.cattle.internal.application.usecases.weight;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba standalone simple para CalculateDailyGainUseCase
 * Este test no tiene dependencias externas y se puede ejecutar de forma independiente
 */
class CalculateDailyGainUseCaseStandaloneTest {
    
    @Test
    void testBasicCalculation() {
        CalculateDailyGainUseCase useCase = new CalculateDailyGainUseCase();
        
        // Animal ganó 15 kg en 30 días = 0.50 kg/día
        BigDecimal result = useCase.calculateDailyGain(
            new BigDecimal("265.00"),
            new BigDecimal("250.00"),
            30
        );
        
        assertEquals(new BigDecimal("0.50"), result);
    }
    
    @Test
    void testNullPesoAnterior() {
        CalculateDailyGainUseCase useCase = new CalculateDailyGainUseCase();
        
        // Primer peso del animal
        BigDecimal result = useCase.calculateDailyGain(
            new BigDecimal("250.00"),
            null,
            30
        );
        
        assertNull(result);
    }
    
    @Test
    void testZeroDays() {
        CalculateDailyGainUseCase useCase = new CalculateDailyGainUseCase();
        
        assertThrows(IllegalArgumentException.class, () -> {
            useCase.calculateDailyGain(
                new BigDecimal("250.00"),
                new BigDecimal("249.00"),
                0
            );
        });
    }
}

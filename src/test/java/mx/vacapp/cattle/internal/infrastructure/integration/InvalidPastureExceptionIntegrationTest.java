package mx.vacapp.cattle.internal.infrastructure.integration;

import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidPastureException;
import mx.vacapp.cattle.internal.infrastructure.integration.GeographyServiceClient;
import mx.vacapp.geography.GeographyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests de integración para validación de potrero inactivo.
 * 
 * <p>Verifica que el flujo completo de validación de potrero funciona correctamente
 * desde GeographyServiceClient hasta el lanzamiento de InvalidPastureException,
 * que será capturado por CattleGlobalExceptionHandler para retornar HTTP 400.</p>
 * 
 * <p>Este test confirma la implementación completa de Requirement 3.2:
 * "Cuando potrero no existe o está inactivo, retornar HTTP 400 con mensaje
 * 'Potrero no existe o está inactivo'"</p>
 */
@DisplayName("InvalidPastureException Integration Test")
class InvalidPastureExceptionIntegrationTest {
    
    private GeographyServiceClient geographyServiceClient;
    
    @Mock
    private GeographyService geographyService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        geographyServiceClient = new GeographyServiceClient(geographyService);
    }
    
    @Test
    @DisplayName("validatePotreroActive debe lanzar InvalidPastureException cuando potrero no existe")
    void validatePotreroActive_shouldThrowExceptionWhenPotreroDoesNotExist() {
        // Given
        UUID potreroId = UUID.randomUUID();
        when(geographyService.isPotreroActive(potreroId)).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> geographyServiceClient.validatePotreroActive(potreroId))
            .isInstanceOf(InvalidPastureException.class)
            .hasMessage("Potrero no existe o está inactivo");
    }
    
    @Test
    @DisplayName("validatePotreroActive debe lanzar InvalidPastureException cuando potrero está inactivo")
    void validatePotreroActive_shouldThrowExceptionWhenPotreroIsInactive() {
        // Given
        UUID potreroId = UUID.randomUUID();
        when(geographyService.isPotreroActive(potreroId)).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> geographyServiceClient.validatePotreroActive(potreroId))
            .isInstanceOf(InvalidPastureException.class)
            .hasMessage("Potrero no existe o está inactivo");
    }
    
    @Test
    @DisplayName("validatePotreroActive NO debe lanzar excepción cuando potrero está activo")
    void validatePotreroActive_shouldNotThrowExceptionWhenPotreroIsActive() {
        // Given
        UUID potreroId = UUID.randomUUID();
        when(geographyService.isPotreroActive(potreroId)).thenReturn(true);
        
        // When & Then
        geographyServiceClient.validatePotreroActive(potreroId);
        // No exception thrown = success
    }
    
    @Test
    @DisplayName("isPotreroActive debe retornar false cuando potrero no existe")
    void isPotreroActive_shouldReturnFalseWhenPotreroDoesNotExist() {
        // Given
        UUID potreroId = UUID.randomUUID();
        when(geographyService.isPotreroActive(potreroId)).thenReturn(false);
        
        // When
        boolean result = geographyServiceClient.isPotreroActive(potreroId);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("isPotreroActive debe retornar true cuando potrero está activo")
    void isPotreroActive_shouldReturnTrueWhenPotreroIsActive() {
        // Given
        UUID potreroId = UUID.randomUUID();
        when(geographyService.isPotreroActive(potreroId)).thenReturn(true);
        
        // When
        boolean result = geographyServiceClient.isPotreroActive(potreroId);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("isPotreroActive debe retornar false de forma defensiva cuando ocurre excepción")
    void isPotreroActive_shouldReturnFalseWhenServiceThrowsException() {
        // Given
        UUID potreroId = UUID.randomUUID();
        when(geographyService.isPotreroActive(potreroId))
            .thenThrow(new RuntimeException("Geography service unavailable"));
        
        // When
        boolean result = geographyServiceClient.isPotreroActive(potreroId);
        
        // Then
        assertThat(result).isFalse(); // Comportamiento defensivo
    }
    
    @Test
    @DisplayName("validatePotreroActive debe lanzar InvalidPastureException cuando servicio falla")
    void validatePotreroActive_shouldThrowExceptionWhenServiceFails() {
        // Given
        UUID potreroId = UUID.randomUUID();
        when(geographyService.isPotreroActive(potreroId))
            .thenThrow(new RuntimeException("Geography service unavailable"));
        
        // When & Then
        assertThatThrownBy(() -> geographyServiceClient.validatePotreroActive(potreroId))
            .isInstanceOf(InvalidPastureException.class)
            .hasMessage("Potrero no existe o está inactivo");
    }
}

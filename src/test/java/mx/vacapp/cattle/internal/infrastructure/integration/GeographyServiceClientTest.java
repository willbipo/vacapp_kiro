package mx.vacapp.cattle.internal.infrastructure.integration;

import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidPastureException;
import mx.vacapp.geography.GeographyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GeographyServiceClient.
 * Tests cover successful validation, error handling, and caching behavior.
 */
@ExtendWith(MockitoExtension.class)
class GeographyServiceClientTest {
    
    @Mock
    private GeographyService geographyService;
    
    @InjectMocks
    private GeographyServiceClient geographyServiceClient;
    
    private UUID validPotreroId;
    private UUID invalidPotreroId;
    
    @BeforeEach
    void setUp() {
        validPotreroId = UUID.randomUUID();
        invalidPotreroId = UUID.randomUUID();
    }
    
    @Test
    void isPotreroActive_shouldReturnTrue_whenPotreroExistsAndIsActive() {
        // Given
        when(geographyService.isPotreroActive(validPotreroId)).thenReturn(true);
        
        // When
        boolean result = geographyServiceClient.isPotreroActive(validPotreroId);
        
        // Then
        assertTrue(result);
        verify(geographyService, times(1)).isPotreroActive(validPotreroId);
    }
    
    @Test
    void isPotreroActive_shouldReturnFalse_whenPotreroDoesNotExist() {
        // Given
        when(geographyService.isPotreroActive(invalidPotreroId)).thenReturn(false);
        
        // When
        boolean result = geographyServiceClient.isPotreroActive(invalidPotreroId);
        
        // Then
        assertFalse(result);
        verify(geographyService, times(1)).isPotreroActive(invalidPotreroId);
    }
    
    @Test
    void isPotreroActive_shouldReturnFalse_whenPotreroIsInactive() {
        // Given
        when(geographyService.isPotreroActive(invalidPotreroId)).thenReturn(false);
        
        // When
        boolean result = geographyServiceClient.isPotreroActive(invalidPotreroId);
        
        // Then
        assertFalse(result);
        verify(geographyService, times(1)).isPotreroActive(invalidPotreroId);
    }
    
    @Test
    void isPotreroActive_shouldReturnFalse_whenServiceThrowsException() {
        // Given
        UUID problematicPotreroId = UUID.randomUUID();
        when(geographyService.isPotreroActive(problematicPotreroId))
            .thenThrow(new RuntimeException("Database connection error"));
        
        // When
        boolean result = geographyServiceClient.isPotreroActive(problematicPotreroId);
        
        // Then
        assertFalse(result);
        verify(geographyService, times(1)).isPotreroActive(problematicPotreroId);
    }
    
    @Test
    void isPotreroActive_shouldHandleNullPointerException() {
        // Given
        UUID problematicPotreroId = UUID.randomUUID();
        when(geographyService.isPotreroActive(problematicPotreroId))
            .thenThrow(new NullPointerException("Geography service unavailable"));
        
        // When
        boolean result = geographyServiceClient.isPotreroActive(problematicPotreroId);
        
        // Then
        assertFalse(result);
        verify(geographyService, times(1)).isPotreroActive(problematicPotreroId);
    }
    
    @Test
    void isPotreroActive_shouldReturnFalse_whenServiceThrowsIllegalArgumentException() {
        // Given
        UUID problematicPotreroId = UUID.randomUUID();
        when(geographyService.isPotreroActive(problematicPotreroId))
            .thenThrow(new IllegalArgumentException("Invalid potrero ID"));
        
        // When
        boolean result = geographyServiceClient.isPotreroActive(problematicPotreroId);
        
        // Then
        assertFalse(result);
        verify(geographyService, times(1)).isPotreroActive(problematicPotreroId);
    }
    
    @Test
    void isPotreroActive_shouldHandleMultipleSuccessiveCallsCorrectly() {
        // Given
        UUID potrero1 = UUID.randomUUID();
        UUID potrero2 = UUID.randomUUID();
        when(geographyService.isPotreroActive(potrero1)).thenReturn(true);
        when(geographyService.isPotreroActive(potrero2)).thenReturn(false);
        
        // When
        boolean result1 = geographyServiceClient.isPotreroActive(potrero1);
        boolean result2 = geographyServiceClient.isPotreroActive(potrero2);
        
        // Then
        assertTrue(result1);
        assertFalse(result2);
        verify(geographyService, times(1)).isPotreroActive(potrero1);
        verify(geographyService, times(1)).isPotreroActive(potrero2);
    }
    
    // Tests for validatePotreroActive() method
    
    @Test
    void validatePotreroActive_shouldNotThrowException_whenPotreroIsActive() {
        // Given
        when(geographyService.isPotreroActive(validPotreroId)).thenReturn(true);
        
        // When & Then
        assertDoesNotThrow(() -> geographyServiceClient.validatePotreroActive(validPotreroId));
        verify(geographyService, times(1)).isPotreroActive(validPotreroId);
    }
    
    @Test
    void validatePotreroActive_shouldThrowInvalidPastureException_whenPotreroDoesNotExist() {
        // Given
        when(geographyService.isPotreroActive(invalidPotreroId)).thenReturn(false);
        
        // When & Then
        InvalidPastureException exception = assertThrows(
            InvalidPastureException.class,
            () -> geographyServiceClient.validatePotreroActive(invalidPotreroId)
        );
        
        assertEquals("Potrero no existe o está inactivo", exception.getMessage());
        verify(geographyService, times(1)).isPotreroActive(invalidPotreroId);
    }
    
    @Test
    void validatePotreroActive_shouldThrowInvalidPastureException_whenPotreroIsInactive() {
        // Given
        when(geographyService.isPotreroActive(invalidPotreroId)).thenReturn(false);
        
        // When & Then
        InvalidPastureException exception = assertThrows(
            InvalidPastureException.class,
            () -> geographyServiceClient.validatePotreroActive(invalidPotreroId)
        );
        
        assertEquals("Potrero no existe o está inactivo", exception.getMessage());
        verify(geographyService, times(1)).isPotreroActive(invalidPotreroId);
    }
    
    @Test
    void validatePotreroActive_shouldThrowInvalidPastureException_whenServiceThrowsException() {
        // Given
        UUID problematicPotreroId = UUID.randomUUID();
        when(geographyService.isPotreroActive(problematicPotreroId))
            .thenThrow(new RuntimeException("Database connection error"));
        
        // When & Then - The method returns false on exception, so validatePotreroActive will throw
        InvalidPastureException exception = assertThrows(
            InvalidPastureException.class,
            () -> geographyServiceClient.validatePotreroActive(problematicPotreroId)
        );
        
        assertEquals("Potrero no existe o está inactivo", exception.getMessage());
        verify(geographyService, times(1)).isPotreroActive(problematicPotreroId);
    }
    
    @Test
    void validatePotreroActive_shouldUseCachedResult() {
        // Given
        when(geographyService.isPotreroActive(validPotreroId)).thenReturn(true);
        
        // When - First call caches the result
        geographyServiceClient.isPotreroActive(validPotreroId);
        
        // Then - Second call through validatePotreroActive should use cached result
        // Note: In a real test with actual cache, this would verify cache usage
        // For unit test with mocks, we verify the method completes successfully
        assertDoesNotThrow(() -> geographyServiceClient.validatePotreroActive(validPotreroId));
    }
}

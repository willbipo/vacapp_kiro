package mx.vacapp.cattle.internal.infrastructure.controllers.mobile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.model.exceptions.DuplicateAreteException;
import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidGenealogyException;
import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidPastureException;
import mx.vacapp.cattle.internal.domain.model.exceptions.SoldOrDeadAnimalException;
import mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para CattleGlobalExceptionHandler.
 * 
 * Verifica que todas las excepciones de dominio del módulo cattle-inventory
 * se mapean correctamente a respuestas HTTP con códigos y mensajes apropiados.
 */
@DisplayName("CattleGlobalExceptionHandler - Manejo de Excepciones")
class CattleGlobalExceptionHandlerTest {
    
    private CattleGlobalExceptionHandler handler;
    
    @Mock
    private HttpServletRequest request;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new CattleGlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/cattle");
    }
    
    @Test
    @DisplayName("InvalidPastureException debe retornar 400 Bad Request")
    void handleInvalidPasture_shouldReturn400() {
        // Given
        String expectedMessage = "Potrero no existe o está inactivo";
        InvalidPastureException exception = new InvalidPastureException(expectedMessage);
        
        // When
        ResponseEntity<ErrorResponse> response = handler.handleInvalidPasture(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo(expectedMessage);
        assertThat(response.getBody().path()).isEqualTo("/api/v1/cattle");
        assertThat(response.getBody().errors()).isNull();
    }
    
    @Test
    @DisplayName("InvalidGenealogyException debe retornar 400 Bad Request")
    void handleInvalidGenealogy_shouldReturn400() {
        // Given
        String expectedMessage = "Madre inválida";
        InvalidGenealogyException exception = new InvalidGenealogyException(expectedMessage);
        
        // When
        ResponseEntity<ErrorResponse> response = handler.handleInvalidGenealogy(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo(expectedMessage);
    }
    
    @Test
    @DisplayName("SoldOrDeadAnimalException debe retornar 400 Bad Request")
    void handleSoldOrDeadAnimal_shouldReturn400() {
        // Given
        String expectedMessage = "No se puede modificar un animal vendido o muerto";
        SoldOrDeadAnimalException exception = new SoldOrDeadAnimalException(expectedMessage);
        
        // When
        ResponseEntity<ErrorResponse> response = handler.handleSoldOrDeadAnimal(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo(expectedMessage);
    }
    
    @Test
    @DisplayName("AnimalNotFoundException debe retornar 404 Not Found")
    void handleAnimalNotFound_shouldReturn404() {
        // Given
        String expectedMessage = "Animal no encontrado";
        AnimalNotFoundException exception = new AnimalNotFoundException(expectedMessage);
        
        // When
        ResponseEntity<ErrorResponse> response = handler.handleAnimalNotFound(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo(expectedMessage);
    }
    
    @Test
    @DisplayName("DuplicateAreteException debe retornar 409 Conflict")
    void handleDuplicateArete_shouldReturn409() {
        // Given
        String expectedMessage = "Arete ya registrado en el sistema";
        DuplicateAreteException exception = new DuplicateAreteException(expectedMessage);
        
        // When
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateArete(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().message()).isEqualTo(expectedMessage);
    }
    
    @Test
    @DisplayName("MethodArgumentNotValidException debe retornar 400 con errores de campo")
    void handleValidationErrors_shouldReturn400WithFieldErrors() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("animalRequest", "arete", "no puede estar vacío");
        FieldError fieldError2 = new FieldError("animalRequest", "sexo", "debe ser Macho o Hembra");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        when(bindingResult.getErrorCount()).thenReturn(2);
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
            null, bindingResult);
        
        // When
        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Error de validación en los datos de entrada");
        assertThat(response.getBody().errors()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(2);
        assertThat(response.getBody().errors().get(0).field()).isEqualTo("arete");
        assertThat(response.getBody().errors().get(0).message()).isEqualTo("no puede estar vacío");
    }
    
    @Test
    @DisplayName("ConstraintViolationException debe retornar 400 con detalles de violación")
    void handleConstraintViolation_shouldReturn400WithViolationDetails() {
        // Given
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("arete");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("debe tener entre 4 y 20 caracteres");
        
        ConstraintViolationException exception = new ConstraintViolationException(
            "Validation failed", Set.of(violation));
        
        // When
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Error de validación en restricciones");
        assertThat(response.getBody().errors()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(1);
    }
    
    @Test
    @DisplayName("IllegalArgumentException debe retornar 400")
    void handleIllegalArgument_shouldReturn400() {
        // Given
        String expectedMessage = "Argumento inválido";
        IllegalArgumentException exception = new IllegalArgumentException(expectedMessage);
        
        // When
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo(expectedMessage);
    }
    
    @Test
    @DisplayName("Excepciones genéricas deben retornar 500 Internal Server Error")
    void handleGenericException_shouldReturn500() {
        // Given
        Exception exception = new RuntimeException("Error inesperado");
        
        // When
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message())
            .isEqualTo("Ha ocurrido un error inesperado. Por favor contacte al administrador.");
        assertThat(response.getBody().errors()).isNull();
    }
    
    @Test
    @DisplayName("ErrorResponse debe incluir timestamp en formato ISO 8601")
    void errorResponse_shouldIncludeIso8601Timestamp() {
        // Given
        InvalidPastureException exception = new InvalidPastureException("Test message");
        
        // When
        ResponseEntity<ErrorResponse> response = handler.handleInvalidPasture(exception, request);
        
        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().timestamp()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z");
    }
}

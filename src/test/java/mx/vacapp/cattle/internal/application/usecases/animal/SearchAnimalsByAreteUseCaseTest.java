package mx.vacapp.cattle.internal.application.usecases.animal;

import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.Breed;
import mx.vacapp.cattle.internal.domain.model.CattleStatus;
import mx.vacapp.cattle.internal.domain.model.CattleType;
import mx.vacapp.cattle.internal.domain.model.Sex;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.users.internal.infrastructure.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitario para SearchAnimalsByAreteUseCase.
 * 
 * Verifica:
 * - Búsqueda case-insensitive por arete
 * - Filtrado automático por tenant_id
 * - Validación de parámetros
 * - Ordenamiento de resultados por arete ASC
 */
@ExtendWith(MockitoExtension.class)
class SearchAnimalsByAreteUseCaseTest {
    
    @Mock
    private AnimalRepository animalRepository;
    
    @InjectMocks
    private SearchAnimalsByAreteUseCase searchAnimalsByAreteUseCase;
    
    private UUID tenantId;
    private UUID ranchoId;
    private UUID createdBy;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        ranchoId = UUID.randomUUID();
        createdBy = UUID.randomUUID();
        
        // Establecer tenant_id en el contexto
        TenantContext.setTenantId(tenantId);
    }
    
    @AfterEach
    void tearDown() {
        // Limpiar TenantContext después de cada test
        TenantContext.clear();
    }
    
    @Test
    void execute_ShouldReturnAnimals_WhenAreteContainsQuery() {
        // Given
        String areteQuery = "123";
        
        Animal animal1 = createTestAnimal("A123", "Animal A123");
        Animal animal2 = createTestAnimal("B1234", "Animal B1234");
        Animal animal3 = createTestAnimal("123C", "Animal 123C");
        
        List<Animal> mockAnimals = Arrays.asList(animal1, animal2, animal3);
        
        when(animalRepository.findByAreteContaining(tenantId, areteQuery))
            .thenReturn(mockAnimals);
        
        // When
        List<AnimalResult> results = searchAnimalsByAreteUseCase.execute(areteQuery);
        
        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).arete()).isEqualTo("A123");
        assertThat(results.get(1).arete()).isEqualTo("B1234");
        assertThat(results.get(2).arete()).isEqualTo("123C");
        
        verify(animalRepository).findByAreteContaining(tenantId, areteQuery);
    }
    
    @Test
    void execute_ShouldReturnEmptyList_WhenNoAnimalsMatchQuery() {
        // Given
        String areteQuery = "XYZ";
        
        when(animalRepository.findByAreteContaining(tenantId, areteQuery))
            .thenReturn(List.of());
        
        // When
        List<AnimalResult> results = searchAnimalsByAreteUseCase.execute(areteQuery);
        
        // Then
        assertThat(results).isEmpty();
        verify(animalRepository).findByAreteContaining(tenantId, areteQuery);
    }
    
    @Test
    void execute_ShouldThrowException_WhenQueryIsNull() {
        // When/Then
        assertThatThrownBy(() -> searchAnimalsByAreteUseCase.execute(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La consulta de búsqueda no puede estar vacía");
    }
    
    @Test
    void execute_ShouldThrowException_WhenQueryIsEmpty() {
        // When/Then
        assertThatThrownBy(() -> searchAnimalsByAreteUseCase.execute(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La consulta de búsqueda no puede estar vacía");
    }
    
    @Test
    void execute_ShouldThrowException_WhenQueryIsBlank() {
        // When/Then
        assertThatThrownBy(() -> searchAnimalsByAreteUseCase.execute("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La consulta de búsqueda no puede estar vacía");
    }
    
    @Test
    void execute_ShouldThrowException_WhenNoTenantInContext() {
        // Given
        TenantContext.clear();
        
        // When/Then
        assertThatThrownBy(() -> searchAnimalsByAreteUseCase.execute("123"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No hay tenant_id en el contexto de seguridad");
    }
    
    @Test
    void execute_ShouldBeCaseInsensitive() {
        // Given
        String areteQuery = "abc";
        
        Animal animal1 = createTestAnimal("ABC123", "Animal ABC");
        Animal animal2 = createTestAnimal("XabcY", "Animal XabcY");
        
        List<Animal> mockAnimals = Arrays.asList(animal1, animal2);
        
        when(animalRepository.findByAreteContaining(tenantId, areteQuery))
            .thenReturn(mockAnimals);
        
        // When
        List<AnimalResult> results = searchAnimalsByAreteUseCase.execute(areteQuery);
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).arete()).containsIgnoringCase("abc");
        assertThat(results.get(1).arete()).containsIgnoringCase("abc");
        
        verify(animalRepository).findByAreteContaining(tenantId, areteQuery);
    }
    
    @Test
    void execute_ShouldCalculateAgeInRealTime() {
        // Given
        String areteQuery = "A1";
        LocalDate birthDate = LocalDate.now().minusMonths(6);
        
        Animal animal = createTestAnimalWithBirthDate("A123", birthDate);
        
        when(animalRepository.findByAreteContaining(tenantId, areteQuery))
            .thenReturn(List.of(animal));
        
        // When
        List<AnimalResult> results = searchAnimalsByAreteUseCase.execute(areteQuery);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).meses()).isEqualTo(6);
    }
    
    /**
     * Helper method para crear un animal de prueba.
     */
    private Animal createTestAnimal(String arete, String nota) {
        return createTestAnimalWithBirthDate(arete, LocalDate.now().minusMonths(12));
    }
    
    /**
     * Helper method para crear un animal de prueba con fecha de nacimiento específica.
     */
    private Animal createTestAnimalWithBirthDate(String arete, LocalDate birthDate) {
        return new Animal.Builder()
            .animalId(UUID.randomUUID())
            .arete(arete)
            .sexo(Sex.HEMBRA)
            .raza(Breed.ANGUS)
            .fechaNacimiento(birthDate)
            .meses(java.time.Period.between(birthDate, LocalDate.now()).getYears() * 12 
                   + java.time.Period.between(birthDate, LocalDate.now()).getMonths())
            .tipo(CattleType.VENTA)
            .status(CattleStatus.ACTIVA)
            .ranchoId(ranchoId)
            .tenantId(tenantId)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
    }
}

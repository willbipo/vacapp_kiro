package mx.vacapp.cattle.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgeCalculator class.
 * Tests cover core functionality, edge cases, and validation rules.
 */
class AgeCalculatorTest {
    
    @Test
    void calculateMonths_ShouldReturnZero_WhenBothDatesAreTheSame() {
        // Given
        LocalDate birthDate = LocalDate.of(2024, 1, 15);
        LocalDate currentDate = LocalDate.of(2024, 1, 15);
        
        // When
        int months = AgeCalculator.calculateMonths(birthDate, currentDate);
        
        // Then
        assertEquals(0, months);
    }
    
    @Test
    void calculateMonths_ShouldCalculateCorrectMonths_WhenAgeIsLessThanOneYear() {
        // Given
        LocalDate birthDate = LocalDate.of(2024, 1, 15);
        LocalDate currentDate = LocalDate.of(2024, 7, 15);
        
        // When
        int months = AgeCalculator.calculateMonths(birthDate, currentDate);
        
        // Then
        assertEquals(6, months);
    }
    
    @Test
    void calculateMonths_ShouldCalculateCorrectMonths_WhenAgeIsExactlyOneYear() {
        // Given
        LocalDate birthDate = LocalDate.of(2023, 1, 15);
        LocalDate currentDate = LocalDate.of(2024, 1, 15);
        
        // When
        int months = AgeCalculator.calculateMonths(birthDate, currentDate);
        
        // Then
        assertEquals(12, months);
    }
    
    @Test
    void calculateMonths_ShouldCalculateCorrectMonths_WhenAgeIsMultipleYears() {
        // Given
        LocalDate birthDate = LocalDate.of(2020, 3, 10);
        LocalDate currentDate = LocalDate.of(2024, 9, 10);
        
        // When
        int months = AgeCalculator.calculateMonths(birthDate, currentDate);
        
        // Then
        assertEquals(54, months); // 4 years * 12 + 6 months
    }
    
    @Test
    void calculateMonths_ShouldCalculateCorrectMonths_WhenYearsAndMonthsAreMixed() {
        // Given
        LocalDate birthDate = LocalDate.of(2022, 5, 20);
        LocalDate currentDate = LocalDate.of(2024, 8, 20);
        
        // When
        int months = AgeCalculator.calculateMonths(birthDate, currentDate);
        
        // Then
        assertEquals(27, months); // 2 years * 12 + 3 months
    }
    
    @Test
    void calculateMonths_ShouldNotCountIncompleteMonths() {
        // Given
        LocalDate birthDate = LocalDate.of(2024, 1, 31);
        LocalDate currentDate = LocalDate.of(2024, 2, 28);
        
        // When
        int months = AgeCalculator.calculateMonths(birthDate, currentDate);
        
        // Then
        assertEquals(0, months); // Incomplete month
    }
    
    @Test
    void calculateMonths_ShouldHandleLeapYearCorrectly() {
        // Given
        LocalDate birthDate = LocalDate.of(2020, 2, 29); // Leap year
        LocalDate currentDate = LocalDate.of(2024, 2, 29); // Another leap year
        
        // When
        int months = AgeCalculator.calculateMonths(birthDate, currentDate);
        
        // Then
        assertEquals(48, months); // Exactly 4 years
    }
    
    @Test
    void calculateMonths_ShouldHandleDifferentDaysOfMonth() {
        // Given
        LocalDate birthDate = LocalDate.of(2023, 1, 31);
        LocalDate currentDate = LocalDate.of(2024, 3, 15);
        
        // When
        int months = AgeCalculator.calculateMonths(birthDate, currentDate);
        
        // Then
        assertEquals(13, months); // 1 year + 1 month (Jan 31 to March 15)
    }
    
    @Test
    void calculateMonths_ShouldThrowException_WhenBirthDateIsAfterCurrentDate() {
        // Given
        LocalDate birthDate = LocalDate.of(2025, 1, 1);
        LocalDate currentDate = LocalDate.of(2024, 1, 1);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AgeCalculator.calculateMonths(birthDate, currentDate)
        );
        
        assertEquals("Fecha de nacimiento no puede ser futura", exception.getMessage());
    }
    
    @Test
    void calculateMonths_ShouldThrowException_WhenBirthDateIsNull() {
        // Given
        LocalDate currentDate = LocalDate.of(2024, 1, 1);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AgeCalculator.calculateMonths(null, currentDate)
        );
        
        assertEquals("Fecha de nacimiento no puede ser null", exception.getMessage());
    }
    
    @Test
    void calculateMonths_ShouldThrowException_WhenCurrentDateIsNull() {
        // Given
        LocalDate birthDate = LocalDate.of(2024, 1, 1);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AgeCalculator.calculateMonths(birthDate, null)
        );
        
        assertEquals("Fecha actual no puede ser null", exception.getMessage());
    }
    
    @Test
    void calculateMonths_ShouldHandleVeryOldAnimals() {
        // Given
        LocalDate birthDate = LocalDate.of(2000, 1, 1);
        LocalDate currentDate = LocalDate.of(2024, 1, 1);
        
        // When
        int months = AgeCalculator.calculateMonths(birthDate, currentDate);
        
        // Then
        assertEquals(288, months); // 24 years * 12 months
    }
    
    @Test
    void constructor_ShouldThrowException_WhenAttemptingToInstantiate() {
        // When & Then
        assertThrows(
            Exception.class,
            () -> {
                // Use reflection to attempt instantiation
                var constructor = AgeCalculator.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            }
        );
    }
}

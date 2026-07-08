package mx.vacapp.cattle.internal.infrastructure.persistence.mappers;

import mx.vacapp.cattle.internal.domain.model.*;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.AnimalEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para AnimalMapper.
 * Valida la transformación bidireccional entre Animal (dominio) y AnimalEntity (JPA).
 */
class AnimalMapperTest {
    
    @Test
    void toEntity_withValidAnimal_shouldMapAllFields() {
        // Given: un animal de dominio completo
        UUID animalId = UUID.randomUUID();
        UUID ranchoId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID madreId = UUID.randomUUID();
        UUID padreId = UUID.randomUUID();
        Instant now = Instant.now();
        LocalDate birthDate = LocalDate.of(2020, 5, 15);
        
        Animal domain = new Animal.Builder()
            .animalId(animalId)
            .arete("MX12345")
            .areteAnterior("MX99999")
            .sexo(Sex.HEMBRA)
            .raza(Breed.CHAROLAIS)
            .fechaNacimiento(birthDate)
            .meses(AgeCalculator.calculateMonths(birthDate, LocalDate.now()))
            .fechaAretado(birthDate.plusDays(5))
            .tipo(CattleType.VIENTRE)
            .status(CattleStatus.PRENADA)
            .folioReemo("REEMO-2024-001")
            .nota("Animal de excelente genética")
            .madreId(madreId)
            .padreId(padreId)
            .ranchoId(ranchoId)
            .tenantId(tenantId)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .fechaVenta(null)
            .precioVenta(null)
            .fechaMuerte(null)
            .motivoMuerte(null)
            .build();
        
        // When: se convierte a entidad JPA
        AnimalEntity entity = AnimalMapper.toEntity(domain);
        
        // Then: todos los campos deben estar mapeados correctamente
        assertNotNull(entity);
        assertEquals(animalId, entity.getAnimalId());
        assertEquals("MX12345", entity.getArete());
        assertEquals("MX99999", entity.getAreteAnterior());
        assertEquals(AnimalEntity.SexEnum.HEMBRA, entity.getSexo());
        assertEquals(AnimalEntity.BreedEnum.CHAROLAIS, entity.getRaza());
        assertEquals(birthDate, entity.getFechaNacimiento());
        assertEquals(AnimalEntity.CattleTypeEnum.VIENTRE, entity.getTipo());
        assertEquals(AnimalEntity.CattleStatusEnum.PRENADA, entity.getStatus());
        assertEquals("REEMO-2024-001", entity.getFolioReemo());
        assertEquals("Animal de excelente genética", entity.getObservaciones());
        assertEquals(madreId, entity.getMadreId());
        assertEquals(padreId, entity.getPadreId());
        assertEquals(ranchoId, entity.getRanchoId());
        assertEquals(tenantId, entity.getTenantId());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
        assertEquals(createdBy, entity.getCreatedBy());
        assertEquals(createdBy, entity.getUpdatedBy());
        assertNull(entity.getFechaVenta());
        assertNull(entity.getPrecioVenta());
        assertNull(entity.getFechaMuerte());
        assertNull(entity.getMotivoMuerte());
    }
    
    @Test
    void toEntity_withSoldAnimal_shouldMapSaleFields() {
        // Given: un animal vendido
        LocalDate saleDate = LocalDate.of(2024, 11, 20);
        BigDecimal salePrice = new BigDecimal("25000.00");
        
        Animal domain = new Animal.Builder()
            .animalId(UUID.randomUUID())
            .arete("MX67890")
            .sexo(Sex.MACHO)
            .raza(Breed.ANGUS)
            .fechaNacimiento(LocalDate.of(2022, 3, 10))
            .meses(30)
            .tipo(CattleType.ENGORDA)
            .status(CattleStatus.VENDIDA)
            .fechaVenta(saleDate)
            .precioVenta(salePrice)
            .ranchoId(UUID.randomUUID())
            .tenantId(UUID.randomUUID())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
        
        // When: se convierte a entidad JPA
        AnimalEntity entity = AnimalMapper.toEntity(domain);
        
        // Then: los campos de venta deben estar mapeados
        assertNotNull(entity);
        assertEquals(AnimalEntity.CattleStatusEnum.VENDIDA, entity.getStatus());
        assertEquals(saleDate, entity.getFechaVenta());
        assertEquals(salePrice, entity.getPrecioVenta());
        assertNull(entity.getFechaMuerte());
        assertNull(entity.getMotivoMuerte());
    }
    
    @Test
    void toEntity_withDeadAnimal_shouldMapDeathFields() {
        // Given: un animal muerto
        LocalDate deathDate = LocalDate.of(2024, 10, 5);
        String deathReason = "Enfermedad respiratoria";
        
        Animal domain = new Animal.Builder()
            .animalId(UUID.randomUUID())
            .arete("MX11111")
            .sexo(Sex.HEMBRA)
            .raza(Breed.BRAHMAN)
            .fechaNacimiento(LocalDate.of(2018, 1, 1))
            .meses(80)
            .tipo(CattleType.CRIA)
            .status(CattleStatus.MUERTA)
            .fechaMuerte(deathDate)
            .motivoMuerte(deathReason)
            .ranchoId(UUID.randomUUID())
            .tenantId(UUID.randomUUID())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
        
        // When: se convierte a entidad JPA
        AnimalEntity entity = AnimalMapper.toEntity(domain);
        
        // Then: los campos de muerte deben estar mapeados
        assertNotNull(entity);
        assertEquals(AnimalEntity.CattleStatusEnum.MUERTA, entity.getStatus());
        assertEquals(deathDate, entity.getFechaMuerte());
        assertEquals(deathReason, entity.getMotivoMuerte());
        assertNull(entity.getFechaVenta());
        assertNull(entity.getPrecioVenta());
    }
    
    @Test
    void toEntity_withNullAnimal_shouldThrowException() {
        // When/Then: debe lanzar excepción con animal null
        assertThrows(IllegalArgumentException.class, () -> AnimalMapper.toEntity(null));
    }
    
    @Test
    void toDomain_withValidEntity_shouldMapAllFields() {
        // Given: una entidad JPA completa
        UUID animalId = UUID.randomUUID();
        UUID ranchoId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID madreId = UUID.randomUUID();
        UUID padreId = UUID.randomUUID();
        Instant now = Instant.now();
        LocalDate birthDate = LocalDate.of(2021, 7, 22);
        
        AnimalEntity entity = AnimalEntity.builder()
            .animalId(animalId)
            .arete("MX55555")
            .areteAnterior("MX44444")
            .sexo(AnimalEntity.SexEnum.MACHO)
            .raza(AnimalEntity.BreedEnum.SIMMENTAL)
            .fechaNacimiento(birthDate)
            .meses(40)
            .fechaAretado(birthDate.plusDays(10))
            .tipo(AnimalEntity.CattleTypeEnum.SEMENTAL)
            .status(AnimalEntity.CattleStatusEnum.ACTIVA)
            .folioReemo("REEMO-2024-999")
            .observaciones("Semental de alta calidad")
            .madreId(madreId)
            .padreId(padreId)
            .ranchoId(ranchoId)
            .tenantId(tenantId)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
        
        // When: se convierte a dominio
        Animal domain = AnimalMapper.toDomain(entity);
        
        // Then: todos los campos deben estar mapeados correctamente
        assertNotNull(domain);
        assertEquals(animalId, domain.getAnimalId());
        assertEquals("MX55555", domain.getArete());
        assertEquals("MX44444", domain.getAreteAnterior());
        assertEquals(Sex.MACHO, domain.getSexo());
        assertEquals(Breed.SIMMENTAL, domain.getRaza());
        assertEquals(birthDate, domain.getFechaNacimiento());
        assertEquals(40, domain.getMeses());
        assertEquals(birthDate.plusDays(10), domain.getFechaAretado());
        assertEquals(CattleType.SEMENTAL, domain.getTipo());
        assertEquals(CattleStatus.ACTIVA, domain.getStatus());
        assertEquals("REEMO-2024-999", domain.getFolioReemo());
        assertEquals("Semental de alta calidad", domain.getNota());
        assertEquals(madreId, domain.getMadreId());
        assertEquals(padreId, domain.getPadreId());
        assertEquals(ranchoId, domain.getRanchoId());
        assertEquals(tenantId, domain.getTenantId());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
        assertEquals(createdBy, domain.getCreatedBy());
        assertEquals(createdBy, domain.getUpdatedBy());
    }
    
    @Test
    void toDomain_withNullEntity_shouldThrowException() {
        // When/Then: debe lanzar excepción con entidad null
        assertThrows(IllegalArgumentException.class, () -> AnimalMapper.toDomain(null));
    }
    
    @Test
    void toEntityAndBack_shouldPreserveAllData() {
        // Given: un animal de dominio
        Animal originalDomain = Animal.create(
            "MX77777",
            Sex.HEMBRA,
            Breed.CRIOLLO,
            LocalDate.of(2023, 2, 14),
            CattleType.VENTA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        // When: se convierte a entidad y de vuelta a dominio
        AnimalEntity entity = AnimalMapper.toEntity(originalDomain);
        Animal resultDomain = AnimalMapper.toDomain(entity);
        
        // Then: todos los datos deben preservarse
        assertNotNull(resultDomain);
        assertEquals(originalDomain.getAnimalId(), resultDomain.getAnimalId());
        assertEquals(originalDomain.getArete(), resultDomain.getArete());
        assertEquals(originalDomain.getSexo(), resultDomain.getSexo());
        assertEquals(originalDomain.getRaza(), resultDomain.getRaza());
        assertEquals(originalDomain.getFechaNacimiento(), resultDomain.getFechaNacimiento());
        assertEquals(originalDomain.getTipo(), resultDomain.getTipo());
        assertEquals(originalDomain.getStatus(), resultDomain.getStatus());
        assertEquals(originalDomain.getRanchoId(), resultDomain.getRanchoId());
        assertEquals(originalDomain.getTenantId(), resultDomain.getTenantId());
    }
    
    @Test
    void shouldMapAllEnumValues_Sex() {
        // Given/When/Then: todos los valores de Sex deben mapearse correctamente
        for (Sex sex : Sex.values()) {
            Animal domain = new Animal.Builder()
                .animalId(UUID.randomUUID())
                .arete("TEST")
                .sexo(sex)
                .raza(Breed.ANGUS)
                .fechaNacimiento(LocalDate.now().minusYears(2))
                .meses(24)
                .tipo(CattleType.VENTA)
                .status(CattleStatus.ACTIVA)
                .ranchoId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(UUID.randomUUID())
                .updatedBy(UUID.randomUUID())
                .build();
            
            AnimalEntity entity = AnimalMapper.toEntity(domain);
            Animal backToDomain = AnimalMapper.toDomain(entity);
            
            assertEquals(sex, backToDomain.getSexo());
        }
    }
    
    @Test
    void shouldMapAllEnumValues_Breed() {
        // Given/When/Then: todos los valores de Breed deben mapearse correctamente
        for (Breed breed : Breed.values()) {
            Animal domain = new Animal.Builder()
                .animalId(UUID.randomUUID())
                .arete("TEST")
                .sexo(Sex.MACHO)
                .raza(breed)
                .fechaNacimiento(LocalDate.now().minusYears(2))
                .meses(24)
                .tipo(CattleType.VENTA)
                .status(CattleStatus.ACTIVA)
                .ranchoId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(UUID.randomUUID())
                .updatedBy(UUID.randomUUID())
                .build();
            
            AnimalEntity entity = AnimalMapper.toEntity(domain);
            Animal backToDomain = AnimalMapper.toDomain(entity);
            
            assertEquals(breed, backToDomain.getRaza());
        }
    }
    
    @Test
    void shouldMapAllEnumValues_CattleStatus() {
        // Given/When/Then: todos los valores de CattleStatus deben mapearse correctamente
        for (CattleStatus status : CattleStatus.values()) {
            Animal domain = new Animal.Builder()
                .animalId(UUID.randomUUID())
                .arete("TEST")
                .sexo(Sex.HEMBRA)
                .raza(Breed.CHAROLAIS)
                .fechaNacimiento(LocalDate.now().minusYears(2))
                .meses(24)
                .tipo(CattleType.VENTA)
                .status(status)
                .ranchoId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(UUID.randomUUID())
                .updatedBy(UUID.randomUUID())
                .build();
            
            AnimalEntity entity = AnimalMapper.toEntity(domain);
            Animal backToDomain = AnimalMapper.toDomain(entity);
            
            assertEquals(status, backToDomain.getStatus());
        }
    }
    
    @Test
    void shouldMapAllEnumValues_CattleType() {
        // Given/When/Then: todos los valores de CattleType deben mapearse correctamente
        for (CattleType type : CattleType.values()) {
            Animal domain = new Animal.Builder()
                .animalId(UUID.randomUUID())
                .arete("TEST")
                .sexo(Sex.MACHO)
                .raza(Breed.BRAHMAN)
                .fechaNacimiento(LocalDate.now().minusYears(2))
                .meses(24)
                .tipo(type)
                .status(CattleStatus.ACTIVA)
                .ranchoId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(UUID.randomUUID())
                .updatedBy(UUID.randomUUID())
                .build();
            
            AnimalEntity entity = AnimalMapper.toEntity(domain);
            Animal backToDomain = AnimalMapper.toDomain(entity);
            
            assertEquals(type, backToDomain.getTipo());
        }
    }
    
    @Test
    void shouldHandleNullableFields() {
        // Given: un animal con campos opcionales en null
        Animal domain = new Animal.Builder()
            .animalId(UUID.randomUUID())
            .arete("MX88888")
            .areteAnterior(null)  // nullable
            .sexo(Sex.HEMBRA)
            .raza(Breed.ANGUS)
            .fechaNacimiento(LocalDate.of(2022, 5, 1))
            .meses(30)
            .fechaAretado(null)  // nullable
            .tipo(CattleType.VENTA)
            .status(CattleStatus.ACTIVA)
            .folioReemo(null)  // nullable
            .nota(null)  // nullable
            .madreId(null)  // nullable
            .padreId(null)  // nullable
            .ranchoId(UUID.randomUUID())
            .tenantId(UUID.randomUUID())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .fechaVenta(null)
            .precioVenta(null)
            .fechaMuerte(null)
            .motivoMuerte(null)
            .build();
        
        // When: se convierte a entidad y de vuelta
        AnimalEntity entity = AnimalMapper.toEntity(domain);
        Animal result = AnimalMapper.toDomain(entity);
        
        // Then: los campos nullable deben preservarse como null
        assertNull(result.getAreteAnterior());
        assertNull(result.getFechaAretado());
        assertNull(result.getFolioReemo());
        assertNull(result.getNota());
        assertNull(result.getMadreId());
        assertNull(result.getPadreId());
        assertNull(result.getFechaVenta());
        assertNull(result.getPrecioVenta());
        assertNull(result.getFechaMuerte());
        assertNull(result.getMotivoMuerte());
    }
}

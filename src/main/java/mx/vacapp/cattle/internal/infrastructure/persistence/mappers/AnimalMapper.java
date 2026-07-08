package mx.vacapp.cattle.internal.infrastructure.persistence.mappers;

import mx.vacapp.cattle.internal.domain.model.*;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.AnimalEntity;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.AnimalEntity.BreedEnum;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.AnimalEntity.CattleStatusEnum;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.AnimalEntity.CattleTypeEnum;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.AnimalEntity.SexEnum;

/**
 * Mapper para transformar entre Animal (dominio) y AnimalEntity (JPA).
 * 
 * Este mapper es la frontera entre la capa de dominio pura (sin JPA) y
 * la capa de infraestructura (con anotaciones Hibernate).
 * 
 * Responsabilidades:
 * - Convertir Animal (POJO inmutable) a AnimalEntity (entidad JPA mutable)
 * - Convertir AnimalEntity a Animal
 * - Mapear enums entre sus representaciones de dominio y JPA
 * - Manejar campos nullable correctamente
 * 
 * Mapea los 29 campos de AnimalEntity bidireconalmente:
 * 1. animalId
 * 2. arete
 * 3. areteAnterior
 * 4. sexo (enum)
 * 5. raza (enum)
 * 6. fechaNacimiento
 * 7. meses (calculado, solo lectura desde entity)
 * 8. fechaAretado
 * 9. pesoNacimientoKg
 * 10. porcentajeGenetica
 * 11. procedencia
 * 12. lote
 * 13. tipo (enum)
 * 14. status (enum)
 * 15. folioReemo
 * 16. folioSiniiga
 * 17. observaciones (mapea a nota en dominio)
 * 18. madreId
 * 19. padreId
 * 20. ranchoId
 * 21. tenantId
 * 22. createdAt
 * 23. updatedAt
 * 24. createdBy
 * 25. updatedBy
 * 26. fechaVenta
 * 27. precioVenta
 * 28. fechaMuerte
 * 29. motivoMuerte
 */
public class AnimalMapper {
    
    /**
     * Constructor privado para prevenir instanciación.
     * Todos los métodos son estáticos.
     */
    private AnimalMapper() {
        throw new UnsupportedOperationException("Utility class - no debe ser instanciada");
    }
    
    /**
     * Convierte una entidad de dominio Animal a una entidad JPA AnimalEntity.
     * 
     * @param domain entidad de dominio Animal
     * @return entidad JPA AnimalEntity con todos los campos mapeados
     * @throws IllegalArgumentException si domain es null
     */
    public static AnimalEntity toEntity(Animal domain) {
        if (domain == null) {
            throw new IllegalArgumentException("Animal de dominio no puede ser null");
        }
        
        return AnimalEntity.builder()
            // Identificación
            .animalId(domain.getAnimalId())
            .arete(domain.getArete())
            .areteAnterior(domain.getAreteAnterior())
            
            // Información biológica
            .sexo(toSexEnum(domain.getSexo()))
            .raza(toBreedEnum(domain.getRaza()))
            .fechaNacimiento(domain.getFechaNacimiento())
            .meses(domain.getMeses())
            .fechaAretado(domain.getFechaAretado())
            
            // Campos adicionales que no existen en dominio básico
            // Se dejan en null si no están en el modelo de dominio
            .pesoNacimientoKg(null)
            .porcentajeGenetica(null)
            .procedencia(null)
            .lote(null)
            
            // Clasificación comercial
            .tipo(toCattleTypeEnum(domain.getTipo()))
            .status(toCattleStatusEnum(domain.getStatus()))
            
            // Información legal y adicional
            .folioReemo(domain.getFolioReemo())
            .folioSiniiga(null)  // No existe en dominio actual
            .observaciones(domain.getNota())  // Mapeo: nota (dominio) → observaciones (entity)
            
            // Genealogía
            .madreId(domain.getMadreId())
            .padreId(domain.getPadreId())
            
            // Contexto organizacional
            .ranchoId(domain.getRanchoId())
            .tenantId(domain.getTenantId())
            
            // Auditoría
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .createdBy(domain.getCreatedBy())
            .updatedBy(domain.getUpdatedBy())
            
            // Campos de venta
            .fechaVenta(domain.getFechaVenta())
            .precioVenta(domain.getPrecioVenta())
            
            // Campos de muerte
            .fechaMuerte(domain.getFechaMuerte())
            .motivoMuerte(domain.getMotivoMuerte())
            
            .build();
    }
    
    /**
     * Convierte una entidad JPA AnimalEntity a una entidad de dominio Animal.
     * 
     * @param entity entidad JPA AnimalEntity
     * @return entidad de dominio Animal con todos los campos mapeados
     * @throws IllegalArgumentException si entity es null
     */
    public static Animal toDomain(AnimalEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("AnimalEntity no puede ser null");
        }
        
        return new Animal.Builder()
            // Identificación
            .animalId(entity.getAnimalId())
            .arete(entity.getArete())
            .areteAnterior(entity.getAreteAnterior())
            
            // Información biológica
            .sexo(toSex(entity.getSexo()))
            .raza(toBreed(entity.getRaza()))
            .fechaNacimiento(entity.getFechaNacimiento())
            .meses(entity.getMeses())
            .fechaAretado(entity.getFechaAretado())
            
            // Clasificación comercial
            .tipo(toCattleType(entity.getTipo()))
            .status(toCattleStatus(entity.getStatus()))
            
            // Información legal y adicional
            .folioReemo(entity.getFolioReemo())
            .nota(entity.getObservaciones())  // Mapeo: observaciones (entity) → nota (dominio)
            
            // Genealogía
            .madreId(entity.getMadreId())
            .padreId(entity.getPadreId())
            
            // Contexto organizacional
            .ranchoId(entity.getRanchoId())
            .tenantId(entity.getTenantId())
            
            // Auditoría
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            
            // Campos de venta
            .fechaVenta(entity.getFechaVenta())
            .precioVenta(entity.getPrecioVenta())
            
            // Campos de muerte
            .fechaMuerte(entity.getFechaMuerte())
            .motivoMuerte(entity.getMotivoMuerte())
            
            .build();
    }
    
    // ========== Métodos privados de conversión de enums ==========
    
    /**
     * Convierte Sex (dominio) a SexEnum (JPA).
     */
    private static SexEnum toSexEnum(Sex sex) {
        if (sex == null) {
            return null;
        }
        return switch (sex) {
            case MACHO -> SexEnum.MACHO;
            case HEMBRA -> SexEnum.HEMBRA;
        };
    }
    
    /**
     * Convierte SexEnum (JPA) a Sex (dominio).
     */
    private static Sex toSex(SexEnum sexEnum) {
        if (sexEnum == null) {
            return null;
        }
        return switch (sexEnum) {
            case MACHO -> Sex.MACHO;
            case HEMBRA -> Sex.HEMBRA;
        };
    }
    
    /**
     * Convierte Breed (dominio) a BreedEnum (JPA).
     */
    private static BreedEnum toBreedEnum(Breed breed) {
        if (breed == null) {
            return null;
        }
        return switch (breed) {
            case CHAROLAIS -> BreedEnum.CHAROLAIS;
            case ANGUS -> BreedEnum.ANGUS;
            case BRAHMAN -> BreedEnum.BRAHMAN;
            case HEREFORD -> BreedEnum.HEREFORD;
            case SIMMENTAL -> BreedEnum.SIMMENTAL;
            case LIMOUSIN -> BreedEnum.LIMOUSIN;
            case CRIOLLO -> BreedEnum.CRIOLLO;
            case BRANGUS -> BreedEnum.BRANGUS;
            case SANTA_GERTRUDIS -> BreedEnum.SANTA_GERTRUDIS;
            case CRUZADA -> BreedEnum.CRUZADA;
        };
    }
    
    /**
     * Convierte BreedEnum (JPA) a Breed (dominio).
     */
    private static Breed toBreed(BreedEnum breedEnum) {
        if (breedEnum == null) {
            return null;
        }
        return switch (breedEnum) {
            case CHAROLAIS -> Breed.CHAROLAIS;
            case ANGUS -> Breed.ANGUS;
            case BRAHMAN -> Breed.BRAHMAN;
            case HEREFORD -> Breed.HEREFORD;
            case SIMMENTAL -> Breed.SIMMENTAL;
            case LIMOUSIN -> Breed.LIMOUSIN;
            case CRIOLLO -> Breed.CRIOLLO;
            case BRANGUS -> Breed.BRANGUS;
            case SANTA_GERTRUDIS -> Breed.SANTA_GERTRUDIS;
            case CRUZADA -> Breed.CRUZADA;
        };
    }
    
    /**
     * Convierte CattleType (dominio) a CattleTypeEnum (JPA).
     */
    private static CattleTypeEnum toCattleTypeEnum(CattleType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case VENTA -> CattleTypeEnum.VENTA;
            case CRIA -> CattleTypeEnum.CRIA;
            case ENGORDA -> CattleTypeEnum.ENGORDA;
            case SEMENTAL -> CattleTypeEnum.SEMENTAL;
            case VIENTRE -> CattleTypeEnum.VIENTRE;
        };
    }
    
    /**
     * Convierte CattleTypeEnum (JPA) a CattleType (dominio).
     */
    private static CattleType toCattleType(CattleTypeEnum typeEnum) {
        if (typeEnum == null) {
            return null;
        }
        return switch (typeEnum) {
            case VENTA -> CattleType.VENTA;
            case CRIA -> CattleType.CRIA;
            case ENGORDA -> CattleType.ENGORDA;
            case SEMENTAL -> CattleType.SEMENTAL;
            case VIENTRE -> CattleType.VIENTRE;
        };
    }
    
    /**
     * Convierte CattleStatus (dominio) a CattleStatusEnum (JPA).
     */
    private static CattleStatusEnum toCattleStatusEnum(CattleStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVA -> CattleStatusEnum.ACTIVA;
            case VENDIDA -> CattleStatusEnum.VENDIDA;
            case MUERTA -> CattleStatusEnum.MUERTA;
            case PRESTADA -> CattleStatusEnum.PRESTADA;
            case PRENADA -> CattleStatusEnum.PRENADA;
            case EN_REPOSO -> CattleStatusEnum.EN_REPOSO;
        };
    }
    
    /**
     * Convierte CattleStatusEnum (JPA) a CattleStatus (dominio).
     */
    private static CattleStatus toCattleStatus(CattleStatusEnum statusEnum) {
        if (statusEnum == null) {
            return null;
        }
        return switch (statusEnum) {
            case ACTIVA -> CattleStatus.ACTIVA;
            case VENDIDA -> CattleStatus.VENDIDA;
            case MUERTA -> CattleStatus.MUERTA;
            case PRESTADA -> CattleStatus.PRESTADA;
            case PRENADA -> CattleStatus.PRENADA;
            case EN_REPOSO -> CattleStatus.EN_REPOSO;
        };
    }
}

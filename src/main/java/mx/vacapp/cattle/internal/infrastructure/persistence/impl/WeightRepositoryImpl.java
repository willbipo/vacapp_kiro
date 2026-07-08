package mx.vacapp.cattle.internal.infrastructure.persistence.impl;

import lombok.RequiredArgsConstructor;
import mx.vacapp.cattle.internal.domain.model.WeightRecord;
import mx.vacapp.cattle.internal.domain.repository.WeightRepository;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.WeightEntity;
import mx.vacapp.cattle.internal.infrastructure.persistence.mappers.WeightMapper;
import mx.vacapp.cattle.internal.infrastructure.persistence.repositories.WeightJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del puerto WeightRepository usando Spring Data JPA.
 * 
 * Esta clase es un adaptador de infraestructura que implementa el puerto de salida
 * definido en la capa de dominio (WeightRepository). Traduce las operaciones de dominio
 * a operaciones JPA, utilizando WeightJpaRepository para la persistencia y WeightMapper
 * para la transformación entre entidades de dominio (WeightRecord) y entidades JPA (WeightEntity).
 * 
 * Responsabilidades:
 * - Implementar todas las operaciones definidas en WeightRepository
 * - Mapear entre WeightRecord (dominio) y WeightEntity (JPA)
 * - Delegar operaciones de persistencia a WeightJpaRepository
 * - Garantizar que todas las consultas respeten el contexto de multi-tenancy (si aplica)
 * 
 * IMPORTANTE: Los registros de peso NO se filtran por tenant_id directamente en esta capa,
 * ya que el filtro de tenant se aplica a nivel del animal. Si un animal existe y es accesible
 * por el tenant actual, sus pesos también lo son.
 */
@Repository
@RequiredArgsConstructor
public class WeightRepositoryImpl implements WeightRepository {
    
    private final WeightJpaRepository weightJpaRepository;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public WeightRecord save(WeightRecord weightRecord) {
        if (weightRecord == null) {
            throw new IllegalArgumentException("WeightRecord no puede ser null");
        }
        
        WeightEntity entity = WeightMapper.toEntity(weightRecord);
        WeightEntity savedEntity = weightJpaRepository.save(entity);
        return WeightMapper.toDomain(savedEntity);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<WeightRecord> findByAnimalId(UUID animalId) {
        List<WeightEntity> entities = weightJpaRepository.findByAnimalIdOrderByFechaPesajeDesc(animalId);
        return entities.stream()
            .map(WeightMapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<WeightRecord> findPreviousWeight(UUID animalId, LocalDate fechaPesaje) {
        Optional<WeightEntity> entityOpt = weightJpaRepository
            .findFirstByAnimalIdAndFechaPesajeLessThanOrderByFechaPesajeDesc(animalId, fechaPesaje);
        return entityOpt.map(WeightMapper::toDomain);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<WeightRecord> findLatestWeight(UUID animalId) {
        Optional<WeightEntity> entityOpt = weightJpaRepository
            .findFirstByAnimalIdOrderByFechaPesajeDesc(animalId);
        return entityOpt.map(WeightMapper::toDomain);
    }
}

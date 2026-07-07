package mx.vacapp.geography.internal.infrastructure.persistence.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.domain.model.GeographicStatus;
import mx.vacapp.geography.internal.domain.model.Potrero;
import mx.vacapp.geography.internal.domain.repository.PotreroRepository;
import mx.vacapp.geography.internal.infrastructure.persistence.entities.PotreroEntity;
import mx.vacapp.geography.internal.infrastructure.persistence.mappers.PotreroMapper;
import mx.vacapp.geography.internal.infrastructure.persistence.repositories.PotreroJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio de potreros.
 * Adaptador que conecta el puerto de dominio con JPA.
 * 
 * Responsabilidades:
 * - Transformar entre entidades de dominio y JPA usando PotreroMapper
 * - Aplicar filtros multi-tenant automáticamente
 * - Lanzar excepciones de dominio cuando corresponda
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class PotreroRepositoryImpl implements PotreroRepository {
    
    private final PotreroJpaRepository jpaRepository;
    private final PotreroMapper mapper;
    
    @Override
    public Potrero save(Potrero potrero) {
        log.debug("Guardando potrero: {}, rancho: {}, sección: {}, tenant: {}", 
                  potrero.getNombre(), potrero.getRanchoId(), 
                  potrero.getSeccionId(), potrero.getTenantId());
        
        PotreroEntity entity = mapper.toEntity(potrero);
        PotreroEntity saved = jpaRepository.save(entity);
        
        log.info("Potrero guardado exitosamente: ID={}, tenant={}", 
                 saved.getPotreroId(), saved.getTenantId());
        
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Potrero> findById(UUID potreroId) {
        log.debug("Buscando potrero por ID: {}", potreroId);
        
        // TODO: Extraer tenantId del contexto de seguridad
        Optional<PotreroEntity> entity = jpaRepository.findById(potreroId);
        
        if (entity.isPresent()) {
            log.debug("Potrero encontrado: {}", entity.get().getNombre());
            return entity.map(mapper::toDomain);
        }
        
        log.debug("Potrero no encontrado con ID: {}", potreroId);
        return Optional.empty();
    }
    
    @Override
    public boolean existsByNombreAndRanchoIdAndTenantId(String nombre, UUID ranchoId, UUID tenantId) {
        log.debug("Verificando existencia de potrero: nombre='{}', rancho={}, tenant={}", 
                  nombre, ranchoId, tenantId);
        
        boolean exists = jpaRepository.existsByNombreIgnoreCaseAndRanchoIdAndTenantId(
                nombre, ranchoId, tenantId);
        
        log.debug("Potrero existe: {}", exists);
        return exists;
    }
    
    @Override
    public boolean existsByNombreAndSeccionIdAndTenantId(String nombre, UUID seccionId, UUID tenantId) {
        log.debug("Verificando existencia de potrero: nombre='{}', sección={}, tenant={}", 
                  nombre, seccionId, tenantId);
        
        boolean exists = jpaRepository.existsByNombreIgnoreCaseAndSeccionIdAndTenantId(
                nombre, seccionId, tenantId);
        
        log.debug("Potrero existe: {}", exists);
        return exists;
    }
    
    @Override
    public List<Potrero> findByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId) {
        log.debug("Listando potreros: rancho={}, tenant={}", ranchoId, tenantId);
        
        List<PotreroEntity> entities = jpaRepository.findByRanchoIdAndTenantId(ranchoId, tenantId);
        
        log.debug("Encontrados {} potreros", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Potrero> findBySeccionIdAndTenantId(UUID seccionId, UUID tenantId) {
        log.debug("Listando potreros: sección={}, tenant={}", seccionId, tenantId);
        
        List<PotreroEntity> entities = jpaRepository.findBySeccionIdAndTenantId(seccionId, tenantId);
        
        log.debug("Encontrados {} potreros", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Potrero> findByTenantId(UUID tenantId, int page, int size) {
        log.debug("Listando potreros: tenant={}, page={}, size={}", tenantId, page, size);
        
        PageRequest pageable = PageRequest.of(page, size);
        List<PotreroEntity> entities = jpaRepository.findByTenantId(tenantId, pageable).getContent();
        
        log.debug("Encontrados {} potreros", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public long countByTenantId(UUID tenantId) {
        log.debug("Contando potreros del tenant: {}", tenantId);
        
        long count = jpaRepository.countByTenantId(tenantId);
        
        log.debug("Total de potreros: {}", count);
        return count;
    }
    
    @Override
    public List<Potrero> findActiveByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId) {
        log.debug("Listando potreros activos: rancho={}, tenant={}", ranchoId, tenantId);
        
        List<PotreroEntity> entities = jpaRepository.findByRanchoIdAndTenantIdAndStatus(
                ranchoId, tenantId, GeographicStatus.ACTIVE);
        
        log.debug("Encontrados {} potreros activos", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Potrero> findActiveBySeccionIdAndTenantId(UUID seccionId, UUID tenantId) {
        log.debug("Listando potreros activos: sección={}, tenant={}", seccionId, tenantId);
        
        List<PotreroEntity> entities = jpaRepository.findBySeccionIdAndTenantIdAndStatus(
                seccionId, tenantId, GeographicStatus.ACTIVE);
        
        log.debug("Encontrados {} potreros activos", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Potrero> findDirectByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId) {
        log.debug("Listando potreros directos: rancho={}, tenant={}", ranchoId, tenantId);
        
        List<PotreroEntity> entities = jpaRepository.findByRanchoIdAndTenantIdAndSeccionIdIsNull(
                ranchoId, tenantId);
        
        log.debug("Encontrados {} potreros directos", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Potrero> findActiveDirectByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId) {
        log.debug("Listando potreros activos directos: rancho={}, tenant={}", ranchoId, tenantId);
        
        List<PotreroEntity> entities = jpaRepository.findByRanchoIdAndTenantIdAndSeccionIdIsNullAndStatus(
                ranchoId, tenantId, GeographicStatus.ACTIVE);
        
        log.debug("Encontrados {} potreros activos directos", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

package mx.vacapp.geography.internal.infrastructure.persistence.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.domain.model.GeographicStatus;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.repository.RanchoRepository;
import mx.vacapp.geography.internal.infrastructure.persistence.entities.RanchoEntity;
import mx.vacapp.geography.internal.infrastructure.persistence.mappers.RanchoMapper;
import mx.vacapp.geography.internal.infrastructure.persistence.repositories.RanchoJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio de ranchos.
 * Adaptador que conecta el puerto de dominio con JPA.
 * 
 * Responsabilidades:
 * - Transformar entre entidades de dominio y JPA usando RanchoMapper
 * - Aplicar filtros multi-tenant automáticamente
 * - Lanzar excepciones de dominio cuando corresponda
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RanchoRepositoryImpl implements RanchoRepository {
    
    private final RanchoJpaRepository jpaRepository;
    private final RanchoMapper mapper;
    
    @Override
    public Rancho save(Rancho rancho) {
        log.debug("Guardando rancho: {}, tenant: {}", rancho.getNombre(), rancho.getTenantId());
        
        RanchoEntity entity = mapper.toEntity(rancho);
        RanchoEntity saved = jpaRepository.save(entity);
        
        log.info("Rancho guardado exitosamente: ID={}, tenant={}", 
                 saved.getRanchoId(), saved.getTenantId());
        
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Rancho> findById(UUID ranchoId) {
        log.debug("Buscando rancho por ID: {}", ranchoId);
        
        // TODO: Extraer tenantId del contexto de seguridad
        // Por ahora usamos un enfoque más amplio - buscar por ID y validar tenant en capa superior
        Optional<RanchoEntity> entity = jpaRepository.findById(ranchoId);
        
        if (entity.isPresent()) {
            log.debug("Rancho encontrado: {}", entity.get().getNombre());
            return entity.map(mapper::toDomain);
        }
        
        log.debug("Rancho no encontrado con ID: {}", ranchoId);
        return Optional.empty();
    }
    
    @Override
    public boolean existsByNombreAndTenantId(String nombre, UUID tenantId) {
        log.debug("Verificando existencia de rancho: nombre='{}', tenant={}", nombre, tenantId);
        
        boolean exists = jpaRepository.existsByNombreIgnoreCaseAndTenantId(nombre, tenantId);
        
        log.debug("Rancho existe: {}", exists);
        return exists;
    }
    
    @Override
    public List<Rancho> findByTenantId(UUID tenantId, int page, int size) {
        log.debug("Listando ranchos: tenant={}, page={}, size={}", tenantId, page, size);
        
        PageRequest pageable = PageRequest.of(page, size);
        List<RanchoEntity> entities = jpaRepository.findByTenantId(tenantId, pageable).getContent();
        
        log.debug("Encontrados {} ranchos", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public long countByTenantId(UUID tenantId) {
        log.debug("Contando ranchos del tenant: {}", tenantId);
        
        long count = jpaRepository.countByTenantId(tenantId);
        
        log.debug("Total de ranchos: {}", count);
        return count;
    }
    
    @Override
    public List<Rancho> findActiveByTenantId(UUID tenantId) {
        log.debug("Listando ranchos activos: tenant={}", tenantId);
        
        List<RanchoEntity> entities = jpaRepository.findByTenantIdAndStatus(
                tenantId, GeographicStatus.ACTIVE);
        
        log.debug("Encontrados {} ranchos activos", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public BigDecimal sumSuperficieSeccionesByRanchoId(UUID ranchoId) {
        log.debug("Calculando suma de superficies de secciones para rancho: {}", ranchoId);
        
        BigDecimal sum = jpaRepository.sumSuperficieSeccionesByRanchoId(ranchoId);
        
        log.debug("Suma de superficies de secciones: {}", sum);
        return sum != null ? sum : BigDecimal.ZERO;
    }
    
    @Override
    public BigDecimal sumSuperficiePotrerosDirectosByRanchoId(UUID ranchoId) {
        log.debug("Calculando suma de superficies de potreros directos para rancho: {}", ranchoId);
        
        BigDecimal sum = jpaRepository.sumSuperficiePotrerosDirectosByRanchoId(ranchoId);
        
        log.debug("Suma de superficies de potreros directos: {}", sum);
        return sum != null ? sum : BigDecimal.ZERO;
    }
    
    @Override
    public long countSeccionesActivasByRanchoId(UUID ranchoId) {
        log.debug("Contando secciones activas del rancho: {}", ranchoId);
        
        long count = jpaRepository.countSeccionesActivasByRanchoId(ranchoId);
        
        log.debug("Total de secciones activas: {}", count);
        return count;
    }
    
    @Override
    public long countPotrerosActivosByRanchoId(UUID ranchoId) {
        log.debug("Contando potreros activos del rancho: {}", ranchoId);
        
        long count = jpaRepository.countPotrerosActivosByRanchoId(ranchoId);
        
        log.debug("Total de potreros activos: {}", count);
        return count;
    }
    
    @Override
    public boolean hasActiveSecciones(UUID ranchoId) {
        log.debug("Verificando si rancho tiene secciones activas: {}", ranchoId);
        
        boolean has = jpaRepository.hasActiveSecciones(ranchoId);
        
        log.debug("Rancho tiene secciones activas: {}", has);
        return has;
    }
    
    @Override
    public boolean hasActivePotreros(UUID ranchoId) {
        log.debug("Verificando si rancho tiene potreros activos: {}", ranchoId);
        
        boolean has = jpaRepository.hasActivePotreros(ranchoId);
        
        log.debug("Rancho tiene potreros activos: {}", has);
        return has;
    }
}

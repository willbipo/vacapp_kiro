package mx.vacapp.geography.internal.infrastructure.persistence.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.domain.model.GeographicStatus;
import mx.vacapp.geography.internal.domain.model.Seccion;
import mx.vacapp.geography.internal.domain.repository.SeccionRepository;
import mx.vacapp.geography.internal.infrastructure.persistence.entities.SeccionEntity;
import mx.vacapp.geography.internal.infrastructure.persistence.mappers.SeccionMapper;
import mx.vacapp.geography.internal.infrastructure.persistence.repositories.SeccionJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio de secciones.
 * Adaptador que conecta el puerto de dominio con JPA.
 * 
 * Responsabilidades:
 * - Transformar entre entidades de dominio y JPA usando SeccionMapper
 * - Aplicar filtros multi-tenant automáticamente
 * - Lanzar excepciones de dominio cuando corresponda
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class SeccionRepositoryImpl implements SeccionRepository {
    
    private final SeccionJpaRepository jpaRepository;
    private final SeccionMapper mapper;
    
    @Override
    public Seccion save(Seccion seccion) {
        log.debug("Guardando sección: {}, rancho: {}, tenant: {}", 
                  seccion.getNombre(), seccion.getRanchoId(), seccion.getTenantId());
        
        SeccionEntity entity = mapper.toEntity(seccion);
        SeccionEntity saved = jpaRepository.save(entity);
        
        log.info("Sección guardada exitosamente: ID={}, tenant={}", 
                 saved.getSeccionId(), saved.getTenantId());
        
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Seccion> findById(UUID seccionId) {
        log.debug("Buscando sección por ID: {}", seccionId);
        
        // TODO: Extraer tenantId del contexto de seguridad
        Optional<SeccionEntity> entity = jpaRepository.findById(seccionId);
        
        if (entity.isPresent()) {
            log.debug("Sección encontrada: {}", entity.get().getNombre());
            return entity.map(mapper::toDomain);
        }
        
        log.debug("Sección no encontrada con ID: {}", seccionId);
        return Optional.empty();
    }
    
    @Override
    public boolean existsByNombreAndRanchoIdAndTenantId(String nombre, UUID ranchoId, UUID tenantId) {
        log.debug("Verificando existencia de sección: nombre='{}', rancho={}, tenant={}", 
                  nombre, ranchoId, tenantId);
        
        boolean exists = jpaRepository.existsByNombreIgnoreCaseAndRanchoIdAndTenantId(
                nombre, ranchoId, tenantId);
        
        log.debug("Sección existe: {}", exists);
        return exists;
    }
    
    @Override
    public List<Seccion> findByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId) {
        log.debug("Listando secciones: rancho={}, tenant={}", ranchoId, tenantId);
        
        List<SeccionEntity> entities = jpaRepository.findByRanchoIdAndTenantId(ranchoId, tenantId);
        
        log.debug("Encontradas {} secciones", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Seccion> findByTenantId(UUID tenantId, int page, int size) {
        log.debug("Listando secciones: tenant={}, page={}, size={}", tenantId, page, size);
        
        PageRequest pageable = PageRequest.of(page, size);
        List<SeccionEntity> entities = jpaRepository.findByTenantId(tenantId, pageable).getContent();
        
        log.debug("Encontradas {} secciones", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public long countByTenantId(UUID tenantId) {
        log.debug("Contando secciones del tenant: {}", tenantId);
        
        long count = jpaRepository.countByTenantId(tenantId);
        
        log.debug("Total de secciones: {}", count);
        return count;
    }
    
    @Override
    public List<Seccion> findActiveByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId) {
        log.debug("Listando secciones activas: rancho={}, tenant={}", ranchoId, tenantId);
        
        List<SeccionEntity> entities = jpaRepository.findByRanchoIdAndTenantIdAndStatus(
                ranchoId, tenantId, GeographicStatus.ACTIVE);
        
        log.debug("Encontradas {} secciones activas", entities.size());
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public BigDecimal sumSuperficiePotrerosSeccionId(UUID seccionId) {
        log.debug("Calculando suma de superficies de potreros para sección: {}", seccionId);
        
        BigDecimal sum = jpaRepository.sumSuperficiePotrerosSeccionId(seccionId);
        
        log.debug("Suma de superficies de potreros: {}", sum);
        return sum != null ? sum : BigDecimal.ZERO;
    }
    
    @Override
    public long countPotrerosActivosBySeccionId(UUID seccionId) {
        log.debug("Contando potreros activos de la sección: {}", seccionId);
        
        long count = jpaRepository.countPotrerosActivosBySeccionId(seccionId);
        
        log.debug("Total de potreros activos: {}", count);
        return count;
    }
    
    @Override
    public boolean hasActivePotreros(UUID seccionId) {
        log.debug("Verificando si sección tiene potreros activos: {}", seccionId);
        
        boolean has = jpaRepository.hasActivePotreros(seccionId);
        
        log.debug("Sección tiene potreros activos: {}", has);
        return has;
    }
}

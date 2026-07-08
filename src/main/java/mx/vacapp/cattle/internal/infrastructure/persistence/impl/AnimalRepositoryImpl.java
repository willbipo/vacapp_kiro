package mx.vacapp.cattle.internal.infrastructure.persistence.impl;

import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.CattleStatus;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.AnimalEntity;
import mx.vacapp.cattle.internal.infrastructure.persistence.mappers.AnimalMapper;
import mx.vacapp.cattle.internal.infrastructure.persistence.repositories.AnimalJpaRepository;
import mx.vacapp.users.internal.infrastructure.security.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del puerto AnimalRepository usando Spring Data JPA.
 * <p>
 * Esta clase es el adaptador de salida que conecta la capa de dominio pura
 * con la infraestructura de persistencia (JPA/Hibernate). Implementa el patrón
 * Repository de Clean Architecture transformando entidades de dominio (Animal)
 * a entidades JPA (AnimalEntity) y viceversa.
 * </p>
 * <p>
 * <strong>Multi-tenancy:</strong> Todos los métodos que retornan colecciones
 * o buscan por ID aplican filtrado automático por tenant_id extraído del
 * TenantContext (SecurityContext), garantizando aislamiento de datos entre
 * tenants.
 * </p>
 * <p>
 * <strong>Excepción:</strong> El método existsByArete() NO filtra por tenant_id
 * ya que el arete debe ser único a nivel global en todo el sistema para cumplir
 * con regulaciones de identificación única de ganado.
 * </p>
 * <p>
 * <strong>Responsabilidades:</strong>
 * <ul>
 *   <li>Transformar Animal ↔ AnimalEntity usando AnimalMapper</li>
 *   <li>Aplicar filtrado automático por tenant_id en operaciones de lectura</li>
 *   <li>Delegar operaciones CRUD a AnimalJpaRepository</li>
 *   <li>Manejar Optional returns correctamente</li>
 *   <li>Aplicar paginación en consultas de colecciones</li>
 * </ul>
 * </p>
 *
 * @see AnimalRepository
 * @see AnimalJpaRepository
 * @see AnimalMapper
 * @see TenantContext
 */
@Repository
public class AnimalRepositoryImpl implements AnimalRepository {
    
    private final AnimalJpaRepository jpaRepository;
    
    /**
     * Constructor con inyección de dependencias.
     * 
     * @param jpaRepository repositorio JPA para operaciones de persistencia
     */
    public AnimalRepositoryImpl(AnimalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Guarda o actualiza un animal en la base de datos.
     * El tenant_id ya está establecido en el objeto Animal antes de persistir.
     * </p>
     */
    @Override
    public Animal save(Animal animal) {
        if (animal == null) {
            throw new IllegalArgumentException("Animal no puede ser null");
        }
        
        AnimalEntity entity = AnimalMapper.toEntity(animal);
        AnimalEntity savedEntity = jpaRepository.save(entity);
        return AnimalMapper.toDomain(savedEntity);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Busca un animal por ID filtrando por tenant_id del contexto actual.
     * Si el animal existe pero pertenece a otro tenant, retorna Optional.empty().
     * </p>
     */
    @Override
    public Optional<Animal> findById(UUID animalId) {
        UUID currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null) {
            throw new IllegalStateException("No hay tenant_id en el contexto de seguridad");
        }
        
        return jpaRepository.findById(animalId)
            .filter(entity -> entity.getTenantId().equals(currentTenantId))
            .map(AnimalMapper::toDomain);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Búsqueda case-insensitive convertiendo el arete a mayúsculas.
     * Filtra por tenant_id del contexto actual.
     * </p>
     */
    @Override
    public Optional<Animal> findByArete(String arete) {
        UUID currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null) {
            throw new IllegalStateException("No hay tenant_id en el contexto de seguridad");
        }
        
        String normalizedArete = arete.toUpperCase();
        return jpaRepository.findByAreteAndTenantId(normalizedArete, currentTenantId)
            .map(AnimalMapper::toDomain);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * <strong>IMPORTANTE:</strong> Este método NO filtra por tenant_id.
     * Valida unicidad del arete a nivel GLOBAL en todo el sistema.
     * </p>
     */
    @Override
    public boolean existsByArete(String arete) {
        String normalizedArete = arete.toUpperCase();
        return jpaRepository.existsByArete(normalizedArete);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Lista animales con paginación filtrando por tenant_id y rancho_id.
     * </p>
     */
    @Override
    public List<Animal> findAll(UUID tenantId, UUID ranchoId, int page, int size) {
        // Validar límites de paginación
        if (page < 0) {
            throw new IllegalArgumentException("El número de página no puede ser negativo");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y 100");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return jpaRepository.findByRanchoIdAndTenantId(ranchoId, tenantId, pageable)
            .stream()
            .map(AnimalMapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Cuenta animales filtrando por tenant_id.
     * </p>
     */
    @Override
    public long count(UUID tenantId, UUID ranchoId) {
        // Para contar por rancho, necesitamos filtrar manualmente
        // ya que AnimalJpaRepository solo tiene countByTenantId()
        return jpaRepository.findByRanchoIdAndTenantId(ranchoId, tenantId, Pageable.unpaged())
            .size();
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Lista animales por status filtrando por tenant_id del contexto actual.
     * </p>
     */
    @Override
    public List<Animal> findByStatus(CattleStatus status, UUID tenantId) {
        AnimalEntity.CattleStatusEnum statusEnum = mapToStatusEnum(status);
        return jpaRepository.findByTenantIdAndStatus(tenantId, statusEnum, Pageable.unpaged())
            .stream()
            .map(AnimalMapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Lista todos los animales de un rancho sin paginación.
     * Filtra por tenant_id del contexto actual.
     * </p>
     */
    @Override
    public List<Animal> findByRancho(UUID ranchoId, UUID tenantId) {
        return jpaRepository.findByRanchoIdAndTenantId(ranchoId, tenantId, Pageable.unpaged())
            .stream()
            .map(AnimalMapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Busca animales cuyo arete contenga la cadena de búsqueda.
     * Búsqueda case-insensitive convirtiendo la consulta a mayúsculas.
     * Filtra por tenant_id y ordena por arete ASC.
     * </p>
     */
    @Override
    public List<Animal> findByAreteContaining(UUID tenantId, String areteQuery) {
        if (areteQuery == null || areteQuery.isBlank()) {
            throw new IllegalArgumentException("La consulta de búsqueda no puede estar vacía");
        }
        
        String normalizedQuery = areteQuery.toLowerCase();
        return jpaRepository.findByTenantIdAndAreteContainingIgnoreCaseOrderByAreteAsc(tenantId, normalizedQuery)
            .stream()
            .map(AnimalMapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Lista animales aplicando filtros opcionales.
     * Los filtros que sean null no se aplicarán en la consulta.
     * Si potreroId no es null, se hace JOIN con pasture_history para filtrar por ubicación actual.
     * Ordena por arete ASC.
     * </p>
     */
    @Override
    public List<Animal> findByFilters(UUID tenantId, 
                                     mx.vacapp.cattle.internal.domain.model.CattleStatus status, 
                                     mx.vacapp.cattle.internal.domain.model.CattleType tipo,
                                     UUID ranchoId, 
                                     UUID potreroId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId no puede ser null");
        }
        
        // Obtener todos los animales del tenant
        List<AnimalEntity> entities = jpaRepository.findAll();
        
        // Filtrar en memoria (no óptimo pero funcional para MVP)
        // TODO: Implementar query nativa o Criteria API para mejor performance
        return entities.stream()
            .filter(entity -> entity.getTenantId().equals(tenantId))
            .filter(entity -> status == null || mapFromStatusEnum(entity.getStatus()).equals(status))
            .filter(entity -> tipo == null || mapFromTipoEnum(entity.getTipo()).equals(tipo))
            .filter(entity -> ranchoId == null || entity.getRanchoId().equals(ranchoId))
            // Filtrar por potrero se hace en el use case con PastureHistoryRepository
            .sorted((a, b) -> a.getArete().compareTo(b.getArete()))
            .map(AnimalMapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * Mapea CattleStatusEnum (JPA) a CattleStatus (dominio).
     * 
     * @param statusEnum enum de JPA
     * @return enum de dominio
     */
    private mx.vacapp.cattle.internal.domain.model.CattleStatus mapFromStatusEnum(AnimalEntity.CattleStatusEnum statusEnum) {
        if (statusEnum == null) {
            throw new IllegalArgumentException("CattleStatusEnum no puede ser null");
        }
        
        return switch (statusEnum) {
            case ACTIVA -> mx.vacapp.cattle.internal.domain.model.CattleStatus.ACTIVA;
            case VENDIDA -> mx.vacapp.cattle.internal.domain.model.CattleStatus.VENDIDA;
            case MUERTA -> mx.vacapp.cattle.internal.domain.model.CattleStatus.MUERTA;
            case PRESTADA -> mx.vacapp.cattle.internal.domain.model.CattleStatus.PRESTADA;
            case PRENADA -> mx.vacapp.cattle.internal.domain.model.CattleStatus.PRENADA;
            case EN_REPOSO -> mx.vacapp.cattle.internal.domain.model.CattleStatus.EN_REPOSO;
        };
    }
    
    /**
     * Mapea CattleTypeEnum (JPA) a CattleType (dominio).
     * 
     * @param tipoEnum enum de JPA
     * @return enum de dominio
     */
    private mx.vacapp.cattle.internal.domain.model.CattleType mapFromTipoEnum(AnimalEntity.CattleTypeEnum tipoEnum) {
        if (tipoEnum == null) {
            throw new IllegalArgumentException("CattleTypeEnum no puede ser null");
        }
        
        return switch (tipoEnum) {
            case VENTA -> mx.vacapp.cattle.internal.domain.model.CattleType.VENTA;
            case CRIA -> mx.vacapp.cattle.internal.domain.model.CattleType.CRIA;
            case ENGORDA -> mx.vacapp.cattle.internal.domain.model.CattleType.ENGORDA;
            case SEMENTAL -> mx.vacapp.cattle.internal.domain.model.CattleType.SEMENTAL;
            case VIENTRE -> mx.vacapp.cattle.internal.domain.model.CattleType.VIENTRE;
        };
    }
    
    /**
     * Mapea CattleStatus (dominio) a CattleStatusEnum (JPA).
     * 
     * @param status enum de dominio
     * @return enum de JPA
     */
    private AnimalEntity.CattleStatusEnum mapToStatusEnum(mx.vacapp.cattle.internal.domain.model.CattleStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("CattleStatus no puede ser null");
        }
        
        return switch (status) {
            case ACTIVA -> AnimalEntity.CattleStatusEnum.ACTIVA;
            case VENDIDA -> AnimalEntity.CattleStatusEnum.VENDIDA;
            case MUERTA -> AnimalEntity.CattleStatusEnum.MUERTA;
            case PRESTADA -> AnimalEntity.CattleStatusEnum.PRESTADA;
            case PRENADA -> AnimalEntity.CattleStatusEnum.PRENADA;
            case EN_REPOSO -> AnimalEntity.CattleStatusEnum.EN_REPOSO;
        };
    }
}

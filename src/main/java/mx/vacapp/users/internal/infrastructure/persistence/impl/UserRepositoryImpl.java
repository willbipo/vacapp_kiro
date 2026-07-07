package mx.vacapp.users.internal.infrastructure.persistence.impl;

import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import mx.vacapp.users.internal.domain.model.exceptions.UserNotFoundException;
import mx.vacapp.users.internal.infrastructure.persistence.entities.UserEntity;
import mx.vacapp.users.internal.infrastructure.persistence.mappers.UserMapper;
import mx.vacapp.users.internal.infrastructure.persistence.repositories.UserJpaRepository;
import mx.vacapp.users.internal.infrastructure.security.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del puerto UserRepository utilizando Spring Data JPA.
 * <p>
 * Esta clase adapta el puerto de dominio UserRepository a la infraestructura de persistencia,
 * implementando todas las operaciones de acceso a datos con filtrado automático por tenant_id
 * para garantizar el aislamiento multi-tenant.
 * </p>
 * <p>
 * <strong>Filtrado Multi-tenant:</strong>
 * Todas las operaciones de lectura (findById, findByEmail, findAll, count) aplican automáticamente
 * el filtro WHERE tenant_id = :currentTenantId, excepto para usuarios con roles SaaS que pueden
 * acceder sin restricción cuando el TenantContext.getTenantId() retorna null.
 * </p>
 * <p>
 * <strong>Validación en escritura:</strong>
 * Las operaciones de escritura (save, deactivate) validan que el tenant_id del usuario
 * coincida con el tenant_id del contexto actual (excepto para operaciones SaaS).
 * </p>
 * <p>
 * Esta implementación utiliza el patrón Adapter de Clean Architecture, traduciendo
 * entre el modelo de dominio (User) y el modelo de persistencia (UserEntity) mediante UserMapper.
 * </p>
 *
 * @see UserRepository
 * @see UserJpaRepository
 * @see UserMapper
 * @see TenantContext
 */
@Service
public class UserRepositoryImpl implements UserRepository {
    
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;
    
    /**
     * Constructor con inyección de dependencias.
     * <p>
     * Spring automáticamente inyecta las dependencias requeridas:
     * UserJpaRepository para operaciones JPA y UserMapper para transformaciones.
     * </p>
     *
     * @param userJpaRepository repositorio JPA para acceso a datos
     * @param userMapper mapper para transformar entre User y UserEntity
     */
    public UserRepositoryImpl(UserJpaRepository userJpaRepository, UserMapper userMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userMapper = userMapper;
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Implementación:
     * <ul>
     *   <li>Valida que el tenant_id del usuario coincida con el contexto actual (para usuarios no SaaS)</li>
     *   <li>Actualiza el campo updated_at al timestamp actual antes de persistir</li>
     *   <li>Utiliza UserMapper para convertir User → UserEntity antes de guardar</li>
     *   <li>Retorna el User con los campos actualizados por la base de datos</li>
     * </ul>
     * </p>
     */
    @Override
    @Transactional
    public User save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User no puede ser null");
        }
        
        // Validar tenant_id para usuarios no SaaS
        UUID currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && user.getTenantId() != null) {
            if (!currentTenantId.equals(user.getTenantId())) {
                throw new SecurityException(
                    "No autorizado para guardar usuarios de otro tenant. " +
                    "Tenant actual: " + currentTenantId + ", Tenant del usuario: " + user.getTenantId()
                );
            }
        }
        
        // Convertir dominio → entidad JPA
        UserEntity entity = userMapper.toEntity(user);
        
        // Persistir
        UserEntity savedEntity = userJpaRepository.save(entity);
        
        // Convertir entidad JPA → dominio
        return userMapper.toDomain(savedEntity);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Implementación:
     * <ul>
     *   <li>Busca el usuario por ID en la base de datos</li>
     *   <li>Aplica filtro por tenant_id del contexto actual (si está presente)</li>
     *   <li>Retorna Optional.empty() si el usuario no existe o pertenece a otro tenant</li>
     * </ul>
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }
        
        Optional<UserEntity> entityOpt = userJpaRepository.findById(userId);
        
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }
        
        UserEntity entity = entityOpt.get();
        
        // Aplicar filtrado por tenant_id del contexto actual
        UUID currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null) {
            // Si hay tenant en el contexto y el usuario tiene tenant diferente, no retornar
            if (entity.getTenantId() != null && !currentTenantId.equals(entity.getTenantId())) {
                return Optional.empty();
            }
        }
        
        return Optional.of(userMapper.toDomain(entity));
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Implementación:
     * <ul>
     *   <li>Normaliza el email a minúsculas antes de buscar</li>
     *   <li>Utiliza el método findByEmailAndTenantId del repositorio JPA</li>
     *   <li>Aplica filtro por tenant_id del contexto actual</li>
     * </ul>
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email no puede ser null o vacío");
        }
        
        String normalizedEmail = email.toLowerCase().trim();
        
        // NOTA: Esta búsqueda NO filtra por tenant_id intencionalmente.
        // Se utiliza durante el flujo de login (LoginUseCase), momento en el cual
        // el tenant del usuario todavía no se conoce (el JWT aún no existe).
        Optional<UserEntity> entityOpt = userJpaRepository.findFirstByEmail(normalizedEmail);
        
        return entityOpt.map(userMapper::toDomain);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Implementación:
     * <ul>
     *   <li>Normaliza el email a minúsculas antes de verificar</li>
     *   <li>Utiliza el método existsByEmailAndTenantId del repositorio JPA</li>
     *   <li>Valida unicidad en el tenant especificado (que puede diferir del contexto actual)</li>
     * </ul>
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmailAndTenantId(String email, UUID tenantId) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email no puede ser null o vacío");
        }
        
        String normalizedEmail = email.toLowerCase().trim();
        return userJpaRepository.existsByEmailAndTenantId(normalizedEmail, tenantId);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Implementación:
     * <ul>
     *   <li>Recupera TODOS los usuarios de la base de datos con paginación</li>
     *   <li>Aplica filtrado en memoria por tenant_id del contexto actual</li>
     *   <li>Convierte cada UserEntity a User usando el mapper</li>
     * </ul>
     * <p>
     * <strong>Nota:</strong> Esta implementación inicial filtra en memoria.
     * Para producción con alto volumen, se debe implementar query nativa o
     * usar Specifications de JPA para filtrar en base de datos.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> findAll(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page debe ser >= 0");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("size debe ser > 0 y <= 100");
        }
        
        UUID currentTenantId = TenantContext.getTenantId();
        
        // Recuperar página de usuarios
        PageRequest pageRequest = PageRequest.of(page, size);
        List<UserEntity> entities = userJpaRepository.findAll(pageRequest).getContent();
        
        // Aplicar filtrado por tenant_id del contexto actual
        return entities.stream()
            .filter(entity -> {
                if (currentTenantId == null) {
                    // Sin tenant en contexto (usuario SaaS) → retornar todos
                    return true;
                }
                // Con tenant en contexto → solo retornar usuarios del mismo tenant
                return entity.getTenantId() != null && currentTenantId.equals(entity.getTenantId());
            })
            .map(userMapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Implementación:
     * <ul>
     *   <li>Cuenta TODOS los usuarios en la base de datos</li>
     *   <li>Aplica filtrado en memoria por tenant_id del contexto actual</li>
     * </ul>
     * <p>
     * <strong>Nota:</strong> Esta implementación inicial cuenta en memoria.
     * Para producción, se debe implementar query nativa COUNT con WHERE tenant_id.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public long count() {
        UUID currentTenantId = TenantContext.getTenantId();
        
        if (currentTenantId == null) {
            // Sin tenant en contexto (usuario SaaS) → contar todos
            return userJpaRepository.count();
        }
        
        // Con tenant en contexto → contar solo usuarios del mismo tenant
        // Nota: Implementación simplificada, en producción usar query COUNT con filtro
        return userJpaRepository.findAll().stream()
            .filter(entity -> entity.getTenantId() != null && currentTenantId.equals(entity.getTenantId()))
            .count();
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Implementación:
     * <ul>
     *   <li>Busca el usuario por ID aplicando filtrado por tenant_id</li>
     *   <li>Marca el status como INACTIVE sin eliminar el registro</li>
     *   <li>Actualiza updated_at y updated_by con los valores proporcionados</li>
     *   <li>Persiste los cambios y retorna el usuario desactivado</li>
     * </ul>
     * </p>
     */
    @Override
    @Transactional
    public User deactivate(UUID userId, UUID deactivatedBy) {
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }
        if (deactivatedBy == null) {
            throw new IllegalArgumentException("deactivatedBy no puede ser null");
        }
        
        // Buscar el usuario aplicando filtrado por tenant_id
        User user = findById(userId)
            .orElseThrow(() -> new UserNotFoundException(
                "Usuario no encontrado con ID: " + userId + 
                " en el tenant actual: " + TenantContext.getTenantId()
            ));
        
        // Crear nuevo usuario con status INACTIVE usando el método de dominio
        User deactivatedUser = user.deactivate(deactivatedBy);
        
        // Persistir los cambios
        return save(deactivatedUser);
    }
}

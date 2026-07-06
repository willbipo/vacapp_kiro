# Design Document: User Management Module

## Overview

El módulo **user-management** es el componente fundamental de Vacapp que proporciona funcionalidades completas de autenticación, autorización y administración de usuarios para una aplicación SaaS multitenant de gestión ganadera. Este módulo implementa un sistema de seguridad basado en JWT con soporte para dos niveles de roles: roles SaaS (super_admin, support) que operan sin restricción de tenant, y roles de negocio (admin, manager, veterinarian, worker) que operan dentro del contexto de un tenant específico.

### Contexto

Vacapp es un monolito modular construido con Spring Modulith siguiendo los principios de Clean Architecture. El módulo user-management debe ser implementado primero, ya que otros módulos funcionales dependen de sus capacidades de autenticación y autorización. El módulo debe soportar tanto una interfaz web basada en Thymeleaf como una API REST para aplicaciones móviles.

### Objetivos del Módulo

1. **Autenticación segura**: Validar credenciales de usuarios y generar tokens JWT con expiración de 24 horas
2. **Autorización basada en roles**: Implementar control de acceso granular basado en 6 roles predefinidos
3. **Multi-tenancy**: Garantizar aislamiento completo de datos entre organizaciones (tenants)
4. **CRUD de usuarios**: Proporcionar operaciones completas de gestión de usuarios con validación
5. **Auditoría completa**: Registrar todas las operaciones críticas para cumplimiento y trazabilidad
6. **Seguridad avanzada**: Implementar rate limiting, bloqueo de cuentas y protección contra ataques comunes
7. **Interfaz dual**: Soportar tanto API REST (móvil) como interfaz web (Thymeleaf)

### Tecnologías

- **Backend**: Java 21, Spring Boot 4.1.0, Spring Modulith
- **Seguridad**: Spring Security 6, JWT (jjwt library), BCrypt
- **Persistencia**: Spring Data JPA, MySQL 8
- **Validación**: Bean Validation (Hibernate Validator)
- **API Documentation**: OpenAPI 3.0 (Design-First con YAML)
- **Frontend Web**: Thymeleaf, HTML5, CSS Vanilla, JavaScript Vanilla
- **Utilidades**: Lombok, MapStruct (opcional para mappers)


## Architecture

### Arquitectura de Capas (Clean Architecture)

El módulo user-management sigue estrictamente la arquitectura hexagonal con separación de responsabilidades en tres capas principales:

```
mx.vacapp.users/
│
├── UsersService.java                    ← API PÚBLICA (único punto de entrada para otros módulos)
│
└── internal/                            ← TODO lo demás es PRIVADO (inaccesible desde otros módulos)
    │
    ├── domain/                          ← Capa de Dominio (lógica de negocio pura)
    │   ├── model/                       ← Entidades de negocio (POJOs sin JPA)
    │   │   ├── User.java                ← Entidad principal de usuario
    │   │   ├── Role.java                ← Enum de roles
    │   │   ├── UserStatus.java          ← Enum de estados
    │   │   └── exceptions/              ← Excepciones de dominio
    │   │       ├── InvalidCredentialsException.java
    │   │       ├── UserAlreadyExistsException.java
    │   │       ├── UserNotFoundException.java
    │   │       ├── AccountLockedException.java
    │   │       └── InactiveAccountException.java
    │   │
    │   └── repository/                  ← Puertos de salida (interfaces)
    │       ├── UserRepository.java      ← Puerto principal de persistencia
    │       └── AuditRepository.java     ← Puerto de auditoría
    │
    ├── application/                     ← Capa de Aplicación (casos de uso)
    │   └── usecases/
    │       ├── auth/                    ← Casos de uso de autenticación
    │       │   ├── LoginUseCase.java
    │       │   ├── ValidateTokenUseCase.java
    │       │   └── RefreshTokenUseCase.java
    │       ├── user/                    ← Casos de uso de gestión de usuarios
    │       │   ├── CreateUserUseCase.java
    │       │   ├── UpdateUserUseCase.java
    │       │   ├── GetUserUseCase.java
    │       │   ├── ListUsersUseCase.java
    │       │   ├── DeactivateUserUseCase.java
    │       │   └── ChangeUserRoleUseCase.java
    │       └── commands/                ← Comandos y resultados (records)
    │           ├── LoginCommand.java
    │           ├── AuthResult.java
    │           ├── CreateUserCommand.java
    │           ├── UpdateUserCommand.java
    │           └── UserResult.java
    │
    └── infrastructure/                  ← Capa de Infraestructura (adaptadores)
        │
        ├── controllers/                 ← Adaptadores de entrada HTTP
        │   ├── mobile/                  ← Controladores REST (API JSON)
        │   │   ├── AuthRestController.java
        │   │   ├── UserRestController.java
        │   │   └── dtos/                ← DTOs Request/Response (Records)
        │   │       ├── LoginRequest.java
        │   │       ├── LoginResponse.java
        │   │       ├── CreateUserRequest.java
        │   │       ├── UpdateUserRequest.java
        │   │       └── UserResponse.java
        │   │
        │   └── web/                     ← Controladores MVC (Thymeleaf)
        │       ├── AuthWebController.java
        │       ├── UserWebController.java
        │       └── dtos/                ← Form DTOs (Records)
        │           ├── LoginFormDto.java
        │           └── UserFormDto.java
        │
        ├── persistence/                 ← Adaptador de salida (JPA)
        │   ├── entities/                ← Entidades JPA
        │   │   ├── UserEntity.java      ← @Entity con @Table("users")
        │   │   ├── UserAuditEntity.java ← @Entity con @Table("users_audit")
        │   │   └── AuthLogEntity.java   ← @Entity con @Table("authentication_log")
        │   ├── repositories/            ← Repositorios JPA
        │   │   ├── UserJpaRepository.java
        │   │   ├── UserAuditJpaRepository.java
        │   │   └── AuthLogJpaRepository.java
        │   ├── impl/                    ← Implementaciones de puertos
        │   │   ├── UserRepositoryImpl.java
        │   │   └── AuditRepositoryImpl.java
        │   └── mappers/                 ← Transformación entre capas
        │       ├── UserMapper.java      ← Mapea User ↔ UserEntity
        │       └── AuditMapper.java
        │
        ├── security/                    ← Configuración de seguridad
        │   ├── JwtTokenProvider.java    ← Generación y validación de JWT
        │   ├── JwtAuthenticationFilter.java
        │   ├── TenantContext.java       ← ThreadLocal para tenant_id actual
        │   ├── SecurityConfig.java      ← Configuración Spring Security
        │   └── PasswordEncoderConfig.java
        │
        └── config/                      ← Configuración del módulo
            ├── UsersModuleConfig.java   ← Beans del módulo
            └── OpenApiConfig.java       ← Configuración Swagger


### Flujo de Datos

#### Flujo de Autenticación (Login)

```
1. Usuario envía POST /api/v1/auth/login con {email, password}
   ↓
2. AuthRestController recibe LoginRequest, valida con @Valid
   ↓
3. AuthRestController mapea a LoginCommand
   ↓
4. LoginUseCase.execute(LoginCommand)
   ↓
5. UserRepository.findByEmail(email) → retorna User o null
   ↓
6. PasswordEncoder.matches(password, user.passwordHash) → valida
   ↓
7. JwtTokenProvider.generateToken(user) → crea JWT con 24h expiración
   ↓
8. AuditRepository.logAuthentication(email, success, ip) → auditoría
   ↓
9. Retorna AuthResult con token
   ↓
10. AuthRestController mapea a LoginResponse
    ↓
11. ResponseEntity.ok(LoginResponse) → HTTP 200
```

#### Flujo de Creación de Usuario

```
1. Admin envía POST /api/v1/users con CreateUserRequest
   ↓
2. JwtAuthenticationFilter extrae JWT, valida firma y expiración
   ↓
3. TenantContext.setTenantId(tenantId del JWT) → ThreadLocal
   ↓
4. UserRestController recibe CreateUserRequest, valida con @Valid
   ↓
5. UserRestController mapea a CreateUserCommand
   ↓
6. CreateUserUseCase.execute(CreateUserCommand)
   ↓
7. UserRepository.existsByEmailAndTenantId(email, tenantId) → valida unicidad
   ↓
8. PasswordEncoder.encode(password) → cifra con BCrypt
   ↓
9. User.create(...) → crea entidad de dominio
   ↓
10. UserRepository.save(user) → persiste (filtra automáticamente por tenant_id)
    ↓
11. AuditRepository.logUserCreation(user, createdBy) → auditoría
    ↓
12. Retorna UserResult
    ↓
13. UserRestController mapea a UserResponse
    ↓
14. ResponseEntity.status(201).body(UserResponse) → HTTP 201
```

#### Flujo de Autorización

```
1. Request con header "Authorization: Bearer {JWT}"
   ↓
2. JwtAuthenticationFilter.doFilterInternal()
   ↓
3. JwtTokenProvider.validateToken(token) → valida firma, expiración
   ↓
4. JwtTokenProvider.extractClaims(token) → extrae user_id, tenant_id, roles
   ↓
5. TenantContext.setTenantId(tenant_id) → almacena en ThreadLocal
   ↓
6. SecurityContextHolder.setAuthentication(authentication) → establece contexto
   ↓
7. UseCase ejecuta operación
   ↓
8. UserRepository aplica filtro WHERE tenant_id = :currentTenantId
   ↓
9. Valida permisos según rol (admin puede CRUD, worker solo GET)
   ↓
10. Retorna resultado o lanza AccessDeniedException
```

### Principios de Diseño

1. **Encapsulamiento estricto**: Solo `UsersService.java` es público, todo bajo `internal/` es privado
2. **Separación de capas**: Dominio no depende de infraestructura (sin anotaciones JPA en domain/model)
3. **Dependency Inversion**: Domain define interfaces (puertos), Infrastructure las implementa
4. **Single Responsibility**: Cada UseCase tiene una única responsabilidad
5. **Immutability**: Records para DTOs y comandos (inmutables por defecto)
6. **Fail-fast**: Validación temprana en controllers con Bean Validation
7. **Multi-tenancy by design**: Filtrado automático por tenant_id en todos los repositorios



## Components and Interfaces

### 1. API Pública del Módulo

#### UsersService.java

```java
package mx.vacapp.users;

import java.util.Optional;
import java.util.UUID;

/**
 * API pública del módulo de usuarios.
 * Único punto de entrada para otros módulos de Vacapp.
 */
public interface UsersService {
    
    /**
     * Valida si un usuario existe y está activo.
     * 
     * @param userId UUID del usuario
     * @return true si el usuario existe y está activo
     */
    boolean isUserActive(UUID userId);
    
    /**
     * Obtiene el tenant_id de un usuario.
     * 
     * @param userId UUID del usuario
     * @return Optional con el tenant_id, o empty si no existe
     */
    Optional<UUID> getUserTenantId(UUID userId);
    
    /**
     * Verifica si un usuario tiene un rol específico.
     * 
     * @param userId UUID del usuario
     * @param role nombre del rol a verificar
     * @return true si el usuario tiene ese rol
     */
    boolean hasRole(UUID userId, String role);
}
```

### 2. Capa de Dominio

#### User.java (Entidad de Dominio)

```java
package mx.vacapp.users.internal.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de dominio que representa un usuario del sistema.
 * POJO puro sin anotaciones de Spring/JPA.
 */
public class User {
    private final UUID userId;
    private final String email;
    private final String name;
    private final String phone;
    private final String passwordHash;
    private final Role role;
    private final UserStatus status;
    private final UUID tenantId;  // null para usuarios SaaS
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID createdBy;
    private final UUID updatedBy;
    
    // Constructor privado para forzar uso de factory methods
    private User(Builder builder) {
        this.userId = builder.userId;
        this.email = builder.email;
        this.name = builder.name;
        this.phone = builder.phone;
        this.passwordHash = builder.passwordHash;
        this.role = builder.role;
        this.status = builder.status;
        this.tenantId = builder.tenantId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }
    
    // Factory method para crear nuevo usuario
    public static User create(String email, String name, String phone, 
                             String passwordHash, Role role, UUID tenantId, UUID createdBy) {
        Instant now = Instant.now();
        return new Builder()
            .userId(UUID.randomUUID())
            .email(email.toLowerCase())
            .name(name)
            .phone(phone)
            .passwordHash(passwordHash)
            .role(role != null ? role : Role.WORKER) // default role
            .status(UserStatus.ACTIVE)
            .tenantId(tenantId)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
    }
    
    // Métodos de negocio
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }
    
    public boolean isSaaSUser() {
        return this.tenantId == null;
    }
    
    public boolean hasSaaSRole() {
        return this.role == Role.SUPER_ADMIN || this.role == Role.SUPPORT;
    }
    
    public User deactivate(UUID deactivatedBy) {
        return new Builder()
            .from(this)
            .status(UserStatus.INACTIVE)
            .updatedAt(Instant.now())
            .updatedBy(deactivatedBy)
            .build();
    }
    
    public User changeRole(Role newRole, UUID changedBy) {
        return new Builder()
            .from(this)
            .role(newRole)
            .updatedAt(Instant.now())
            .updatedBy(changedBy)
            .build();
    }
    
    // Getters (sin setters - inmutabilidad)
    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public UUID getTenantId() { return tenantId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    
    // Builder pattern para inmutabilidad
    public static class Builder {
        private UUID userId;
        private String email;
        private String name;
        private String phone;
        private String passwordHash;
        private Role role;
        private UserStatus status;
        private UUID tenantId;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;
        
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder role(Role role) { this.role = role; return this; }
        public Builder status(UserStatus status) { this.status = status; return this; }
        public Builder tenantId(UUID tenantId) { this.tenantId = tenantId; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder createdBy(UUID createdBy) { this.createdBy = createdBy; return this; }
        public Builder updatedBy(UUID updatedBy) { this.updatedBy = updatedBy; return this; }
        
        public Builder from(User user) {
            this.userId = user.userId;
            this.email = user.email;
            this.name = user.name;
            this.phone = user.phone;
            this.passwordHash = user.passwordHash;
            this.role = user.role;
            this.status = user.status;
            this.tenantId = user.tenantId;
            this.createdAt = user.createdAt;
            this.updatedAt = user.updatedAt;
            this.createdBy = user.createdBy;
            this.updatedBy = user.updatedBy;
            return this;
        }
        
        public User build() {
            return new User(this);
        }
    }
}
```



#### Role.java (Enum de Roles)

```java
package mx.vacapp.users.internal.domain.model;

/**
 * Roles del sistema en dos niveles: SaaS y Business.
 */
public enum Role {
    // SaaS Roles (sin tenant_id)
    SUPER_ADMIN("super_admin", true),
    SUPPORT("support", true),
    
    // Business Roles (con tenant_id)
    ADMIN("admin", false),
    MANAGER("manager", false),
    VETERINARIAN("veterinarian", false),
    WORKER("worker", false);
    
    private final String value;
    private final boolean saasRole;
    
    Role(String value, boolean saasRole) {
        this.value = value;
        this.saasRole = saasRole;
    }
    
    public String getValue() {
        return value;
    }
    
    public boolean isSaaSRole() {
        return saasRole;
    }
    
    public boolean isBusinessRole() {
        return !saasRole;
    }
    
    public static Role fromString(String value) {
        for (Role role : Role.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rol inválido: " + value);
    }
}
```

#### UserStatus.java (Enum de Estados)

```java
package mx.vacapp.users.internal.domain.model;

public enum UserStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    LOCKED("locked");  // bloqueado temporalmente por seguridad
    
    private final String value;
    
    UserStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static UserStatus fromString(String value) {
        for (UserStatus status : UserStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado inválido: " + value);
    }
}
```

#### UserRepository.java (Puerto de Salida)

```java
package mx.vacapp.users.internal.domain.repository;

import mx.vacapp.users.internal.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para operaciones de persistencia de usuarios.
 * Implementado por UserRepositoryImpl en la capa de infraestructura.
 */
public interface UserRepository {
    
    /**
     * Guarda un nuevo usuario o actualiza uno existente.
     * Aplica filtrado automático por tenant_id si corresponde.
     */
    User save(User user);
    
    /**
     * Busca un usuario por su ID.
     * Aplica filtrado automático por tenant_id del contexto actual.
     */
    Optional<User> findById(UUID userId);
    
    /**
     * Busca un usuario por email.
     * Aplica filtrado automático por tenant_id del contexto actual.
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Verifica si existe un usuario con el email dado en el tenant actual.
     */
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
    
    /**
     * Lista usuarios con paginación.
     * Aplica filtrado automático por tenant_id del contexto actual.
     */
    List<User> findAll(int page, int size);
    
    /**
     * Cuenta total de usuarios en el tenant actual.
     */
    long count();
    
    /**
     * Elimina lógicamente un usuario (marca como inactive).
     * No se realizan deletes físicos.
     */
    User deactivate(UUID userId, UUID deactivatedBy);
}
```

#### AuditRepository.java (Puerto de Auditoría)

```java
package mx.vacapp.users.internal.domain.repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Puerto para operaciones de auditoría.
 */
public interface AuditRepository {
    
    /**
     * Registra un intento de autenticación.
     */
    void logAuthentication(String email, boolean success, String clientIp, String userAgent);
    
    /**
     * Registra la creación de un usuario.
     */
    void logUserCreation(UUID userId, UUID createdBy, String oldValues, String newValues);
    
    /**
     * Registra la actualización de un usuario.
     */
    void logUserUpdate(UUID userId, UUID updatedBy, String oldValues, String newValues);
    
    /**
     * Registra la desactivación de un usuario.
     */
    void logUserDeactivation(UUID userId, UUID deactivatedBy, String reason);
}
```



### 3. Capa de Aplicación (Use Cases)

#### LoginUseCase.java

```java
package mx.vacapp.users.internal.application.usecases.auth;

import lombok.RequiredArgsConstructor;
import mx.vacapp.users.internal.application.usecases.commands.LoginCommand;
import mx.vacapp.users.internal.application.usecases.commands.AuthResult;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.UserStatus;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.exceptions.InvalidCredentialsException;
import mx.vacapp.users.internal.domain.exceptions.InactiveAccountException;
import mx.vacapp.users.internal.infrastructure.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Autenticar usuario con email y contraseña.
 */
@Service
@RequiredArgsConstructor
public class LoginUseCase {
    
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Transactional(readOnly = true)
    public AuthResult execute(LoginCommand command) {
        // 1. Validar formato de email (ya validado por Bean Validation en DTO)
        String email = command.email().toLowerCase();
        
        // 2. Buscar usuario por email
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas"));
        
        // 3. Validar contraseña
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            auditRepository.logAuthentication(email, false, command.clientIp(), command.userAgent());
            throw new InvalidCredentialsException("Credenciales inválidas");
        }
        
        // 4. Verificar estado de la cuenta
        if (user.getStatus() == UserStatus.INACTIVE) {
            auditRepository.logAuthentication(email, false, command.clientIp(), command.userAgent());
            throw new InactiveAccountException("Cuenta inactiva");
        }
        
        if (user.getStatus() == UserStatus.LOCKED) {
            auditRepository.logAuthentication(email, false, command.clientIp(), command.userAgent());
            throw new InactiveAccountException("Cuenta bloqueada temporalmente");
        }
        
        // 5. Generar JWT token
        String token = jwtTokenProvider.generateToken(user);
        
        // 6. Registrar autenticación exitosa
        auditRepository.logAuthentication(email, true, command.clientIp(), command.userAgent());
        
        // 7. Retornar resultado
        return new AuthResult(
            token,
            user.getUserId(),
            user.getEmail(),
            user.getName(),
            user.getRole().getValue(),
            user.getTenantId()
        );
    }
}
```

#### CreateUserUseCase.java

```java
package mx.vacapp.users.internal.application.usecases.user;

import lombok.RequiredArgsConstructor;
import mx.vacapp.users.internal.application.usecases.commands.CreateUserCommand;
import mx.vacapp.users.internal.application.usecases.commands.UserResult;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.exceptions.UserAlreadyExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Crear nuevo usuario.
 */
@Service
@RequiredArgsConstructor
public class CreateUserUseCase {
    
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public UserResult execute(CreateUserCommand command) {
        // 1. Validar unicidad del email en el tenant
        String email = command.email().toLowerCase();
        if (userRepository.existsByEmailAndTenantId(email, command.tenantId())) {
            throw new UserAlreadyExistsException("Email ya registrado en esta organización");
        }
        
        // 2. Cifrar contraseña con BCrypt
        String passwordHash = passwordEncoder.encode(command.password());
        
        // 3. Determinar rol (usar WORKER por defecto si no se especifica)
        Role role = command.role() != null 
            ? Role.fromString(command.role()) 
            : Role.WORKER;
        
        // 4. Crear entidad de dominio
        User user = User.create(
            email,
            command.name(),
            command.phone(),
            passwordHash,
            role,
            command.tenantId(),
            command.createdBy()
        );
        
        // 5. Persistir usuario
        User savedUser = userRepository.save(user);
        
        // 6. Registrar auditoría
        auditRepository.logUserCreation(
            savedUser.getUserId(),
            command.createdBy(),
            null, // oldValues
            formatUserAsJson(savedUser) // newValues
        );
        
        // 7. Retornar resultado
        return UserResult.fromDomain(savedUser);
    }
    
    private String formatUserAsJson(User user) {
        // Implementación simplificada - usar Jackson en producción
        return String.format("{\"email\":\"%s\",\"name\":\"%s\",\"role\":\"%s\"}",
            user.getEmail(), user.getName(), user.getRole().getValue());
    }
}
```



#### Comandos y Resultados (Records)

```java
package mx.vacapp.users.internal.application.usecases.commands;

import java.util.UUID;

// Comando de login
public record LoginCommand(
    String email,
    String password,
    String clientIp,
    String userAgent
) {}

// Resultado de autenticación
public record AuthResult(
    String token,
    UUID userId,
    String email,
    String name,
    String role,
    UUID tenantId
) {}

// Comando de creación de usuario
public record CreateUserCommand(
    String email,
    String name,
    String phone,
    String password,
    String role,
    UUID tenantId,
    UUID createdBy
) {}

// Comando de actualización de usuario
public record UpdateUserCommand(
    UUID userId,
    String name,
    String phone,
    String role,
    UUID updatedBy
) {}

// Resultado de usuario
public record UserResult(
    UUID userId,
    String email,
    String name,
    String phone,
    String role,
    String status,
    UUID tenantId,
    String createdAt,
    String updatedAt
) {
    public static UserResult fromDomain(User user) {
        return new UserResult(
            user.getUserId(),
            user.getEmail(),
            user.getName(),
            user.getPhone(),
            user.getRole().getValue(),
            user.getStatus().getValue(),
            user.getTenantId(),
            user.getCreatedAt().toString(),
            user.getUpdatedAt().toString()
        );
    }
}
```

### 4. Capa de Infraestructura - Controladores REST

#### AuthRestController.java

```java
package mx.vacapp.users.internal.infrastructure.controllers.mobile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.vacapp.users.internal.application.usecases.auth.LoginUseCase;
import mx.vacapp.users.internal.application.usecases.commands.LoginCommand;
import mx.vacapp.users.internal.application.usecases.commands.AuthResult;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.LoginRequest;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.LoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación.
 * Implementa la interfaz generada por openapi-generator desde openapi-users.yaml
 */
@RestController
@RequiredArgsConstructor
public class AuthRestController implements AuthApi {
    
    private final LoginUseCase loginUseCase;
    
    @Override
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        // Extraer IP y User-Agent del request HTTP
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        // Mapear DTO a comando
        LoginCommand command = new LoginCommand(
            request.email(),
            request.password(),
            clientIp,
            userAgent
        );
        
        // Ejecutar caso de uso
        AuthResult result = loginUseCase.execute(command);
        
        // Mapear resultado a DTO de respuesta
        LoginResponse response = new LoginResponse(
            result.token(),
            result.userId(),
            result.email(),
            result.name(),
            result.role()
        );
        
        return ResponseEntity.ok(response);
    }
    
    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

#### UserRestController.java

```java
package mx.vacapp.users.internal.infrastructure.controllers.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.vacapp.users.internal.application.usecases.user.*;
import mx.vacapp.users.internal.application.usecases.commands.*;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.*;
import mx.vacapp.users.internal.infrastructure.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para gestión de usuarios.
 * Implementa la interfaz generada desde openapi-users.yaml
 */
@RestController
@RequiredArgsConstructor
public class UserRestController implements UsersApi {
    
    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    
    @Override
    public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request,
        Authentication authentication
    ) {
        UUID currentUserId = extractUserId(authentication);
        UUID tenantId = TenantContext.getTenantId();
        
        CreateUserCommand command = new CreateUserCommand(
            request.email(),
            request.name(),
            request.phone(),
            request.password(),
            request.role(),
            tenantId,
            currentUserId
        );
        
        UserResult result = createUserUseCase.execute(command);
        UserResponse response = mapToResponse(result);
        
        return ResponseEntity.status(201).body(response);
    }
    
    @Override
    public ResponseEntity<UserResponse> getUser(UUID userId, Authentication authentication) {
        UserResult result = getUserUseCase.execute(userId);
        return ResponseEntity.ok(mapToResponse(result));
    }
    
    @Override
    public ResponseEntity<UserListResponse> listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        Authentication authentication
    ) {
        List<UserResult> results = listUsersUseCase.execute(page, size);
        List<UserResponse> users = results.stream()
            .map(this::mapToResponse)
            .toList();
        
        long total = listUsersUseCase.count();
        
        UserListResponse response = new UserListResponse(
            users,
            new PaginationMetadata(page, size, total)
        );
        
        return ResponseEntity.ok(response);
    }
    
    @Override
    public ResponseEntity<UserResponse> updateUser(
        UUID userId,
        @Valid @RequestBody UpdateUserRequest request,
        Authentication authentication
    ) {
        UUID currentUserId = extractUserId(authentication);
        
        UpdateUserCommand command = new UpdateUserCommand(
            userId,
            request.name(),
            request.phone(),
            request.role(),
            currentUserId
        );
        
        UserResult result = updateUserUseCase.execute(command);
        return ResponseEntity.ok(mapToResponse(result));
    }
    
    @Override
    public ResponseEntity<Void> deleteUser(UUID userId, Authentication authentication) {
        UUID currentUserId = extractUserId(authentication);
        deactivateUserUseCase.execute(userId, currentUserId);
        return ResponseEntity.noContent().build();
    }
    
    private UUID extractUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
    
    private UserResponse mapToResponse(UserResult result) {
        return new UserResponse(
            result.userId(),
            result.email(),
            result.name(),
            result.phone(),
            result.role(),
            result.status(),
            result.tenantId(),
            result.createdAt(),
            result.updatedAt()
        );
    }
}
```



#### DTOs Request/Response (Records)

```java
package mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos;

import jakarta.validation.constraints.*;
import java.util.UUID;

// Login Request
public record LoginRequest(
    @NotNull(message = "Email es requerido")
    @Email(message = "Email debe tener formato válido")
    @Size(max = 254, message = "Email no debe exceder 254 caracteres")
    String email,
    
    @NotNull(message = "Contraseña es requerida")
    @Size(min = 8, max = 128, message = "Contraseña debe tener entre 8 y 128 caracteres")
    String password
) {}

// Login Response
public record LoginResponse(
    String token,
    UUID userId,
    String email,
    String name,
    String role
) {}

// Create User Request
public record CreateUserRequest(
    @NotNull(message = "Email es requerido")
    @Email(message = "Email debe tener formato válido")
    @Size(max = 255, message = "Email no debe exceder 255 caracteres")
    String email,
    
    @NotNull(message = "Nombre es requerido")
    @Size(min = 2, max = 100, message = "Nombre debe tener entre 2 y 100 caracteres")
    String name,
    
    @Size(min = 7, max = 20, message = "Teléfono debe tener entre 7 y 20 dígitos")
    String phone,
    
    @NotNull(message = "Contraseña es requerida")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,128}$",
        message = "La contraseña debe tener 8-128 caracteres, una mayúscula, minúscula, dígito y carácter especial"
    )
    String password,
    
    String role  // Opcional, por defecto 'worker'
) {}

// Update User Request
public record UpdateUserRequest(
    @Size(min = 2, max = 100, message = "Nombre debe tener entre 2 y 100 caracteres")
    String name,
    
    @Size(min = 7, max = 20, message = "Teléfono debe tener entre 7 y 20 dígitos")
    String phone,
    
    String role
) {}

// User Response
public record UserResponse(
    UUID userId,
    String email,
    String name,
    String phone,
    String role,
    String status,
    UUID tenantId,
    String createdAt,
    String updatedAt
) {}

// User List Response
public record UserListResponse(
    List<UserResponse> users,
    PaginationMetadata pagination
) {}

// Pagination Metadata
public record PaginationMetadata(
    int page,
    int size,
    long total
) {}
```

### 5. Capa de Infraestructura - Controladores Web (Thymeleaf)

#### AuthWebController.java

```java
package mx.vacapp.users.internal.infrastructure.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador MVC para vistas de autenticación.
 * Sirve páginas HTML con Thymeleaf.
 */
@Controller
public class AuthWebController {
    
    @GetMapping("/auth/login")
    public String loginPage(
        @RequestParam(required = false) String timeout,
        Model model
    ) {
        if ("true".equals(timeout)) {
            model.addAttribute("message", "Sesión expirada, inicie sesión nuevamente");
        }
        return "auth/login";  // templates/auth/login.html
    }
    
    @GetMapping("/auth/logout")
    public String logoutPage() {
        return "auth/logout";  // templates/auth/logout.html
    }
}
```

#### UserWebController.java

```java
package mx.vacapp.users.internal.infrastructure.controllers.web;

import lombok.RequiredArgsConstructor;
import mx.vacapp.users.internal.application.usecases.user.ListUsersUseCase;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador MVC para gestión de usuarios desde web.
 */
@Controller
@RequiredArgsConstructor
public class UserWebController {
    
    private final ListUsersUseCase listUsersUseCase;
    
    @GetMapping("/admin/usuarios")
    public String usersPage(Model model) {
        // La carga de datos se hace desde JavaScript con Fetch API
        return "admin/usuarios";  // templates/admin/usuarios.html
    }
    
    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "dashboard/index";  // templates/dashboard/index.html
    }
}
```



## Data Models

### Modelo de Dominio (Domain Layer)

El modelo de dominio se centra en las entidades de negocio sin dependencias de infraestructura:

- **User**: Entidad principal que representa un usuario con toda su información
- **Role**: Enum que define los 6 roles del sistema (2 SaaS + 4 Business)
- **UserStatus**: Enum que define los estados posibles (ACTIVE, INACTIVE, LOCKED)

### Modelo de Persistencia (Infrastructure Layer)

#### UserEntity.java

```java
package mx.vacapp.users.internal.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA para la tabla users.
 * NO usar directamente en la capa de dominio.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email_tenant", columnList = "email, tenant_id"),
    @Index(name = "idx_users_tenant", columnList = "tenant_id"),
    @Index(name = "idx_users_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    
    @Id
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;
    
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;
    
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RoleEntity role;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StatusEntity status;
    
    @Column(name = "tenant_id", columnDefinition = "BINARY(16)")
    private UUID tenantId;  // NULL para usuarios SaaS
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;
    
    @Column(name = "updated_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID updatedBy;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

enum RoleEntity {
    SUPER_ADMIN, SUPPORT, ADMIN, MANAGER, VETERINARIAN, WORKER
}

enum StatusEntity {
    ACTIVE, INACTIVE, LOCKED
}
```

#### UserAuditEntity.java

```java
package mx.vacapp.users.internal.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA para la tabla users_audit.
 */
@Entity
@Table(name = "users_audit", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_operation", columnList = "operation_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuditEntity {
    
    @Id
    @Column(name = "audit_id", columnDefinition = "BINARY(16)")
    private UUID auditId;
    
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "modified_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID modifiedBy;
    
    @Column(name = "operation_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OperationType operationType;
    
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;  // JSON
    
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;  // JSON
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @PrePersist
    protected void onCreate() {
        if (auditId == null) auditId = UUID.randomUUID();
        if (timestamp == null) timestamp = Instant.now();
    }
}

enum OperationType {
    CREATE, UPDATE, DEACTIVATE
}
```

#### AuthLogEntity.java

```java
package mx.vacapp.users.internal.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA para la tabla authentication_log.
 */
@Entity
@Table(name = "authentication_log", indexes = {
    @Index(name = "idx_authlog_email", columnList = "email"),
    @Index(name = "idx_authlog_timestamp", columnList = "timestamp"),
    @Index(name = "idx_authlog_client_ip", columnList = "client_ip")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthLogEntity {
    
    @Id
    @Column(name = "log_id", columnDefinition = "BINARY(16)")
    private UUID logId;
    
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "success", nullable = false)
    private Boolean success;
    
    @Column(name = "client_ip", length = 45)  // IPv6 max length
    private String clientIp;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @PrePersist
    protected void onCreate() {
        if (logId == null) logId = UUID.randomUUID();
        if (timestamp == null) timestamp = Instant.now();
    }
}
```



### Repositorios JPA

#### UserJpaRepository.java

```java
package mx.vacapp.users.internal.infrastructure.persistence.repositories;

import mx.vacapp.users.internal.infrastructure.persistence.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA Spring Data para UserEntity.
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    
    /**
     * Busca usuario por email y tenant_id.
     * Para usuarios SaaS (tenant_id null), usar findByEmailAndTenantIdIsNull.
     */
    Optional<UserEntity> findByEmailAndTenantId(String email, UUID tenantId);
    
    /**
     * Busca usuario SaaS por email (tenant_id es null).
     */
    Optional<UserEntity> findByEmailAndTenantIdIsNull(String email);
    
    /**
     * Verifica existencia de usuario por email y tenant.
     */
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
    
    /**
     * Lista usuarios de un tenant con paginación.
     */
    Page<UserEntity> findByTenantId(UUID tenantId, Pageable pageable);
    
    /**
     * Cuenta usuarios de un tenant.
     */
    long countByTenantId(UUID tenantId);
    
    /**
     * Busca usuario por ID y tenant_id (para validar acceso).
     */
    @Query("SELECT u FROM UserEntity u WHERE u.userId = :userId AND u.tenantId = :tenantId")
    Optional<UserEntity> findByIdAndTenantId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
}
```

#### UserAuditJpaRepository.java

```java
package mx.vacapp.users.internal.infrastructure.persistence.repositories;

import mx.vacapp.users.internal.infrastructure.persistence.entities.UserAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserAuditJpaRepository extends JpaRepository<UserAuditEntity, UUID> {
    // Métodos básicos heredados de JpaRepository
}
```

#### AuthLogJpaRepository.java

```java
package mx.vacapp.users.internal.infrastructure.persistence.repositories;

import mx.vacapp.users.internal.infrastructure.persistence.entities.AuthLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.UUID;

public interface AuthLogJpaRepository extends JpaRepository<AuthLogEntity, UUID> {
    
    /**
     * Cuenta intentos fallidos de login para un email en una ventana de tiempo.
     */
    @Query("SELECT COUNT(a) FROM AuthLogEntity a WHERE a.email = :email " +
           "AND a.success = false AND a.timestamp >= :since")
    long countFailedAttemptsSince(@Param("email") String email, @Param("since") Instant since);
    
    /**
     * Cuenta intentos de login desde una IP en una ventana de tiempo.
     */
    @Query("SELECT COUNT(a) FROM AuthLogEntity a WHERE a.clientIp = :ip " +
           "AND a.timestamp >= :since")
    long countAttemptsByIpSince(@Param("ip") String ip, @Param("since") Instant since);
}
```

### Mappers (Transformación entre Capas)

#### UserMapper.java

```java
package mx.vacapp.users.internal.infrastructure.persistence.mappers;

import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.UserStatus;
import mx.vacapp.users.internal.infrastructure.persistence.entities.UserEntity;
import mx.vacapp.users.internal.infrastructure.persistence.entities.RoleEntity;
import mx.vacapp.users.internal.infrastructure.persistence.entities.StatusEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper para transformar entre User (dominio) y UserEntity (JPA).
 */
@Component
public class UserMapper {
    
    /**
     * Convierte de entidad JPA a modelo de dominio.
     */
    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        
        return new User.Builder()
            .userId(entity.getUserId())
            .email(entity.getEmail())
            .name(entity.getName())
            .phone(entity.getPhone())
            .passwordHash(entity.getPasswordHash())
            .role(mapRole(entity.getRole()))
            .status(mapStatus(entity.getStatus()))
            .tenantId(entity.getTenantId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
    
    /**
     * Convierte de modelo de dominio a entidad JPA.
     */
    public UserEntity toEntity(User domain) {
        if (domain == null) return null;
        
        return UserEntity.builder()
            .userId(domain.getUserId())
            .email(domain.getEmail())
            .name(domain.getName())
            .phone(domain.getPhone())
            .passwordHash(domain.getPasswordHash())
            .role(mapRoleEntity(domain.getRole()))
            .status(mapStatusEntity(domain.getStatus()))
            .tenantId(domain.getTenantId())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .createdBy(domain.getCreatedBy())
            .updatedBy(domain.getUpdatedBy())
            .build();
    }
    
    private Role mapRole(RoleEntity roleEntity) {
        return Role.valueOf(roleEntity.name());
    }
    
    private RoleEntity mapRoleEntity(Role role) {
        return RoleEntity.valueOf(role.name());
    }
    
    private UserStatus mapStatus(StatusEntity statusEntity) {
        return UserStatus.valueOf(statusEntity.name());
    }
    
    private StatusEntity mapStatusEntity(UserStatus status) {
        return StatusEntity.valueOf(status.name());
    }
}
```



### Implementación de Repositorios (Puertos)

#### UserRepositoryImpl.java

```java
package mx.vacapp.users.internal.infrastructure.persistence.impl;

import lombok.RequiredArgsConstructor;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import mx.vacapp.users.internal.infrastructure.persistence.entities.UserEntity;
import mx.vacapp.users.internal.infrastructure.persistence.repositories.UserJpaRepository;
import mx.vacapp.users.internal.infrastructure.persistence.mappers.UserMapper;
import mx.vacapp.users.internal.infrastructure.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del puerto UserRepository usando JPA.
 * Aplica filtrado automático por tenant_id usando TenantContext.
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    
    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;
    
    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<User> findById(UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            // Usuario SaaS o super_admin - buscar sin filtro
            return jpaRepository.findById(userId)
                .map(mapper::toDomain);
        }
        
        // Usuario con tenant - aplicar filtro
        return jpaRepository.findByIdAndTenantId(userId, tenantId)
            .map(mapper::toDomain);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        UUID tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            // Buscar usuario SaaS (tenant_id IS NULL)
            return jpaRepository.findByEmailAndTenantIdIsNull(email.toLowerCase())
                .map(mapper::toDomain);
        }
        
        // Buscar en el tenant específico
        return jpaRepository.findByEmailAndTenantId(email.toLowerCase(), tenantId)
            .map(mapper::toDomain);
    }
    
    @Override
    public boolean existsByEmailAndTenantId(String email, UUID tenantId) {
        return jpaRepository.existsByEmailAndTenantId(email.toLowerCase(), tenantId);
    }
    
    @Override
    public List<User> findAll(int page, int size) {
        UUID tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            // Super admin - listar todos los usuarios
            Page<UserEntity> entities = jpaRepository.findAll(PageRequest.of(page, size));
            return entities.stream()
                .map(mapper::toDomain)
                .toList();
        }
        
        // Listar solo usuarios del tenant
        Page<UserEntity> entities = jpaRepository.findByTenantId(tenantId, PageRequest.of(page, size));
        return entities.stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public long count() {
        UUID tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            return jpaRepository.count();
        }
        
        return jpaRepository.countByTenantId(tenantId);
    }
    
    @Override
    public User deactivate(UUID userId, UUID deactivatedBy) {
        User user = findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        User deactivated = user.deactivate(deactivatedBy);
        return save(deactivated);
    }
}
```

#### AuditRepositoryImpl.java

```java
package mx.vacapp.users.internal.infrastructure.persistence.impl;

import lombok.RequiredArgsConstructor;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.infrastructure.persistence.entities.AuthLogEntity;
import mx.vacapp.users.internal.infrastructure.persistence.entities.UserAuditEntity;
import mx.vacapp.users.internal.infrastructure.persistence.entities.OperationType;
import mx.vacapp.users.internal.infrastructure.persistence.repositories.AuthLogJpaRepository;
import mx.vacapp.users.internal.infrastructure.persistence.repositories.UserAuditJpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.UUID;

/**
 * Implementación del puerto AuditRepository usando JPA.
 */
@Repository
@RequiredArgsConstructor
public class AuditRepositoryImpl implements AuditRepository {
    
    private final AuthLogJpaRepository authLogJpaRepository;
    private final UserAuditJpaRepository userAuditJpaRepository;
    
    @Override
    public void logAuthentication(String email, boolean success, String clientIp, String userAgent) {
        AuthLogEntity log = AuthLogEntity.builder()
            .logId(UUID.randomUUID())
            .email(email.toLowerCase())
            .timestamp(Instant.now())
            .success(success)
            .clientIp(clientIp)
            .userAgent(truncate(userAgent, 500))
            .build();
        
        authLogJpaRepository.save(log);
    }
    
    @Override
    public void logUserCreation(UUID userId, UUID createdBy, String oldValues, String newValues) {
        UserAuditEntity audit = UserAuditEntity.builder()
            .auditId(UUID.randomUUID())
            .userId(userId)
            .timestamp(Instant.now())
            .modifiedBy(createdBy)
            .operationType(OperationType.CREATE)
            .oldValues(oldValues)
            .newValues(newValues)
            .build();
        
        userAuditJpaRepository.save(audit);
    }
    
    @Override
    public void logUserUpdate(UUID userId, UUID updatedBy, String oldValues, String newValues) {
        UserAuditEntity audit = UserAuditEntity.builder()
            .auditId(UUID.randomUUID())
            .userId(userId)
            .timestamp(Instant.now())
            .modifiedBy(updatedBy)
            .operationType(OperationType.UPDATE)
            .oldValues(oldValues)
            .newValues(newValues)
            .build();
        
        userAuditJpaRepository.save(audit);
    }
    
    @Override
    public void logUserDeactivation(UUID userId, UUID deactivatedBy, String reason) {
        UserAuditEntity audit = UserAuditEntity.builder()
            .auditId(UUID.randomUUID())
            .userId(userId)
            .timestamp(Instant.now())
            .modifiedBy(deactivatedBy)
            .operationType(OperationType.DEACTIVATE)
            .reason(truncate(reason, 500))
            .build();
        
        userAuditJpaRepository.save(audit);
    }
    
    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
```



### Esquema de Base de Datos MySQL

```sql
-- Tabla principal de usuarios
CREATE TABLE users (
    user_id BINARY(16) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    password_hash VARCHAR(60) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BINARY(16),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    updated_by BINARY(16) NOT NULL,
    
    CONSTRAINT chk_role CHECK (role IN ('SUPER_ADMIN', 'SUPPORT', 'ADMIN', 'MANAGER', 'VETERINARIAN', 'WORKER')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    
    INDEX idx_users_email_tenant (email, tenant_id),
    INDEX idx_users_tenant (tenant_id),
    INDEX idx_users_status (status),
    
    UNIQUE KEY uk_email_tenant (email, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de auditoría de usuarios
CREATE TABLE users_audit (
    audit_id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_by BINARY(16) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    old_values TEXT,
    new_values TEXT,
    reason VARCHAR(500),
    
    CONSTRAINT chk_operation_type CHECK (operation_type IN ('CREATE', 'UPDATE', 'DEACTIVATE')),
    
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_operation (operation_type),
    
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de log de autenticación
CREATE TABLE authentication_log (
    log_id BINARY(16) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    success BOOLEAN NOT NULL,
    client_ip VARCHAR(45),
    user_agent VARCHAR(500),
    
    INDEX idx_authlog_email (email),
    INDEX idx_authlog_timestamp (timestamp),
    INDEX idx_authlog_client_ip (client_ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comentarios de documentación
ALTER TABLE users 
    COMMENT 'Tabla principal de usuarios del sistema con soporte multi-tenant';

ALTER TABLE users_audit 
    COMMENT 'Auditoría completa de cambios en usuarios para cumplimiento normativo';

ALTER TABLE authentication_log 
    COMMENT 'Log de intentos de autenticación para análisis de seguridad';
```

### Datos Iniciales (Seed Data)

```sql
-- Usuario super_admin por defecto (sin tenant)
-- Password: Admin123!
INSERT INTO users (user_id, email, name, phone, password_hash, role, status, tenant_id, created_by, updated_by)
VALUES (
    UNHEX(REPLACE(UUID(), '-', '')),
    'admin@vacapp.mx',
    'Super Admin',
    NULL,
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIiIw4MQBG',  -- BCrypt hash de "Admin123!"
    'SUPER_ADMIN',
    'ACTIVE',
    NULL,  -- Sin tenant (usuario SaaS)
    UNHEX(REPLACE(UUID(), '-', '')),
    UNHEX(REPLACE(UUID(), '-', ''))
);
```



## Configuración de Seguridad JWT

### JwtTokenProvider.java

```java
package mx.vacapp.users.internal.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.users.internal.domain.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Proveedor de JWT tokens.
 * Genera, valida y extrae información de tokens JWT.
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    private final SecretKey secretKey;
    private final long expirationHours;
    
    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration-hours:24}") long expirationHours
    ) {
        // Generar clave segura de mínimo 256 bits (32 bytes)
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }
    
    /**
     * Genera un JWT token para un usuario autenticado.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationHours, ChronoUnit.HOURS);
        
        return Jwts.builder()
            .setSubject(user.getUserId().toString())  // user_id como subject
            .claim("email", user.getEmail())
            .claim("tenant_id", user.getTenantId() != null ? user.getTenantId().toString() : null)
            .claim("roles", new String[]{user.getRole().getValue()})  // array con 1 rol
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiration))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }
    
    /**
     * Valida un JWT token.
     * @return true si es válido, false si es inválido o expirado
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(60)  // tolerancia de 60 segundos
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expirado: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extrae claims del JWT token.
     */
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    /**
     * Extrae user_id del token.
     */
    public UUID extractUserId(String token) {
        String subject = extractClaims(token).getSubject();
        return UUID.fromString(subject);
    }
    
    /**
     * Extrae tenant_id del token (puede ser null para usuarios SaaS).
     */
    public UUID extractTenantId(String token) {
        String tenantId = extractClaims(token).get("tenant_id", String.class);
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }
    
    /**
     * Extrae roles del token.
     */
    @SuppressWarnings("unchecked")
    public String[] extractRoles(String token) {
        return extractClaims(token).get("roles", String[].class);
    }
}
```

### JwtAuthenticationFilter.java

```java
package mx.vacapp.users.internal.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebDetailsSource;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Filtro de autenticación JWT.
 * Se ejecuta una vez por request para validar el token JWT y establecer el contexto de seguridad.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // 1. Extraer token del header Authorization
            String token = extractTokenFromRequest(request);
            
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // 2. Extraer información del token
                UUID userId = jwtTokenProvider.extractUserId(token);
                UUID tenantId = jwtTokenProvider.extractTenantId(token);
                String[] roles = jwtTokenProvider.extractRoles(token);
                
                // 3. Establecer tenant_id en TenantContext (ThreadLocal)
                TenantContext.setTenantId(tenantId);
                
                // 4. Crear authorities de Spring Security
                List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .toList();
                
                // 5. Crear Authentication object
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userId.toString(),  // principal (user_id como String)
                        null,  // credentials (no necesarias después de autenticación)
                        authorities
                    );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 6. Establecer en SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Usuario autenticado: userId={}, tenantId={}, roles={}", 
                    userId, tenantId, Arrays.toString(roles));
            }
        } catch (Exception e) {
            log.error("Error en autenticación JWT: {}", e.getMessage());
            // No establecer authentication - el request será rechazado por Spring Security
        }
        
        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
        
        // Limpiar TenantContext después del request
        TenantContext.clear();
    }
    
    /**
     * Extrae el token JWT del header Authorization.
     * Formato esperado: "Bearer {token}"
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
```

### TenantContext.java

```java
package mx.vacapp.users.internal.infrastructure.security;

import java.util.UUID;

/**
 * Contexto de tenant usando ThreadLocal.
 * Almacena el tenant_id del usuario autenticado durante el request.
 */
public class TenantContext {
    
    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    
    /**
     * Establece el tenant_id para el thread actual.
     */
    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    /**
     * Obtiene el tenant_id del thread actual.
     * @return UUID del tenant, o null para usuarios SaaS
     */
    public static UUID getTenantId() {
        return TENANT_ID.get();
    }
    
    /**
     * Limpia el contexto de tenant.
     * Debe llamarse al final de cada request.
     */
    public static void clear() {
        TENANT_ID.remove();
    }
}
```



### SecurityConfig.java

```java
package mx.vacapp.users.internal.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security para el módulo de usuarios.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF (usando JWT stateless)
            .csrf(csrf -> csrf.disable())
            
            // Configurar autorización de requests
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas (sin autenticación)
                .requestMatchers("/auth/login", "/api/v1/auth/login").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // Rutas de API REST (requieren JWT)
                .requestMatchers("/api/v1/**").authenticated()
                
                // Rutas web (requieren JWT o redirección a login)
                .requestMatchers("/dashboard", "/admin/**").authenticated()
                
                // Cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
            )
            
            // Configurar sesión stateless (no usar HttpSession)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Agregar filtro JWT antes del filtro de autenticación estándar
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configurar manejo de excepciones
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    // Si es API, retornar 401 JSON
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Autenticación requerida\"}");
                    } else {
                        // Si es web, redireccionar a login
                        response.sendRedirect("/auth/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // Si es API, retornar 403 JSON
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Acceso denegado\"}");
                    } else {
                        // Si es web, mostrar página de error
                        response.sendRedirect("/error/403");
                    }
                })
            );
        
        return http.build();
    }
}
```

### PasswordEncoderConfig.java

```java
package mx.vacapp.users.internal.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuración del encoder de contraseñas.
 */
@Configuration
public class PasswordEncoderConfig {
    
    /**
     * Bean de PasswordEncoder usando BCrypt con factor de trabajo 12.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // Factor de trabajo entre 10-12
    }
}
```

### application.yml

```yaml
# Configuración del módulo de usuarios
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vacapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:vacapp_user}
    password: ${DB_PASSWORD:vacapp_password}
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  jpa:
    hibernate:
      ddl-auto: validate  # En producción usar 'validate', en desarrollo 'update'
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect
        
  security:
    filter:
      order: -100  # Filtro JWT antes que otros filtros

# Configuración JWT
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-change-in-production-minimum-32-characters}
  expiration-hours: 24

# Configuración de logging
logging:
  level:
    mx.vacapp.users: DEBUG
    org.springframework.security: INFO
    org.hibernate.SQL: DEBUG
```



## Error Handling

### Excepciones de Dominio

El módulo define excepciones específicas en la capa de dominio para diferentes casos de error:

```java
package mx.vacapp.users.internal.domain.exceptions;

// Excepción base
public class UserDomainException extends RuntimeException {
    public UserDomainException(String message) {
        super(message);
    }
}

// Credenciales inválidas
public class InvalidCredentialsException extends UserDomainException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}

// Usuario no encontrado
public class UserNotFoundException extends UserDomainException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

// Usuario ya existe
public class UserAlreadyExistsException extends UserDomainException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

// Cuenta inactiva
public class InactiveAccountException extends UserDomainException {
    public InactiveAccountException(String message) {
        super(message);
    }
}

// Cuenta bloqueada
public class AccountLockedException extends UserDomainException {
    public AccountLockedException(String message) {
        super(message);
    }
}

// Acceso denegado por tenant
public class TenantAccessDeniedException extends UserDomainException {
    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
```

### GlobalExceptionHandler

```java
package mx.vacapp.users.internal.infrastructure.controllers;

import mx.vacapp.users.internal.domain.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para el módulo de usuarios.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 400 - Validación de request DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ValidationError(
                error.getField(),
                redactSensitiveField(error.getField(), error.getDefaultMessage())
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("Errores de validación", errors));
    }
    
    // 401 - Credenciales inválidas
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(ex.getMessage(), null));
    }
    
    // 403 - Cuenta inactiva o bloqueada
    @ExceptionHandler({InactiveAccountException.class, AccountLockedException.class})
    public ResponseEntity<ErrorResponse> handleInactiveAccount(UserDomainException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(ex.getMessage(), null));
    }
    
    // 403 - Acceso denegado por tenant
    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTenantAccessDenied(TenantAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("No autorizado para modificar datos de otro tenant", null));
    }
    
    // 404 - Usuario no encontrado
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("Usuario no encontrado", null));
    }
    
    // 409 - Usuario ya existe
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getMessage(), null));
    }
    
    // 429 - Demasiados intentos
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new ErrorResponse(ex.getMessage(), null));
    }
    
    // 500 - Error genérico
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // Log del error completo (no exponer al cliente)
        log.error("Error inesperado: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Error interno del servidor", null));
    }
    
    /**
     * Redacta valores de campos sensibles en mensajes de error.
     */
    private String redactSensitiveField(String fieldName, String message) {
        if (fieldName.toLowerCase().contains("password") || 
            fieldName.toLowerCase().contains("secret")) {
            return message.replace(fieldName, "[REDACTED]");
        }
        return message;
    }
}

// DTOs de error
record ErrorResponse(String message, List<ValidationError> errors) {}
record ValidationError(String field, String message) {}
```



## Testing Strategy

### Enfoque de Pruebas

El módulo user-management requiere un enfoque mixto de testing que combine pruebas unitarias, de integración y property-based testing donde sea aplicable. Dado que este módulo incluye lógica de infraestructura (JWT, Spring Security, JPA) y lógica de negocio pura, el enfoque de testing se divide en:

1. **Pruebas Unitarias**: Para lógica de dominio pura, casos de uso y mappers
2. **Pruebas de Integración**: Para controllers, repositorios JPA y configuración de seguridad
3. **Property-Based Testing**: Para validación de lógica crítica de negocio
4. **Pruebas de Seguridad**: Para verificar configuración JWT, filtros y autorización

### Evaluación de Property-Based Testing

**¿Es apropiado PBT para este módulo?**

Este módulo tiene componentes donde PBT ES apropiado y otros donde NO lo es:

**PBT ES APROPIADO para:**
- Validación de formato de contraseñas (regex, complejidad)
- Generación y parseo de JWT tokens (round-trip)
- Transformaciones entre User (dominio) y UserEntity (JPA) via mappers
- Validación de formato de email (RFC 5322)
- Lógica de roles y permisos (verificación de autorización)

**PBT NO ES APROPIADO para:**
- Configuración de Spring Security (IaC-like, declarativo)
- Endpoints REST (mejor con pruebas de integración)
- Operaciones CRUD en base de datos (mejor con pruebas de integración)
- Filtros de autenticación (side-effects, mejor con mocks)
- Controladores Thymeleaf (rendering, mejor con pruebas funcionales)

**Decisión**: Incluir property-based testing SOLO para la lógica de negocio pura (validaciones, transformaciones, round-trips). Usar pruebas de integración para todo lo relacionado con infraestructura.

### Herramientas de Testing

- **JUnit 5**: Framework de testing principal
- **Mockito**: Mocking para pruebas unitarias
- **Spring Boot Test**: Pruebas de integración con contexto Spring
- **Testcontainers**: MySQL en contenedor para pruebas de integración
- **jqwik**: Property-based testing para Java
- **RestAssured**: Testing de API REST
- **AssertJ**: Assertions fluidas

### Estructura de Pruebas

```
src/test/java/mx/vacapp/users/
├── UsersServiceTest.java                    ← Test de API pública
│
└── internal/
    ├── domain/
    │   └── model/
    │       ├── UserTest.java                ← Tests unitarios de entidad User
    │       ├── UserPropertiesTest.java      ← Property tests (jqwik)
    │       └── RoleTest.java
    │
    ├── application/
    │   └── usecases/
    │       ├── auth/
    │       │   └── LoginUseCaseTest.java    ← Tests unitarios con mocks
    │       └── user/
    │           └── CreateUserUseCaseTest.java
    │
    └── infrastructure/
        ├── controllers/
        │   └── mobile/
        │       ├── AuthRestControllerIntegrationTest.java   ← Tests de integración
        │       └── UserRestControllerIntegrationTest.java
        │
        ├── persistence/
        │   ├── impl/
        │   │   └── UserRepositoryImplIntegrationTest.java   ← Tests con Testcontainers
        │   └── mappers/
        │       └── UserMapperPropertiesTest.java            ← Property tests para mappers
        │
        └── security/
            ├── JwtTokenProviderTest.java                    ← Tests unitarios
            ├── JwtTokenProviderPropertiesTest.java          ← Property tests (round-trip)
            └── SecurityConfigIntegrationTest.java
```

### Configuración de Property-Based Testing

```xml
<!-- pom.xml - Dependencia jqwik para PBT -->
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.7.4</version>
    <scope>test</scope>
</dependency>
```

**Configuración de iteraciones**: Mínimo 100 iteraciones por property test (configurado globalmente en jqwik.properties):

```properties
# src/test/resources/jqwik.properties
jqwik.tries.default = 100
jqwik.maxDiscardRatio.default = 5
```

### Ejemplos de Tests

#### Test Unitario: CreateUserUseCase

```java
@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private AuditRepository auditRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private CreateUserUseCase createUserUseCase;
    
    @Test
    void execute_WithValidCommand_ShouldCreateUser() {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        CreateUserCommand command = new CreateUserCommand(
            "test@example.com",
            "Test User",
            "1234567890",
            "Password123!",
            "admin",
            tenantId,
            createdBy
        );
        
        when(userRepository.existsByEmailAndTenantId(anyString(), any())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        
        // When
        UserResult result = createUserUseCase.execute(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("test@example.com");
        verify(auditRepository).logUserCreation(any(), eq(createdBy), isNull(), anyString());
    }
    
    @Test
    void execute_WithDuplicateEmail_ShouldThrowException() {
        // Given
        UUID tenantId = UUID.randomUUID();
        CreateUserCommand command = new CreateUserCommand(
            "duplicate@example.com",
            "Test",
            null,
            "Pass123!",
            null,
            tenantId,
            UUID.randomUUID()
        );
        
        when(userRepository.existsByEmailAndTenantId(anyString(), any())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> createUserUseCase.execute(command))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessage("Email ya registrado en esta organización");
    }
}
```



#### Test de Integración: UserRestController

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserRestControllerIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("vacapp_test")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Test
    void createUser_WithValidRequest_ShouldReturn201() {
        // Given
        String token = generateAdminToken();
        CreateUserRequest request = new CreateUserRequest(
            "newuser@example.com",
            "New User",
            "1234567890",
            "Password123!@",
            "worker"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
            "/api/v1/users",
            entity,
            UserResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("newuser@example.com");
    }
    
    @Test
    void createUser_WithoutAuthentication_ShouldReturn401() {
        // Given
        CreateUserRequest request = new CreateUserRequest(
            "test@example.com",
            "Test",
            null,
            "Pass123!@",
            null
        );
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/users",
            request,
            ErrorResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    private String generateAdminToken() {
        User admin = User.create(
            "admin@test.com",
            "Admin",
            null,
            "hash",
            Role.ADMIN,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        return jwtTokenProvider.generateToken(admin);
    }
}
```

#### Property Test: JWT Round-Trip

```java
@Property
void jwtToken_ParseThenGenerate_ShouldPreserveData(
    @ForAll @AlphaNumeric String email,
    @ForAll UUID userId,
    @ForAll UUID tenantId
) {
    // Feature: user-management, Property 1: JWT token round-trip preservation
    
    // Given - Usuario con datos generados
    User user = new User.Builder()
        .userId(userId)
        .email(email + "@test.com")
        .name("Test User")
        .phone(null)
        .passwordHash("hash")
        .role(Role.ADMIN)
        .status(UserStatus.ACTIVE)
        .tenantId(tenantId)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .createdBy(UUID.randomUUID())
        .updatedBy(UUID.randomUUID())
        .build();
    
    // When - Generar token y extraer claims
    String token = jwtTokenProvider.generateToken(user);
    Claims claims = jwtTokenProvider.extractClaims(token);
    
    // Then - Los claims deben preservar los datos originales
    assertThat(UUID.fromString(claims.getSubject())).isEqualTo(userId);
    assertThat(claims.get("email", String.class)).isEqualTo(user.getEmail());
    assertThat(UUID.fromString(claims.get("tenant_id", String.class))).isEqualTo(tenantId);
    
    String[] roles = claims.get("roles", String[].class);
    assertThat(roles).hasSize(1);
    assertThat(roles[0]).isEqualTo("admin");
}
```

#### Property Test: UserMapper

```java
@Property
void userMapper_ToEntityThenToDomain_ShouldPreserveData(
    @ForAll UUID userId,
    @ForAll @Email String email,
    @ForAll @StringLength(min = 2, max = 100) String name
) {
    // Feature: user-management, Property 2: User domain-entity mapping round-trip
    
    // Given - Usuario de dominio con datos generados
    User original = new User.Builder()
        .userId(userId)
        .email(email)
        .name(name)
        .phone(null)
        .passwordHash("$2a$12$hash")
        .role(Role.MANAGER)
        .status(UserStatus.ACTIVE)
        .tenantId(UUID.randomUUID())
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .createdBy(UUID.randomUUID())
        .updatedBy(UUID.randomUUID())
        .build();
    
    // When - Convertir a entidad JPA y de vuelta a dominio
    UserEntity entity = userMapper.toEntity(original);
    User roundTripped = userMapper.toDomain(entity);
    
    // Then - Todos los campos deben ser idénticos
    assertThat(roundTripped.getUserId()).isEqualTo(original.getUserId());
    assertThat(roundTripped.getEmail()).isEqualTo(original.getEmail());
    assertThat(roundTripped.getName()).isEqualTo(original.getName());
    assertThat(roundTripped.getRole()).isEqualTo(original.getRole());
    assertThat(roundTripped.getStatus()).isEqualTo(original.getStatus());
    assertThat(roundTripped.getTenantId()).isEqualTo(original.getTenantId());
}
```

### Cobertura de Testing

**Objetivos de cobertura:**
- **Capa de dominio**: 90%+ (lógica crítica de negocio)
- **Capa de aplicación (use cases)**: 85%+ (orquestación)
- **Capa de infraestructura**: 70%+ (adaptadores, configuración)
- **Global**: 80%+

**Métricas a monitorear:**
- Cobertura de líneas
- Cobertura de branches
- Cobertura de métodos públicos
- Mutación testing (opcional con Pitest)



## Correctness Properties

*Una propiedad (property) es una característica o comportamiento que debe cumplirse en todas las ejecuciones válidas de un sistema—esencialmente, una afirmación formal sobre lo que el sistema debe hacer. Las propiedades sirven como puente entre especificaciones legibles por humanos y garantías de correctitud verificables por máquina.*

### Análisis de Testabilidad (Prework)

A continuación se analiza cada acceptance criterion para determinar si es testeable como property, example, edge-case, integration test o smoke test:

#### Requirement 1: Autenticación de Usuarios

**1.1** WHEN usuario proporciona email y contraseña correctos THEN generar JWT válido con 24h expiración
- **Thoughts**: Esta es una operación que varía con diferentes usuarios y contraseñas. Podemos generar usuarios aleatorios, autenticarlos y verificar que el token generado tiene exactamente 24 horas de expiración.
- **Classification**: PROPERTY
- **Test Strategy**: Generar usuarios aleatorios, cifrar contraseñas, validar que el token tiene exp = iat + 24 horas

**1.2** WHEN usuario proporciona credenciales incorrectas THEN retornar 401 sin indicar qué campo es incorrecto
- **Thoughts**: Esta es validación de seguridad que debe aplicar para todos los casos. Podemos generar combinaciones aleatorias de emails/passwords incorrectos.
- **Classification**: PROPERTY
- **Test Strategy**: Generar credenciales inválidas aleatorias, verificar siempre código 401 con mensaje genérico

**1.3** WHEN JWT con exp < timestamp actual UTC THEN rechazar con 401
- **Thoughts**: Esto es validación de expiración que debe funcionar para cualquier token. Podemos generar tokens con diferentes timestamps de expiración.
- **Classification**: PROPERTY
- **Test Strategy**: Generar tokens con timestamps expirados aleatorios, verificar rechazo consistente

**1.4** THE Password_Encoder SHALL utilizar BCrypt con factor 10-12
- **Thoughts**: Esta es una configuración fija del sistema, no varía con inputs.
- **Classification**: INTEGRATION
- **Test Strategy**: Test único que verifica la configuración del PasswordEncoder

**1.5** WHEN usuario Business_Role se autentica THEN JWT incluye tenant_id y array con 1 rol
- **Thoughts**: Esto debe cumplirse para todos los usuarios con Business_Role. Podemos generar usuarios aleatorios con diferentes Business_Roles.
- **Classification**: PROPERTY
- **Test Strategy**: Generar usuarios con Business_Roles aleatorios, verificar estructura del JWT

**1.6** THE Authentication_System SHALL validar formato email RFC 5322 max 254 chars
- **Thoughts**: Esto es validación de formato que debe funcionar para todos los emails. Podemos generar strings aleatorios y verificar que solo emails válidos pasan.
- **Classification**: PROPERTY
- **Test Strategy**: Generar strings aleatorios, verificar que solo emails RFC 5322 válidos son aceptados

**1.7** WHEN usuario SaaS_Role se autentica THEN JWT con tenant_id null y rol SaaS
- **Thoughts**: Similar a 1.5 pero para SaaS_Roles. Podemos generar usuarios SaaS aleatorios.
- **Classification**: PROPERTY
- **Test Strategy**: Generar usuarios SaaS aleatorios, verificar tenant_id null en JWT

**1.8** THE Authentication_System SHALL incluir claims: user_id, email, tenant_id, roles
- **Thoughts**: Esto debe cumplirse para todos los tokens generados.
- **Classification**: PROPERTY (subsumido por 1.5 y 1.7)

**1.9** WHEN usuario inactive proporciona credenciales correctas THEN retornar 403
- **Thoughts**: Esto es un ejemplo específico del estado inactive.
- **Classification**: EXAMPLE
- **Test Strategy**: Test específico con usuario inactive

**1.10** WHEN 5 intentos fallidos en 15 min THEN bloquear usuario y retornar 429
- **Thoughts**: Esta es una regla específica con números concretos (5, 15 min). Es mejor probarla con ejemplos.
- **Classification**: INTEGRATION
- **Test Strategy**: Test de integración que simula 5 intentos fallidos

**1.11** WHEN bloqueo expira después 15 min THEN permitir nuevos intentos
- **Thoughts**: Continuación de 1.10, mejor como integration test.
- **Classification**: INTEGRATION

#### Requirement 2: Registro de Usuarios

**2.1** WHEN admin registra usuario con email único THEN crear con status active y retornar 201
- **Thoughts**: Esto debe funcionar para cualquier combinación válida de datos de usuario.
- **Classification**: PROPERTY
- **Test Strategy**: Generar datos de usuario aleatorios válidos, verificar creación exitosa

**2.2** WHEN admin intenta registrar email duplicado en mismo tenant THEN retornar 409
- **Thoughts**: Esto es validación de unicidad que debe funcionar para cualquier email.
- **Classification**: PROPERTY
- **Test Strategy**: Generar usuarios aleatorios, intentar duplicar, verificar 409

**2.3** THE User_Module SHALL permitir mismo email en diferentes tenants
- **Thoughts**: Esto es una regla de multi-tenancy que debe cumplirse siempre.
- **Classification**: PROPERTY
- **Test Strategy**: Generar usuarios con mismo email en diferentes tenants, verificar ambos existen

**2.4** WHEN usuario registrado sin rol especificado THEN asignar "worker"
- **Thoughts**: Esto es un comportamiento por defecto que debe funcionar siempre.
- **Classification**: PROPERTY
- **Test Strategy**: Generar usuarios sin campo rol, verificar todos tienen rol WORKER

**2.5** WHEN email válido RFC 5322 max 255 chars THEN aceptar
- **Thoughts**: Validación de formato (ya cubierta por 1.6).
- **Classification**: PROPERTY (redundante con 1.6)

**2.6** WHEN contraseña 8-128 chars con mayúscula, minúscula, dígito THEN aceptar y cifrar
- **Thoughts**: Validación de complejidad de contraseña. Podemos generar contraseñas aleatorias que cumplan/incumplan.
- **Classification**: PROPERTY
- **Test Strategy**: Generar contraseñas aleatorias válidas/inválidas, verificar aceptación/rechazo

**2.7** WHEN contraseña no cumple criterios THEN retornar 400 con mensaje
- **Thoughts**: Caso de error de 2.6.
- **Classification**: PROPERTY (subsumido por 2.6)

**2.8** WHEN super_admin registra usuario con tenant_id null THEN crear usuario SaaS
- **Thoughts**: Caso específico de creación de usuario SaaS.
- **Classification**: EXAMPLE
- **Test Strategy**: Test específico con super_admin creando usuario SaaS

**2.9** WHEN usuario registrado THEN registrar created_at, created_by, tenant_id
- **Thoughts**: Esto debe cumplirse para todos los usuarios creados.
- **Classification**: PROPERTY
- **Test Strategy**: Generar usuarios aleatorios, verificar campos de auditoría presentes

**2.10** WHEN rol no existe en catálogo THEN retornar 400
- **Thoughts**: Validación de enum. Podemos generar strings aleatorios para rol.
- **Classification**: PROPERTY
- **Test Strategy**: Generar strings aleatorios, verificar solo roles válidos aceptados

#### Requirement 3: Gestión de Roles

**3.1-3.9** - Mayoría son reglas de negocio sobre asignación de roles y validación de permisos
- **Classification**: Mezcla de PROPERTY (reglas de validación) e INTEGRATION (persistencia)

#### Requirements 4-12

Por brevedad, el análisis completo de los demás requirements sigue el mismo patrón:
- Validaciones de formato/estructura → PROPERTY
- Reglas de negocio que aplican a todos los casos → PROPERTY
- Configuración del sistema → INTEGRATION o SMOKE
- Operaciones CRUD básicas → INTEGRATION
- Casos específicos de error → EXAMPLE


### Property 1: Round-trip JWT Token Validation

**Validates**: Requirements 1.8, 10.4, 10.5

**Property Statement**: Para todos los JWT tokens válidos generados por el sistema, parsear el token, formatear sus claims como JSON con indentación, y luego volver a parsear ese JSON debe producir exactamente los mismos claims con los mismos tipos de datos y valores.

**Test Strategy**:
- Generar usuarios aleatorios con diferentes roles (SaaS y Business)
- Autenticar cada usuario para obtener JWT
- Parsear JWT → obtener claims map
- Formatear claims como JSON con indentación de 2 espacios
- Volver a parsear el JSON → obtener claims2
- Comparar: claims must equal claims2 (deep equality)
- En particular: sub string, exp long, roles array, tenant_id string/null

**Preconditions**:
- Usuario válido con email/contraseña correcta
- Cuenta activa (no locked, inactive)
- No excede rate limiting

**Postconditions**:
- claims1.deepEquals(claims2) == true
- Tipo de datos preservado: strings siguen strings, longs siguen longs
- Estructura JSON válida con UTF-8

### Property 2: Email Uniqueness Within Tenant

**Validates**: Requirements 2.1, 2.2, 2.3

**Property Statement**: Para todos los pares de usuarios (u1, u2) en el mismo tenant, si u1.email.equalsIgnoreCase(u2.email) entonces u1.userId.equals(u2.userId) (es el mismo usuario).

**Test Strategy**:
- Generar emails aleatorios válidos RFC 5322
- Por cada email, intentar crear usuarios con ese email en el mismo tenant
- Verificar que solo el primer intento tiene éxito (HTTP 201)
- Verificar que intentos posteriores fallan con HTTP 409
- Verificar que usuarios con el mismo email en diferentes tenants son exitosos

**Preconditions**:
- Tenant_id válido
- Email RFC 5322 válido con longitud ≤ 254
- Credenciales de administrador válidas

**Postconditions**:
- ∀u1,u2 ∈ Users(t): u1.email.equalsIgnoreCase(u2.email) ⇒ u1.userId.equals(u2.userId)
- ∀u1 ∈ Users(t1), u2 ∈ Users(t2), t1 ≠ t2: u1.email.equalsIgnoreCase(u2.email) no implica conflicto

### Property 3: Password Complexity Enforcement

**Validates**: Requirements 2.5, 2.6, 2.7, 9.7, 9.8

**Property Statement**: Para todas las contraseñas p recibidas en CreateUserRequest, si p cumple las reglas de complejidad entonces el sistema las acepta, de lo contrario las rechaza con código HTTP 400 y mensaje específico.

**Test Strategy**:
- Generar contraseñas aleatorias con diferentes propiedades:
  - Longitud < 8, 8-128, > 128
  - Con/sin mayúsculas
  - Con/sin minúsculas  
  - Con/sin dígitos
  - Con/sin caracteres especiales
- Enviar CreateUserRequest con cada contraseña
- Verificar respuesta:
  - HTTP 201 si cumple todas las reglas
  - HTTP 400 si falla cualquier regla
- Verificar que mensaje de error contiene "La contraseña debe tener"

**Preconditions**:
- Email único válido
- Nombre válido (2-100 chars)
- Phone válido (7-20 dígitos)

**Postconditions**:
- Aceptado si length ∈ [8,128] ∧ ∃A-Z ∧ ∃a-z ∧ ∃0-9 ∧ ∃[@$!%*?&]
- Rechazado con 400 si falta cualquier condición
- Hash BCrypt almacenado (≠ password en texto plano)

### Property 4: Multi-tenancy Isolation

**Validates**: Requirements 5.1, 5.2, 5.3, 5.7

**Property Statement**: Para todas las operaciones de lectura (SELECT) ejecutadas por un usuario con Business_Role, el sistema aplica automáticamente el filtro WHERE tenant_id = :currentTenantId extraído del JWT.

**Test Strategy**:
- Crear usuarios en diferentes tenants (t1, t2)
- Autenticar usuario de tenant t1
- Ejecutar operaciones:
  - GET /users
  - GET /users/{id}
  - Buscar por email
  - Contar usuarios
- Verificar que solo retorna usuarios de tenant t1
- Verificar que ningún usuario de tenant t2 es visible
- Repetir con usuario de tenant t2

**Preconditions**:
- Usuario autenticado con JWT válido conteniendo tenant_id ≠ null
- Cuenta activa con Business_Role (admin, manager, veterinarian, worker)

**Postconditions**:
- ResultSet ⊆ Users(tenant_id = currentTenantId)
- ∀u ∈ ResultSet: u.tenantId = currentTenantId
- |ResultSet| = COUNT(users WHERE tenant_id = currentTenantId)

### Property 5: JWT Expiration Consistency

**Validates**: Requirements 1.1, 10.1, 10.2, 10.3

**Property Statement**: Para todos los tokens JWT generados por el sistema, la diferencia entre claim exp y claim iat es exactamente 24 horas (86,400 segundos).

**Test Strategy**:
- Generar usuarios aleatorios con diferentes roles
- Autenticar cada usuario para obtener JWT
- Parsear JWT, extraer claims exp y iat
- Calcular diferencia: exp - iat
- Verificar que diferencia = 86400 segundos ± 1 segundo (tolerancia de clock skew)
- Verificar que exp > timestamp actual UTC al momento de generación

**Preconditions**:
- Autenticación exitosa
- No rate limiting
- Cuenta activa

**Postconditions**:
- ∀token generado: token.exp - token.iat = 86400 ± 1
- token.exp > CurrentTimestampUTC
- token.iat ≤ CurrentTimestampUTC

### Property 6: Role-Based Authorization Hierarchy

**Validates**: Requirements 4.1-4.9

**Property Statement**: Para todos los pares de roles (r1, r2) donde r1 tiene permisos estrictamente mayores que r2, cualquier operación permitida para r2 también está permitida para r1, pero existen operaciones permitidas para r1 que no están permitidas para r2.

**Test Strategy**:
- Definir jerarquía de permisos:
  - super_admin > support > admin > manager > veterinarian > worker
- Para cada par (r1, r2) donde r1 > r2:
  - Crear usuario con rol r1, autenticar
  - Crear usuario con rol r2, autenticar
  - Ejecutar conjunto de operaciones representativas
  - Verificar: operations_allowed(r2) ⊆ operations_allowed(r1)
  - Verificar: operations_allowed(r1) ⊃ operations_allowed(r2)
- Operaciones a probar: CRUD users, read cattle, CRUD health records, etc.

**Preconditions**:
- Tenant_id válido
- Operaciones definidas en requirements (Req 4)

**Postconditions**:
- super_admin ⊆ support ⊆ admin ⊆ manager ⊆ veterinarian ⊆ worker (en permisos)
- ∀r1,r2: r1 > r2 ⇒ Permissions(r1) ⊇ Permissions(r2)
- ∃ operación: ∈ Permissions(r1) ∧ ∉ Permissions(r2)

### Property 7: Audit Trail Completeness

**Validates**: Requirements 6.8, 12.1-12.4, 12.5

**Property Statement**: Para todas las operaciones CRUD en la entidad User, el sistema registra una entrada completa en la tabla users_audit con timestamp, usuario responsable, tipo de operación, valores anteriores y valores nuevos.

**Test Strategy**:
- Generar operaciones aleatorias sobre usuarios:
  - CREATE user
  - UPDATE name/email/phone/role
  - DEACTIVATE user
- Para cada operación:
  - Ejecutar operación
  - Consultar tabla users_audit
  - Verificar presencia de registro
  - Verificar campos completos:
    - timestamp UTC reciente
    - modified_by = usuario autenticado
    - operation_type correcto
    - old_values contiene estado anterior (null para CREATE)
    - new_values contiene estado posterior
- Verificar atomicidad: operación exitosa ⇔ registro de auditoría creado

**Preconditions**:
- Usuario autenticado con permisos suficientes
- Operación válida según reglas de negocio

**Postconditions**:
- ∀ operación ∈ {CREATE, UPDATE, DEACTIVATE}: ∃ registro ∈ users_audit
- registro.timestamp ≈ CurrentTimestampUTC
- registro.modified_by = current_user.userId
- registro.operation_type = operación
- registro.old_values representa estado anterior
- registro.new_values representa estado posterior

### Property 8: Email Format Validation

**Validates**: Requirements 1.6, 2.5

**Property Statement**: Para todas las strings s recibidas como email, el sistema acepta s si y solo si s cumple RFC 5322 y longitud ≤ 254 caracteres.

**Test Strategy**:
- Generar strings aleatorias:
  - RFC 5322 válidos
  - RFC 5322 inválidos
  - Longitud ≤ 254
  - Longitud > 254
- Enviar en LoginRequest o CreateUserRequest
- Verificar:
  - Aceptado si RFC 5322 válido ∧ length ≤ 254
  - Rechazado con HTTP 400 si RFC 5322 inválido ∨ length > 254
- Verificar que mensaje de error indica problema específico

**Preconditions**:
- Request DTO válido excepto email
- Contraseña válida (si es LoginRequest)

**Postconditions**:
- Accepted(s) ⇔ RFC5322(s) ∧ |s| ≤ 254
- Rejected(s) ⇒ HTTP 400 ∧ error_message indica problema
- ∀s1,s2: s1.equalsIgnoreCase(s2) ⇒ tratamiento consistente

### Property 9: SaaS vs Business Role Behavior

**Validates**: Requirements 1.5, 1.7, 3.1, 3.2, 3.3, 5.3, 5.6

**Property Statement**: Para todos los usuarios con SaaS_Role (super_admin, support), el sistema permite operaciones sin filtro de tenant y con tenant_id = null en JWT. Para todos los usuarios con Business_Role (admin, manager, veterinarian, worker), el sistema requiere tenant_id ≠ null y aplica filtro automático.

**Test Strategy**:
- Crear usuarios con cada rol (6 roles)
- Autenticar cada usuario
- Extraer JWT, verificar claims:
  - SaaS_Role ⇒ tenant_id = null
  - Business_Role ⇒ tenant_id ≠ null
- Ejecutar operaciones de lectura:
  - SaaS_Role ⇒ sin filtro tenant (ve todos los tenants)
  - Business_Role ⇒ solo ve su tenant
- Verificar autorización:
  - super_admin puede operar en cualquier tenant
  - support solo lectura en cualquier tenant
  - Business_Roles solo en su tenant

**Preconditions**:
- Usuarios pre-creados en múltiples tenants
- Permisos definidos según requirements

**Postconditions**:
- SaaS_Role(u) ⇒ u.tenantId = null ∧ u.jwt.tenantId = null
- Business_Role(u) ⇒ u.tenantId ≠ null ∧ u.jwt.tenantId ≠ null
- SaaS_Role(u) ⇒ puede leer ∀ tenant
- Business_Role(u) ⇒ solo puede leer tenant_id = u.tenantId

### Property 10: Password Redaction in Logs

**Validates**: Requirements 11.1, 11.2

**Property Statement**: Para todos los mensajes de log generados por el sistema, si un campo contiene "password" o "secret" en su nombre (case-insensitive), el valor del campo es reemplazado por "[REDACTED]" antes de escribirse en el log.

**Test Strategy**:
- Configurar logger en nivel DEBUG/TRACE
- Ejecutar operaciones que contengan campos password:
  - LoginRequest con contraseña
  - CreateUserRequest con contraseña
  - UpdatePasswordRequest
  - Errores de validación de contraseña
- Capturar output del logger
- Verificar que:
  - Ninguna línea contiene contraseñas en texto plano
  - Campos "password", "passwordHash", "secretKey" muestran "[REDACTED]"
  - Otros campos (email, name) se muestran normalmente
- Probar con diferentes niveles de log (ERROR, WARN, INFO, DEBUG, TRACE)

**Preconditions**:
- Logger configurado para capturar output
- Operaciones que involucran datos sensibles

**Postconditions**:
- ∀ línea ∈ logs: ¬∃ contraseña_en_texto_plano
- ∀ campo ∈ {password.*, secret.*}: valor_log = "[REDACTED]"
- ∀ campo ∉ {password.*, secret.*}: valor_log = valor_real
- Regla aplica ∀ nivel_log ∈ {ERROR, WARN, INFO, DEBUG, TRACE}


**Property Statement**: Para todos los mensajes de log generados por el sistema, si un campo contiene "password" o "secret" en su nombre (case-insensitive), el valor del campo es reemplazado por "[REDACTED]" antes de escribirse en el log.

**Test Strategy**:
- Configurar logger en nivel DEBUG/TRACE
- Ejecutar operaciones que contengan campos password:
  - LoginRequest con contraseña
  - CreateUserRequest con contraseña
  - UpdatePasswordRequest
  - Errores de validación de contraseña
- Capturar output del logger
- Verificar que:
  - Ninguna línea contiene contraseñas en texto plano
  - Campos "password", "passwordHash", "secretKey" muestran "[REDACTED]"
  - Otros campos (email, name) se muestran normalmente
- Probar con diferentes niveles de log (ERROR, WARN, INFO, DEBUG, TRACE)

**Preconditions**:
- Logger configurado para capturar output
- Operaciones que involucran datos sensibles

**Postconditions**:
- ∀ línea ∈ logs: ¬∃ contraseña_en_texto_plano
- ∀ campo ∈ {password.*, secret.*}: valor_log = "[REDACTED]"
- ∀ campo ∉ {password.*, secret.*}: valor_log = valor_real
- Regla aplica ∀ nivel_log ∈ {ERROR, WARN, INFO, DEBUG, TRACE}

---

## ✅ Documento de Diseño Completado

El diseño técnico para el módulo `user-management` de Vacapp está completo y cumple con:

1. **Arquitectura Spring Modulith**: Estructura modular con API pública `UsersService.java` y encapsulamiento interno
2. **Clean Architecture**: Separación clara de dominio, aplicación e infraestructura
3. **Multi-tenancy**: Sistema completo con filtrado automático por tenant_id
4. **Seguridad JWT**: Autenticación y autorización basada en roles
5. **Auditoría completa**: Registro de todas las operaciones críticas
6. **Interfaz dual**: API REST para móvil + interfaz web Thymeleaf
7. **Propiedades de correctitud**: 10 propiedades formales con estrategias de testing

**Próximo paso**: Generar la lista de tareas de implementación (tasks.md) basada en este diseño.
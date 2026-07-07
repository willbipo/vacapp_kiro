package mx.vacapp.users.internal.infrastructure.controllers.mobile;

import jakarta.validation.Valid;
import mx.vacapp.users.internal.application.usecases.commands.CreateUserCommand;
import mx.vacapp.users.internal.application.usecases.commands.UpdateUserCommand;
import mx.vacapp.users.internal.application.usecases.commands.UserResult;
import mx.vacapp.users.internal.application.usecases.user.CreateUserUseCase;
import mx.vacapp.users.internal.application.usecases.user.DeactivateUserUseCase;
import mx.vacapp.users.internal.application.usecases.user.GetUserUseCase;
import mx.vacapp.users.internal.application.usecases.user.ListUsersUseCase;
import mx.vacapp.users.internal.application.usecases.user.UpdateUserUseCase;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.api.UsersApi;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.CreateUserRequest;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.PaginationMetadata;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.UpdateUserRequest;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.UserListResponse;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.UserResponse;
import mx.vacapp.users.internal.infrastructure.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador REST para gestión de usuarios.
 * <p>
 * Implementa la interfaz {@link UsersApi} generada por openapi-generator-maven-plugin
 * desde el archivo openapi-users.yaml.
 * </p>
 * <p>
 * Este controlador proporciona endpoints CRUD completos para gestión de usuarios:
 * listar, obtener por ID, crear, actualizar y desactivar usuarios.
 * Todos los endpoints requieren autenticación JWT y respetan el aislamiento multi-tenant.
 * </p>
 * <p>
 * <strong>Arquitectura:</strong>
 * <ul>
 *   <li>Capa de infraestructura (controllers/mobile) - adaptador de entrada HTTP</li>
 *   <li>Implementa la interfaz generada UsersApi (OpenAPI Design-First)</li>
 *   <li>Recibe DTOs Request generados desde OpenAPI</li>
 *   <li>Mapea DTOs a comandos de dominio (CreateUserCommand, UpdateUserCommand)</li>
 *   <li>Delega la lógica de negocio a los casos de uso correspondientes</li>
 *   <li>Mapea resultados de casos de uso a DTOs Response</li>
 *   <li>Retorna ResponseEntity con códigos HTTP apropiados</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Multi-tenancy:</strong>
 * Todos los endpoints respetan el aislamiento de datos por tenant. El contexto del tenant
 * se establece automáticamente por el JwtAuthenticationFilter basado en el token JWT.
 * Los casos de uso y repositorios filtran automáticamente por tenant_id del contexto.
 * </p>
 *
 * @see UsersApi
 * @see ListUsersUseCase
 * @see GetUserUseCase
 * @see CreateUserUseCase
 * @see UpdateUserUseCase
 * @see DeactivateUserUseCase
 * @see CreateUserRequest
 * @see UpdateUserRequest
 * @see UserResponse
 * @see UserListResponse
 */
@RestController
@RequestMapping("/api/v1")
public class UserRestController implements UsersApi {
    
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserUseCase getUserUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    
    /**
     * Constructor con inyección de dependencias de todos los casos de uso.
     *
     * @param listUsersUseCase caso de uso para listar usuarios con paginación
     * @param getUserUseCase caso de uso para obtener usuario por ID
     * @param createUserUseCase caso de uso para crear nuevo usuario
     * @param updateUserUseCase caso de uso para actualizar usuario existente
     * @param deactivateUserUseCase caso de uso para desactivar usuario (soft delete)
     */
    public UserRestController(
            ListUsersUseCase listUsersUseCase,
            GetUserUseCase getUserUseCase,
            CreateUserUseCase createUserUseCase,
            UpdateUserUseCase updateUserUseCase,
            DeactivateUserUseCase deactivateUserUseCase) {
        this.listUsersUseCase = listUsersUseCase;
        this.getUserUseCase = getUserUseCase;
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deactivateUserUseCase = deactivateUserUseCase;
    }
    
    /**
     * Lista usuarios del tenant actual con paginación.
     * <p>
     * Obtiene una lista paginada de usuarios del tenant actual.
     * Los usuarios con rol SaaS (super_admin, support) pueden ver usuarios de todos los tenants.
     * Los usuarios con roles Business solo ven usuarios de su propio tenant.
     * </p>
     * <p>
     * <strong>Parámetros de paginación:</strong>
     * <ul>
     *   <li>page: número de página (0-based), valor por defecto: 0</li>
     *   <li>size: tamaño de página (1-100), valor por defecto: 50, máximo: 100</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Flujo:</strong>
     * <ol>
     *   <li>Ejecuta ListUsersUseCase.execute(page, size)</li>
     *   <li>Obtiene total de usuarios con ListUsersUseCase.count()</li>
     *   <li>Calcula metadatos de paginación</li>
     *   <li>Mapea UserResult a UserResponse DTO</li>
     *   <li>Retorna HTTP 200 OK con UserListResponse</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Códigos HTTP:</strong>
     * <ul>
     *   <li>200 OK - Lista de usuarios obtenida exitosamente</li>
     *   <li>401 Unauthorized - Token JWT inválido o ausente</li>
     *   <li>403 Forbidden - Permisos insuficientes</li>
     * </ul>
     * </p>
     *
     * @param page número de página (0-based), opcional, valor por defecto: 0
     * @param size cantidad de elementos por página (1-100), opcional, valor por defecto: 50
     * @return ResponseEntity con UserListResponse y código HTTP 200
     */
    @Override
    public ResponseEntity<UserListResponse> listUsers(Integer page, Integer size) {
        // Validar parámetros de paginación (pueden ser null)
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size >= 1 && size <= 100) ? size : 50;
        
        // Ejecutar caso de uso para obtener usuarios paginados
        var users = listUsersUseCase.execute(pageNumber, pageSize);
        
        // Obtener total de usuarios para metadatos de paginación
        long totalUsers = listUsersUseCase.count();
        
        // Mapear UserResult a UserResponse
        var userResponses = users.stream()
            .map(this::mapUserResultToUserResponse)
            .collect(Collectors.toList());
        
        // Crear metadatos de paginación
        var pagination = new PaginationMetadata(
            pageNumber,
            pageSize,
            (int) totalUsers
        );
        
        // Crear respuesta completa
        var response = new UserListResponse(userResponses, pagination);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene un usuario por su ID.
     * <p>
     * Obtiene los detalles de un usuario específico por su UUID.
     * Los usuarios solo pueden ver usuarios de su propio tenant (excepto roles SaaS).
     * </p>
     * <p>
     * <strong>Flujo:</strong>
     * <ol>
     *   <li>Ejecuta GetUserUseCase.execute(userId)</li>
     *   <li>Mapea UserResult a UserResponse DTO</li>
     *   <li>Retorna HTTP 200 OK con UserResponse</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Códigos HTTP:</strong>
     * <ul>
     *   <li>200 OK - Usuario encontrado</li>
     *   <li>401 Unauthorized - Token JWT inválido o ausente</li>
     *   <li>403 Forbidden - Usuario pertenece a otro tenant</li>
     *   <li>404 Not Found - Usuario no encontrado</li>
     * </ul>
     * </p>
     *
     * @param id UUID del usuario a buscar
     * @return ResponseEntity con UserResponse y código HTTP 200
     */
    @Override
    public ResponseEntity<UserResponse> getUserById(UUID id) {
        // Ejecutar caso de uso para obtener usuario por ID
        var userResult = getUserUseCase.execute(id);
        
        // Mapear UserResult a UserResponse
        var response = mapUserResultToUserResponse(userResult);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Crea un nuevo usuario.
     * <p>
     * Crea un nuevo usuario en el tenant actual.
     * Requiere rol admin o super_admin.
     * El email debe ser único dentro del tenant.
     * </p>
     * <p>
     * <strong>Flujo:</strong>
     * <ol>
     *   <li>Extraer UUID del usuario autenticado desde contexto de seguridad</li>
     *   <li>Mapear CreateUserRequest DTO a CreateUserCommand</li>
     *   <li>Ejecutar CreateUserUseCase.execute(command)</li>
     *   <li>Mapear UserResult a UserResponse DTO</li>
     *   <li>Retornar HTTP 201 Created con UserResponse</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Códigos HTTP:</strong>
     * <ul>
     *   <li>201 Created - Usuario creado exitosamente</li>
     *   <li>400 Bad Request - Datos de entrada inválidos o contraseña no cumple requisitos</li>
     *   <li>401 Unauthorized - Token JWT inválido o ausente</li>
     *   <li>403 Forbidden - Permisos insuficientes</li>
     *   <li>409 Conflict - Email ya registrado en esta organización</li>
     * </ul>
     * </p>
     * <p>
     * <strong>NOTA:</strong> Por ahora se usa un UUID hardcodeado para createdBy.
     * En la siguiente iteración se obtendrá del contexto de seguridad del usuario autenticado.
     * </p>
     *
     * @param createUserRequest DTO con datos del nuevo usuario
     * @return ResponseEntity con UserResponse y código HTTP 201
     */
    @Override
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        // Obtener userId del usuario autenticado desde contexto de seguridad
        UUID createdBy = SecurityUtils.getRequiredCurrentUserId();
        
        // Obtener tenantId del contexto de seguridad (puede ser null para usuarios SaaS)
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        
        // Mapear DTO a comando de dominio
        var command = new CreateUserCommand(
            createUserRequest.email(),
            createUserRequest.name(),
            createUserRequest.phone(),
            createUserRequest.password(),
            createUserRequest.role(),
            tenantId,
            createdBy
        );
        
        // Ejecutar caso de uso para crear usuario
        var userResult = createUserUseCase.execute(command);
        
        // Mapear UserResult a UserResponse
        var response = mapUserResultToUserResponse(userResult);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Actualiza un usuario existente.
     * <p>
     * Actualiza los datos de un usuario existente.
     * Requiere rol admin o super_admin.
     * No se permite cambiar el tenant_id de un usuario.
     * </p>
     * <p>
     * <strong>Flujo:</strong>
     * <ol>
     *   <li>Extraer UUID del usuario autenticado desde contexto de seguridad</li>
     *   <li>Mapear UpdateUserRequest DTO a UpdateUserCommand</li>
     *   <li>Ejecutar UpdateUserUseCase.execute(command)</li>
     *   <li>Mapear UserResult a UserResponse DTO</li>
     *   <li>Retornar HTTP 200 OK con UserResponse</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Códigos HTTP:</strong>
     * <ul>
     *   <li>200 OK - Usuario actualizado exitosamente</li>
     *   <li>400 Bad Request - Datos de entrada inválidos</li>
     *   <li>401 Unauthorized - Token JWT inválido o ausente</li>
     *   <li>403 Forbidden - Permisos insuficientes o usuario de otro tenant</li>
     *   <li>404 Not Found - Usuario no encontrado</li>
     * </ul>
     * </p>
     * <p>
     * <strong>NOTA:</strong> Por ahora se usa un UUID hardcodeado para updatedBy.
     * En la siguiente iteración se obtendrá del contexto de seguridad del usuario autenticado.
     * </p>
     *
     * @param id UUID del usuario a actualizar
     * @param updateUserRequest DTO con datos a actualizar
     * @return ResponseEntity con UserResponse y código HTTP 200
     */
    @Override
    public ResponseEntity<UserResponse> updateUser(UUID id, @Valid @RequestBody UpdateUserRequest updateUserRequest) {
        // Obtener userId del usuario autenticado desde contexto de seguridad
        UUID updatedBy = SecurityUtils.getRequiredCurrentUserId();
        
        // Mapear DTO a comando de dominio
        var command = new UpdateUserCommand(
            id,
            updateUserRequest.name(),
            updateUserRequest.phone(),
            updateUserRequest.role(),
            updatedBy
        );
        
        // Ejecutar caso de uso para actualizar usuario
        var userResult = updateUserUseCase.execute(command);
        
        // Mapear UserResult a UserResponse
        var response = mapUserResultToUserResponse(userResult);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Desactiva un usuario (soft delete).
     * <p>
     * Desactiva un usuario (eliminación lógica).
     * No se realiza DELETE físico, solo se marca el estado como 'inactive'.
     * Requiere rol admin o super_admin.
     * </p>
     * <p>
     * <strong>Flujo:</strong>
     * <ol>
     *   <li>Extraer UUID del usuario autenticado desde contexto de seguridad</li>
     *   <li>Ejecutar DeactivateUserUseCase.execute(id, deactivatedBy)</li>
     *   <li>Retornar HTTP 204 No Content</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Códigos HTTP:</strong>
     * <ul>
     *   <li>204 No Content - Usuario desactivado exitosamente (sin contenido)</li>
     *   <li>401 Unauthorized - Token JWT inválido o ausente</li>
     *   <li>403 Forbidden - Permisos insuficientes o usuario de otro tenant</li>
     *   <li>404 Not Found - Usuario no encontrado</li>
     * </ul>
     * </p>
     * <p>
     * <strong>NOTA:</strong> Por ahora se usa un UUID hardcodeado para deactivatedBy.
     * En la siguiente iteración se obtendrá del contexto de seguridad del usuario autenticado.
     * </p>
     *
     * @param id UUID del usuario a desactivar
     * @return ResponseEntity vacío con código HTTP 204
     */
    @Override
    public ResponseEntity<Void> deleteUser(UUID id) {
        // Obtener userId del usuario autenticado desde contexto de seguridad
        UUID deactivatedBy = SecurityUtils.getRequiredCurrentUserId();
        
        // Ejecutar caso de uso para desactivar usuario
        deactivateUserUseCase.execute(id, deactivatedBy);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Mapea UserResult (capa de aplicación) a UserResponse (capa de infraestructura).
     * <p>
     * Convierte el DTO interno del caso de uso en el DTO de respuesta HTTP.
     * Ambos DTOs tienen estructura similar pero son entidades separadas para
     * mantener la separación de capas (Clean Architecture).
     * </p>
     * <p>
     * NOTA: El DTO UserResponse sigue el esquema definido en openapi-users.yaml
     * y NO incluye los campos createdBy y updatedBy por diseño (requerimiento
     * de seguridad y privacidad).
     * </p>
     *
     * @param userResult resultado del caso de uso
     * @return UserResponse DTO para respuesta HTTP
     */
    private UserResponse mapUserResultToUserResponse(UserResult userResult) {
        return new UserResponse(
            userResult.userId(),
            userResult.email(),
            userResult.name(),
            userResult.phone(),
            userResult.role(),
            userResult.status(),
            userResult.tenantId(),
            userResult.createdAt(),
            userResult.updatedAt()
        );
    }
}
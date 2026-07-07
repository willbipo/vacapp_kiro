package mx.vacapp.users.internal.infrastructure.controllers.mobile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import mx.vacapp.users.internal.application.usecases.auth.LoginUseCase;
import mx.vacapp.users.internal.application.usecases.commands.AuthResult;
import mx.vacapp.users.internal.application.usecases.commands.LoginCommand;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.api.AuthenticationApi;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.LoginRequest;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.LoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Controlador REST para autenticación.
 * <p>
 * Implementa la interfaz {@link AuthenticationApi} generada por openapi-generator-maven-plugin
 * desde el archivo openapi-users.yaml.
 * </p>
 * <p>
 * Este controlador proporciona el endpoint público de login que NO requiere autenticación JWT.
 * Es el punto de entrada para que los usuarios obtengan un token JWT válido por 24 horas.
 * </p>
 * <p>
 * <strong>Arquitectura:</strong>
 * <ul>
 *   <li>Capa de infraestructura (controllers/mobile) - adaptador de entrada HTTP</li>
 *   <li>Recibe DTOs Request generados desde OpenAPI</li>
 *   <li>Mapea DTOs a comandos de dominio (LoginCommand)</li>
 *   <li>Delega la lógica de negocio al caso de uso (LoginUseCase)</li>
 *   <li>Mapea resultados del caso de uso a DTOs Response</li>
 *   <li>Retorna ResponseEntity con código HTTP apropiado</li>
 * </ul>
 * </p>
 *
 * @see AuthenticationApi
 * @see LoginUseCase
 * @see LoginRequest
 * @see LoginResponse
 */
@RestController
@RequestMapping("/api/v1")
public class AuthRestController implements AuthenticationApi {
    
    private final LoginUseCase loginUseCase;
    
    /**
     * Constructor con inyección de dependencias.
     *
     * @param loginUseCase caso de uso para autenticación de usuarios
     */
    public AuthRestController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }
    
    /**
     * Autentica un usuario con email y contraseña.
     * <p>
     * Este es el único endpoint público que NO requiere autenticación JWT previa.
     * </p>
     * <p>
     * <strong>Flujo:</strong>
     * <ol>
     *   <li>Extrae clientIp del request HTTP (X-Forwarded-For o RemoteAddr)</li>
     *   <li>Extrae userAgent del request HTTP</li>
     *   <li>Mapea LoginRequest DTO a LoginCommand</li>
     *   <li>Ejecuta LoginUseCase.execute(command)</li>
     *   <li>Mapea AuthResult a LoginResponse DTO</li>
     *   <li>Retorna HTTP 200 OK con el token JWT</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Códigos HTTP:</strong>
     * <ul>
     *   <li>200 OK - Autenticación exitosa, retorna token JWT</li>
     *   <li>400 Bad Request - Datos de entrada inválidos (validación Bean Validation)</li>
     *   <li>401 Unauthorized - Credenciales inválidas</li>
     *   <li>403 Forbidden - Cuenta inactiva o bloqueada</li>
     *   <li>429 Too Many Requests - Rate limit excedido</li>
     * </ul>
     * </p>
     *
     * @param loginRequest DTO con email y contraseña (validado con @Valid)
     * @return ResponseEntity con LoginResponse y código HTTP 200
     */
    @Override
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        // Obtener HttpServletRequest del contexto de Spring
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        
        // Extraer IP del cliente (considerar proxy con X-Forwarded-For)
        String clientIp = extractClientIp(httpRequest);
        
        // Extraer User-Agent del header
        String userAgent = extractUserAgent(httpRequest);
        
        // Mapear DTO a comando de dominio
        LoginCommand command = new LoginCommand(
            loginRequest.email(),
            loginRequest.password(),
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
            result.role(),
            result.tenantId()
        );
        
        // Retornar respuesta exitosa
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene el HttpServletRequest actual desde el contexto de Spring.
     * <p>
     * Utiliza RequestContextHolder para acceder al request en el hilo actual.
     * </p>
     *
     * @return HttpServletRequest actual
     * @throws IllegalStateException si no hay request en el contexto
     */
    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
    
    /**
     * Extrae la dirección IP del cliente desde el HttpServletRequest.
     * <p>
     * Considera el header X-Forwarded-For cuando la aplicación está detrás de un proxy/load balancer.
     * Si X-Forwarded-For contiene múltiples IPs (cadena de proxies), toma la primera (IP original del cliente).
     * </p>
     * <p>
     * <strong>Orden de prioridad:</strong>
     * <ol>
     *   <li>X-Forwarded-For (primera IP si hay múltiples)</li>
     *   <li>X-Real-IP</li>
     *   <li>request.getRemoteAddr() (IP directa)</li>
     * </ol>
     * </p>
     *
     * @param request HttpServletRequest
     * @return dirección IP del cliente, o "unknown" si no se puede determinar
     */
    private String extractClientIp(HttpServletRequest request) {
        // Intentar obtener IP desde X-Forwarded-For (usado por proxies y load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For puede contener múltiples IPs: "client, proxy1, proxy2"
            // La primera es la IP original del cliente
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Intentar obtener IP desde X-Real-IP (alternativa usada por algunos proxies)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        // Fallback: obtener IP directamente del request
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isEmpty()) ? remoteAddr : "unknown";
    }
    
    /**
     * Extrae el User-Agent desde el HttpServletRequest.
     * <p>
     * El User-Agent identifica el navegador/aplicación del cliente (ej: "Mozilla/5.0...", "Vacapp-Mobile/1.0").
     * Se utiliza para auditoría y detección de patrones de uso sospechosos.
     * </p>
     *
     * @param request HttpServletRequest
     * @return User-Agent del cliente, o "unknown" si no está presente
     */
    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        
        // Si el User-Agent está presente y no está vacío, retornarlo
        if (userAgent != null && !userAgent.isEmpty()) {
            // Limitar longitud a 500 caracteres (como especifica el requirement)
            return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
        }
        
        // Fallback si no hay User-Agent
        return "unknown";
    }
}

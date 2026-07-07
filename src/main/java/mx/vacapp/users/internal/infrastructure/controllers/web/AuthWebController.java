package mx.vacapp.users.internal.infrastructure.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador web para autenticación y dashboard.
 * <p>
 * Este controlador maneja las vistas HTML (Thymeleaf) relacionadas con autenticación
 * y navegación principal de la aplicación web. No maneja lógica de negocio
 * directamente - la autenticación real se realiza mediante la API REST {@code /api/v1/auth/login}.
 * </p>
 * <p>
 * Las vistas retornadas por este controlador contienen JavaScript vanilla que se
 * comunica con la API REST para autenticación, almacenando el token JWT en
 * {@code sessionStorage} y redirigiendo al dashboard tras login exitoso.
 * </p>
 * <p>
 * <strong>Roles de seguridad:</strong>
 * <ul>
 *   <li>{@code /auth/login} - Público (no requiere autenticación)</li>
 *   <li>{@code /dashboard} - Requiere autenticación JWT válida</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Arquitectura:</strong>
 * <ul>
 *   <li>Capa de infraestructura (controllers/web) - adaptador de entrada HTTP MVC</li>
 *   <li>Sirve vistas Thymeleaf para interfaz web</li>
 *   <li>No contiene lógica de negocio - solo maneja navegación y presentación</li>
 *   <li>La autenticación real se delega a la API REST {@code /api/v1/auth/login}</li>
 *   <li>La autorización se maneja mediante Spring Security + filtro JWT</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Flujo de autenticación web:</strong>
 * <ol>
 *   <li>Usuario accede a {@code /auth/login} (GET)</li>
 *   <li>Se muestra formulario de login (login.html)</li>
 *   <li>Usuario ingresa email y contraseña</li>
 *   <li>JavaScript envía POST a {@code /api/v1/auth/login}</li>
 *   <li>Si exitoso, guarda token en sessionStorage</li>
 *   <li>Redirige a {@code /dashboard}</li>
 *   <li>Dashboard verifica token y muestra contenido protegido</li>
 * </ol>
 * </p>
 *
 * @see org.springframework.stereotype.Controller
 * @see org.springframework.web.bind.annotation.GetMapping
 */
@Controller
public class AuthWebController {
    
    /**
     * Constructor por defecto.
     * <p>
     * Inyección de dependencias por constructor siguiendo las reglas de AGENTS.md.
     * Actualmente no se requieren dependencias para este controlador, pero el
     * patrón de inyección por constructor se mantiene para consistencia.
     * </p>
     */
    public AuthWebController() {
        // Constructor vacío - no se requieren dependencias para este controlador
    }
    
    /**
     * Muestra la página de inicio de sesión.
     * <p>
     * Esta es una ruta pública que no requiere autenticación. Sirve el formulario
     * de login que permite a los usuarios ingresar sus credenciales.
     * </p>
     * <p>
     * <strong>Template:</strong> {@code templates/auth/login.html}
     * </p>
     * <p>
     * <strong>Contenido del formulario:</strong>
     * <ul>
     *   <li>Campo email (type="text")</li>
     *   <li>Campo contraseña (type="password")</li>
     *   <li>Botón submit</li>
     *   <li>JavaScript vanilla para manejar el envío del formulario</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Seguridad:</strong> Esta ruta debe estar configurada como pública
     * en {@code SecurityConfig.java} para permitir acceso sin autenticación.
     * </p>
     *
     * @return nombre del template Thymeleaf "auth/login"
     */
    @GetMapping("/auth/login")
    public String showLoginPage() {
        // Retorna el nombre del template Thymeleaf (sin extensión .html)
        // Spring Boot buscará en: src/main/resources/templates/auth/login.html
        return "auth/login";
    }
    
    /**
     * Muestra la página principal del dashboard.
     * <p>
     * Esta es una ruta protegida que requiere autenticación JWT válida. Sirve
     * la página principal de la aplicación después del login exitoso.
     * </p>
     * <p>
     * <strong>Template:</strong> {@code templates/dashboard/index.html}
     * </p>
     * <p>
     * <strong>Contenido del dashboard:</strong>
     * <ul>
     *   <li>Barra de navegación superior (navbar fragment)</li>
     *   <li>Barra lateral de navegación (sidebar fragment)</li>
     *   <li>Contenido principal con resumen y accesos rápidos</li>
     *   <li>JavaScript vanilla para interactuar con APIs REST</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Seguridad:</strong> Esta ruta debe estar configurada como protegida
     * en {@code SecurityConfig.java}. El acceso requiere:
     * <ol>
     *   <li>Token JWT válido en sessionStorage (manejado por JavaScript)</li>
     *   <li>Header Authorization: Bearer {token} en requests a APIs</li>
     *   <li>Filtro JWT valida el token antes de permitir acceso</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Nota:</strong> Aunque este endpoint retorna el template del dashboard,
     * la validación de autenticación y autorización se realiza a nivel de Spring Security
     * mediante el filtro JWT. El template en sí no contiene lógica de validación.
     * </p>
     *
     * @return nombre del template Thymeleaf "dashboard/index"
     */
    @GetMapping("/dashboard")
    public String showDashboard() {
        // Retorna el nombre del template Thymeleaf (sin extensión .html)
        // Spring Boot buscará en: src/main/resources/templates/dashboard/index.html
        return "dashboard/index";
    }
}
package mx.vacapp.users.internal.infrastructure.config;

import mx.vacapp.users.internal.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de Spring Security 6 para el módulo de usuarios.
 * <p>
 * Esta clase configura la seguridad de la aplicación usando el patrón SecurityFilterChain
 * de Spring Security 6, reemplazando el antiguo WebSecurityConfigurerAdapter (deprecado).
 * </p>
 * <p>
 * Responsabilidades principales:
 * <ul>
 *   <li>Definir rutas públicas (sin autenticación): /api/v1/auth/login, /auth/login, /css/**, /js/**, /health, /swagger-ui/**, /v3/api-docs/**</li>
 *   <li>Definir rutas protegidas (requieren autenticación): todas las demás</li>
 *   <li>Añadir JwtAuthenticationFilter antes de UsernamePasswordAuthenticationFilter</li>
 *   <li>Configurar CORS para permitir orígenes localhost en desarrollo</li>
 *   <li>Deshabilitar CSRF para REST API (stateless JWT)</li>
 *   <li>Habilitar CSRF para formularios web (Thymeleaf)</li>
 *   <li>Configurar gestión de sesiones como STATELESS (sin HttpSession)</li>
 *   <li>Configurar manejo de excepciones (401 para unauthorized, 403 para forbidden)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Arquitectura de seguridad:</strong>
 * <ul>
 *   <li>API REST (/api/**): Autenticación JWT stateless, CSRF deshabilitado</li>
 *   <li>Web MVC (/auth/**, /admin/**, /dashboard/**): Autenticación JWT + CSRF habilitado para formularios</li>
 *   <li>Recursos estáticos (/css/**, /js/**, /images/**): Acceso público</li>
 *   <li>Swagger UI y OpenAPI Docs: Acceso público en desarrollo</li>
 * </ul>
 * </p>
 *
 * @see org.springframework.security.config.annotation.web.builders.HttpSecurity
 * @see org.springframework.security.web.SecurityFilterChain
 * @see JwtAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    /**
     * Filtro de autenticación JWT inyectado por constructor.
     * <p>
     * Este filtro intercepta todos los requests HTTP, extrae y valida el token JWT,
     * y establece el contexto de seguridad de Spring Security.
     * </p>
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * Constructor con inyección de dependencias.
     * <p>
     * Spring inyectará automáticamente el JwtAuthenticationFilter al instanciar esta configuración.
     * </p>
     *
     * @param jwtAuthenticationFilter filtro de autenticación JWT
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    /**
     * Configura la cadena de filtros de seguridad (SecurityFilterChain).
     * <p>
     * Este bean define las reglas de seguridad para toda la aplicación.
     * Spring Security 6 requiere que este método retorne un SecurityFilterChain
     * en lugar de extender WebSecurityConfigurerAdapter.
     * </p>
     * <p>
     * <strong>Configuración de rutas:</strong>
     * <ul>
     *   <li>Públicas (sin autenticación):
     *     <ul>
     *       <li>/api/v1/auth/login - Endpoint de login REST</li>
     *       <li>/auth/login - Página de login web</li>
     *       <li>/css/**, /js/**, /images/** - Recursos estáticos</li>
     *       <li>/health - Health check para monitoreo</li>
     *       <li>/swagger-ui/**, /v3/api-docs/** - Documentación OpenAPI</li>
     *     </ul>
     *   </li>
     *   <li>Protegidas (requieren autenticación): Todas las demás rutas</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Configuración CSRF:</strong>
     * <ul>
     *   <li>Deshabilitado para /api/** (REST API stateless con JWT)</li>
     *   <li>Habilitado para rutas web (formularios Thymeleaf)</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Manejo de excepciones:</strong>
     * <ul>
     *   <li>401 Unauthorized: Retornado cuando no hay token JWT o es inválido</li>
     *   <li>403 Forbidden: Retornado cuando el usuario no tiene permisos para la operación</li>
     * </ul>
     * </p>
     *
     * @param http el objeto HttpSecurity para configurar
     * @return el SecurityFilterChain configurado
     * @throws Exception si ocurre un error durante la configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configuración de autorización de requests
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas - Acceso sin autenticación
                .requestMatchers(
                    "/api/v1/auth/login",  // Endpoint de login REST
                    "/auth/login",         // Página de login web
                    "/css/**",             // Hojas de estilo
                    "/js/**",              // JavaScript
                    "/images/**",          // Imágenes
                    "/health",             // Health check
                    "/swagger-ui/**",      // Swagger UI
                    "/v3/api-docs/**"      // OpenAPI Docs
                ).permitAll()
                
                // Rutas protegidas - Requieren autenticación
                .anyRequest().authenticated()
            )
            
            // Configuración CSRF
            .csrf(csrf -> csrf
                // Deshabilitar CSRF para API REST (stateless JWT)
                .ignoringRequestMatchers("/api/**")
                // CSRF habilitado para formularios web (Thymeleaf)
                // Spring Security genera automáticamente el token CSRF
                // y lo inyecta en formularios con th:action
            )
            
            // Configuración de CORS
            .cors(cors -> cors
                .configurationSource(corsConfigurationSource())
            )
            
            // Configuración de gestión de sesiones
            .sessionManagement(session -> session
                // STATELESS: No crear ni usar HttpSession
                // La autenticación se valida en cada request mediante JWT
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Añadir filtro JWT antes del filtro de autenticación estándar
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configuración de manejo de excepciones de seguridad
            .exceptionHandling(exception -> exception
                // 401 Unauthorized - Sin autenticación válida
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"Autenticación requerida\"}"
                    );
                })
                
                // 403 Forbidden - Autenticado pero sin permisos
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"error\":\"Forbidden\",\"message\":\"Acceso denegado\"}"
                    );
                })
            )
            
            // Deshabilitar formulario de login por defecto de Spring Security
            // (usamos nuestro propio formulario en /auth/login)
            .formLogin(AbstractHttpConfigurer::disable)
            
            // Deshabilitar HTTP Basic Authentication
            // (usamos JWT, no Basic Auth)
            .httpBasic(AbstractHttpConfigurer::disable)
            
            // Deshabilitar logout por defecto
            // (implementamos logout personalizado en el frontend)
            .logout(AbstractHttpConfigurer::disable);
        
        return http.build();
    }
    
    /**
     * Configura CORS (Cross-Origin Resource Sharing) para la aplicación.
     * <p>
     * Esta configuración permite requests desde orígenes localhost para desarrollo.
     * En producción, debería ajustarse para permitir solo los dominios específicos
     * de la aplicación.
     * </p>
     * <p>
     * <strong>Configuración actual (desarrollo):</strong>
     * <ul>
     *   <li>Orígenes permitidos: http://localhost:*, http://127.0.0.1:*</li>
     *   <li>Métodos permitidos: GET, POST, PUT, DELETE, OPTIONS</li>
     *   <li>Headers permitidos: Authorization, Content-Type, Accept</li>
     *   <li>Credenciales permitidas: true (permite envío de cookies y headers de auth)</li>
     *   <li>Max Age: 3600 segundos (1 hora de caché de preflight requests)</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Nota de seguridad:</strong> En producción, reemplazar allowedOriginPatterns
     * con allowedOrigins específicos (ej: "https://app.vacapp.mx").
     * </p>
     *
     * @return el CorsConfigurationSource configurado
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Permitir orígenes localhost para desarrollo
        // En producción, cambiar a dominios específicos
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()
        ));
        
        // Headers permitidos en requests
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Origin"
        ));
        
        // Headers expuestos en responses (accesibles desde JavaScript)
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "Content-Type"
        ));
        
        // Permitir envío de credenciales (cookies, Authorization header)
        configuration.setAllowCredentials(true);
        
        // Tiempo de caché de preflight requests (OPTIONS)
        configuration.setMaxAge(3600L);
        
        // Aplicar configuración a todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

package mx.vacapp.users.internal.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Filtro de autenticación JWT que intercepta todos los requests HTTP.
 * <p>
 * Este filtro es responsable de:
 * <ul>
 *   <li>Extraer el token JWT del header Authorization: Bearer {token}</li>
 *   <li>Validar la firma y expiración del token usando JwtTokenProvider</li>
 *   <li>Extraer información del usuario (userId, tenantId, roles) del token</li>
 *   <li>Establecer TenantContext con el tenant_id extraído del token</li>
 *   <li>Crear un objeto Authentication de Spring Security y establecerlo en SecurityContextHolder</li>
 *   <li>Permitir que el request continúe si el token es válido</li>
 *   <li>Rechazar con 401 Unauthorized si el token es inválido o está ausente en rutas protegidas</li>
 * </ul>
 * </p>
 * <p>
 * El filtro extiende OncePerRequestFilter para garantizar que se ejecuta exactamente
 * una vez por request, incluso en casos de forwards o includes internos.
 * </p>
 * <p>
 * <strong>Flujo de procesamiento:</strong>
 * <ol>
 *   <li>Extraer token del header Authorization</li>
 *   <li>Si no hay token, continuar con el request (Spring Security decide si rechazar)</li>
 *   <li>Si hay token, validar con JwtTokenProvider</li>
 *   <li>Si es válido, extraer claims (userId, tenantId, roles)</li>
 *   <li>Establecer TenantContext.setTenantId(tenantId)</li>
 *   <li>Crear Authentication con roles como GrantedAuthorities</li>
 *   <li>Establecer en SecurityContextHolder</li>
 *   <li>Continuar con la cadena de filtros</li>
 *   <li>En el bloque finally, limpiar TenantContext para evitar memory leaks</li>
 * </ol>
 * </p>
 *
 * @see org.springframework.web.filter.OncePerRequestFilter
 * @see JwtTokenProvider
 * @see TenantContext
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    /**
     * Prefijo esperado en el header Authorization.
     * <p>
     * El formato esperado es: "Authorization: Bearer {token}"
     * </p>
     */
    private static final String BEARER_PREFIX = "Bearer ";
    
    /**
     * Nombre del header HTTP donde se espera el token JWT.
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    
    /**
     * Proveedor de tokens JWT para validación y extracción de claims.
     * Inyectado por constructor.
     */
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * Constructor con inyección de dependencias.
     * <p>
     * Spring inyectará automáticamente el JwtTokenProvider al instanciar este filtro.
     * </p>
     *
     * @param jwtTokenProvider proveedor de tokens JWT para validación
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    /**
     * Método principal que procesa cada request HTTP exactamente una vez.
     * <p>
     * Este método es invocado automáticamente por el contenedor de servlets
     * para cada request que pasa por este filtro.
     * </p>
     *
     * @param request el HttpServletRequest actual
     * @param response el HttpServletResponse actual
     * @param filterChain la cadena de filtros para continuar el procesamiento
     * @throws ServletException si ocurre un error durante el procesamiento
     * @throws IOException si ocurre un error de I/O
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // 1. Extraer token JWT del header Authorization
            String jwt = extractJwtFromRequest(request);
            
            // 2. Si no hay token, continuar sin establecer autenticación
            //    (Spring Security decidirá si el endpoint requiere autenticación)
            if (jwt == null) {
                log.debug("No se encontró token JWT en el request a: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }
            
            // 3. Validar el token JWT (firma, expiración, estructura)
            if (!jwtTokenProvider.validateToken(jwt)) {
                log.warn("Token JWT inválido en request a: {}", request.getRequestURI());
                // No establecer autenticación - Spring Security retornará 401
                filterChain.doFilter(request, response);
                return;
            }
            
            // 4. Extraer información del usuario del token
            UUID userId = jwtTokenProvider.extractUserId(jwt);
            UUID tenantId = jwtTokenProvider.extractTenantId(jwt);
            List<String> roles = jwtTokenProvider.extractRoles(jwt);
            
            // 5. Validar que se pudieron extraer los datos esenciales
            if (userId == null || roles.isEmpty()) {
                log.error("Token JWT válido pero sin user_id o roles en request a: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }
            
            // 6. Establecer TenantContext con el tenant_id extraído
            //    (puede ser null para usuarios SaaS como super_admin y support)
            TenantContext.setTenantId(tenantId);
            log.debug("TenantContext establecido con tenant_id: {} para user_id: {}", tenantId, userId);
            
            // 7. Convertir roles a GrantedAuthorities de Spring Security
            //    Formato: "ROLE_admin", "ROLE_super_admin", etc.
            List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
            
            // 8. Crear objeto Authentication de Spring Security
            //    - Principal: user_id como String
            //    - Credentials: null (ya autenticado mediante JWT)
            //    - Authorities: roles convertidos a GrantedAuthorities
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                authorities
            );
            
            // 9. Establecer detalles adicionales del request (IP, session, etc.)
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // 10. Establecer el Authentication en el SecurityContextHolder
            //     Esto hace que el usuario esté "autenticado" para Spring Security
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("Autenticación establecida para user_id: {}, roles: {}, tenant_id: {}", 
                userId, roles, tenantId);
            
            // 11. Continuar con la cadena de filtros
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Error al procesar autenticación JWT: {}", e.getMessage(), e);
            // No re-lanzar la excepción - dejar que Spring Security maneje el caso sin autenticación
            filterChain.doFilter(request, response);
        } finally {
            // 12. CRÍTICO: Limpiar TenantContext al finalizar el request
            //     Esto previene memory leaks en servidores con thread pooling
            TenantContext.clear();
            log.debug("TenantContext limpiado para request a: {}", request.getRequestURI());
        }
    }
    
    /**
     * Extrae el token JWT del header Authorization del request.
     * <p>
     * El formato esperado es: "Authorization: Bearer {token}"
     * </p>
     * <p>
     * Este método:
     * <ul>
     *   <li>Lee el header "Authorization"</li>
     *   <li>Verifica que comience con "Bearer "</li>
     *   <li>Extrae y retorna la parte del token (sin el prefijo "Bearer ")</li>
     * </ul>
     * </p>
     *
     * @param request el HttpServletRequest del cual extraer el token
     * @return el token JWT sin el prefijo "Bearer ", o null si no está presente o el formato es incorrecto
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // Obtener el valor del header Authorization
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        // Verificar que el header existe y tiene contenido
        if (!StringUtils.hasText(bearerToken)) {
            return null;
        }
        
        // Verificar que comienza con "Bearer "
        if (!bearerToken.startsWith(BEARER_PREFIX)) {
            log.warn("Header Authorization no comienza con 'Bearer ': {}", 
                bearerToken.substring(0, Math.min(bearerToken.length(), 20)));
            return null;
        }
        
        // Extraer el token (sin el prefijo "Bearer ")
        String token = bearerToken.substring(BEARER_PREFIX.length());
        
        // Validar que el token no esté vacío después de quitar el prefijo
        if (!StringUtils.hasText(token)) {
            log.warn("Token JWT vacío después de remover prefijo 'Bearer '");
            return null;
        }
        
        return token;
    }
}

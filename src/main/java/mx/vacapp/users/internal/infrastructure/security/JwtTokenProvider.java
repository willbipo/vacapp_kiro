package mx.vacapp.users.internal.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import mx.vacapp.users.internal.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Proveedor de tokens JWT para autenticación stateless.
 * <p>
 * Este componente es responsable de:
 * <ul>
 *   <li>Generar tokens JWT con claims: sub (user_id), exp, iat, tenant_id, roles</li>
 *   <li>Validar la firma HMAC-SHA256 y expiración de tokens</li>
 *   <li>Extraer claims individuales: userId, tenantId, roles</li>
 * </ul>
 * </p>
 * <p>
 * Los tokens tienen una expiración configurable (por defecto 24 horas) y son firmados
 * con HMAC-SHA256 usando una clave secreta de mínimo 256 bits.
 * </p>
 */
@Component
public class JwtTokenProvider {
    
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;
    
    @Value("${jwt.issuer:vacapp}")
    private String jwtIssuer;
    
    /**
     * Genera un token JWT a partir de un objeto User.
     * <p>
     * El token incluye los siguientes claims:
     * <ul>
     *   <li>sub: user_id (UUID como string)</li>
     *   <li>email: email del usuario</li>
     *   <li>tenant_id: UUID del tenant o null para usuarios SaaS</li>
     *   <li>roles: array con un único rol del usuario</li>
     *   <li>iat: timestamp de emisión</li>
     *   <li>exp: timestamp de expiración (iat + 24 horas)</li>
     *   <li>iss: emisor del token (vacapp)</li>
     * </ul>
     * </p>
     *
     * @param user el usuario para el cual generar el token
     * @return el token JWT como string
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(jwtExpirationMs, ChronoUnit.MILLIS);
        
        // Construir array de roles con un único elemento
        List<String> roles = List.of(user.getRole().getValue());
        
        // Generar la clave secreta para firma HMAC-SHA256
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        // Construir el token JWT
        String token = Jwts.builder()
            .subject(user.getUserId().toString())
            .claim("email", user.getEmail())
            .claim("tenant_id", user.getTenantId() != null ? user.getTenantId().toString() : null)
            .claim("roles", roles)
            .issuer(jwtIssuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryDate))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
        
        log.debug("Token JWT generado para usuario: {}, tenant: {}, roles: {}", 
            user.getEmail(), user.getTenantId(), roles);
        
        return token;
    }
    
    /**
     * Valida un token JWT verificando su firma y expiración.
     * <p>
     * Realiza las siguientes validaciones:
     * <ul>
     *   <li>Estructura correcta (header.payload.signature)</li>
     *   <li>Firma HMAC-SHA256 válida</li>
     *   <li>Token no expirado (con tolerancia de 60 segundos)</li>
     *   <li>Presencia de claims obligatorios: sub, exp, iat, tenant_id, roles</li>
     * </ul>
     * </p>
     *
     * @param token el token JWT a validar
     * @return true si el token es válido, false en caso contrario
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            // Parsear y validar el token (lanza excepción si es inválido)
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(60) // Tolerancia de 60 segundos
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            // Validar presencia de claims obligatorios
            if (claims.getSubject() == null || 
                claims.getExpiration() == null || 
                claims.getIssuedAt() == null ||
                !claims.containsKey("roles")) {
                log.warn("Token inválido: faltan claims obligatorios");
                return false;
            }
            
            log.debug("Token JWT validado exitosamente para usuario: {}", claims.getSubject());
            return true;
            
        } catch (SignatureException e) {
            log.error("Firma JWT inválida: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Token JWT malformado: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Token JWT no soportado: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Claims JWT vacíos: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Extrae todos los claims de un token JWT.
     * <p>
     * Este método asume que el token ya ha sido validado previamente.
     * Si el token es inválido, retornará null.
     * </p>
     *
     * @param token el token JWT del cual extraer los claims
     * @return objeto Claims con todos los datos del token, o null si es inválido
     */
    public Claims extractClaims(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            return Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload();
                
        } catch (Exception e) {
            log.error("Error al extraer claims del token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrae el user_id del token JWT.
     * <p>
     * El user_id está almacenado en el claim 'sub' (subject) como UUID en formato string.
     * </p>
     *
     * @param token el token JWT
     * @return el UUID del usuario, o null si el token es inválido o no contiene el claim
     */
    public UUID extractUserId(String token) {
        try {
            Claims claims = extractClaims(token);
            if (claims == null || claims.getSubject() == null) {
                return null;
            }
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            log.error("Error al parsear user_id del token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrae el tenant_id del token JWT.
     * <p>
     * El tenant_id puede ser null para usuarios SaaS (super_admin, support).
     * Para usuarios de negocio, siempre debe estar presente.
     * </p>
     *
     * @param token el token JWT
     * @return el UUID del tenant, o null si es usuario SaaS o token inválido
     */
    public UUID extractTenantId(String token) {
        try {
            Claims claims = extractClaims(token);
            if (claims == null) {
                return null;
            }
            
            String tenantIdStr = claims.get("tenant_id", String.class);
            if (tenantIdStr == null || tenantIdStr.isEmpty()) {
                return null; // Usuario SaaS
            }
            
            return UUID.fromString(tenantIdStr);
        } catch (IllegalArgumentException e) {
            log.error("Error al parsear tenant_id del token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrae los roles del token JWT.
     * <p>
     * Los roles están almacenados como un array de strings en el claim 'roles'.
     * En la implementación actual, cada usuario tiene exactamente un rol.
     * </p>
     *
     * @param token el token JWT
     * @return lista de roles del usuario, o lista vacía si el token es inválido
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Claims claims = extractClaims(token);
            if (claims == null) {
                return List.of();
            }
            
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List) {
                return (List<String>) rolesObj;
            }
            
            log.warn("El claim 'roles' no es una lista válida");
            return List.of();
            
        } catch (Exception e) {
            log.error("Error al extraer roles del token: {}", e.getMessage());
            return List.of();
        }
    }
}

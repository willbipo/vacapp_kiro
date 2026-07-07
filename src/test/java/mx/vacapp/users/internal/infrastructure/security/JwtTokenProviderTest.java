package mx.vacapp.users.internal.infrastructure.security;

import io.jsonwebtoken.Claims;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para JwtTokenProvider.
 * <p>
 * Verifica:
 * - Generación de tokens JWT válidos
 * - Validación de tokens
 * - Extracción de claims (userId, tenantId, roles)
 * - Manejo de tokens expirados e inválidos
 * </p>
 */
class JwtTokenProviderTest {
    
    private JwtTokenProvider jwtTokenProvider;
    
    private static final String TEST_SECRET = "test-secret-key-at-least-256-bits-32-bytes-for-hmac-sha256-algorithm";
    private static final long TEST_EXPIRATION_MS = 3600000L; // 1 hora
    private static final String TEST_ISSUER = "vacapp-test";
    
    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        
        // Configurar propiedades usando reflection (simula @Value)
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", TEST_EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtIssuer", TEST_ISSUER);
    }
    
    @Test
    void generateToken_debeGenerarTokenValidoParaUsuarioDeNegocio() {
        // Given: un usuario de negocio con tenant_id
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        User user = User.create(
            "test@example.com",
            "Test User",
            "1234567890",
            "hashedPassword",
            Role.ADMIN,
            tenantId,
            createdBy
        );
        
        // When: se genera un token
        String token = jwtTokenProvider.generateToken(user);
        
        // Then: el token no debe ser nulo ni vacío
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Y debe tener la estructura JWT (header.payload.signature)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "Token JWT debe tener 3 partes");
    }
    
    @Test
    void generateToken_debeGenerarTokenValidoParaUsuarioSaaS() {
        // Given: un usuario SaaS sin tenant_id
        UUID userId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        User user = User.create(
            "admin@vacapp.com",
            "Super Admin",
            "9876543210",
            "hashedPassword",
            Role.SUPER_ADMIN,
            null, // Usuario SaaS
            createdBy
        );
        
        // When: se genera un token
        String token = jwtTokenProvider.generateToken(user);
        
        // Then: el token debe ser válido
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }
    
    @Test
    void validateToken_debeRetornarTrueParaTokenValido() {
        // Given: un token válido
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        User user = User.create(
            "test@example.com",
            "Test User",
            "1234567890",
            "hashedPassword",
            Role.MANAGER,
            tenantId,
            createdBy
        );
        
        String token = jwtTokenProvider.generateToken(user);
        
        // When: se valida el token
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // Then: debe ser válido
        assertTrue(isValid);
    }
    
    @Test
    void validateToken_debeRetornarFalseParaTokenMalformado() {
        // Given: un token malformado
        String invalidToken = "esto.no.es.un.token.jwt";
        
        // When: se valida el token
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);
        
        // Then: debe ser inválido
        assertFalse(isValid);
    }
    
    @Test
    void validateToken_debeRetornarFalseParaTokenVacio() {
        // Given: un token vacío
        String emptyToken = "";
        
        // When: se valida el token
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);
        
        // Then: debe ser inválido
        assertFalse(isValid);
    }
    
    @Test
    void extractClaims_debeExtraerClaimsCorrectamente() {
        // Given: un token válido
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        User user = User.create(
            "test@example.com",
            "Test User",
            "1234567890",
            "hashedPassword",
            Role.VETERINARIAN,
            tenantId,
            createdBy
        );
        
        String token = jwtTokenProvider.generateToken(user);
        
        // When: se extraen los claims
        Claims claims = jwtTokenProvider.extractClaims(token);
        
        // Then: los claims deben coincidir con los datos del usuario
        assertNotNull(claims);
        assertEquals(user.getUserId().toString(), claims.getSubject());
        assertEquals("test@example.com", claims.get("email", String.class));
        assertEquals(tenantId.toString(), claims.get("tenant_id", String.class));
        
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("veterinarian", roles.get(0));
    }
    
    @Test
    void extractUserId_debeExtraerUserIdCorrectamente() {
        // Given: un token válido
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        User user = User.create(
            "test@example.com",
            "Test User",
            "1234567890",
            "hashedPassword",
            Role.WORKER,
            tenantId,
            createdBy
        );
        
        String token = jwtTokenProvider.generateToken(user);
        
        // When: se extrae el userId
        UUID extractedUserId = jwtTokenProvider.extractUserId(token);
        
        // Then: debe coincidir con el userId del usuario
        assertNotNull(extractedUserId);
        assertEquals(user.getUserId(), extractedUserId);
    }
    
    @Test
    void extractTenantId_debeExtraerTenantIdCorrectamente() {
        // Given: un token válido de usuario de negocio
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        User user = User.create(
            "test@example.com",
            "Test User",
            "1234567890",
            "hashedPassword",
            Role.ADMIN,
            tenantId,
            createdBy
        );
        
        String token = jwtTokenProvider.generateToken(user);
        
        // When: se extrae el tenantId
        UUID extractedTenantId = jwtTokenProvider.extractTenantId(token);
        
        // Then: debe coincidir con el tenantId original
        assertNotNull(extractedTenantId);
        assertEquals(tenantId, extractedTenantId);
    }
    
    @Test
    void extractTenantId_debeRetornarNullParaUsuarioSaaS() {
        // Given: un token válido de usuario SaaS (sin tenant_id)
        UUID userId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        User user = User.create(
            "support@vacapp.com",
            "Support User",
            "1234567890",
            "hashedPassword",
            Role.SUPPORT,
            null, // Usuario SaaS
            createdBy
        );
        
        String token = jwtTokenProvider.generateToken(user);
        
        // When: se extrae el tenantId
        UUID extractedTenantId = jwtTokenProvider.extractTenantId(token);
        
        // Then: debe ser null
        assertNull(extractedTenantId);
    }
    
    @Test
    void extractRoles_debeExtraerRolesCorrectamente() {
        // Given: un token válido
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        User user = User.create(
            "test@example.com",
            "Test User",
            "1234567890",
            "hashedPassword",
            Role.MANAGER,
            tenantId,
            createdBy
        );
        
        String token = jwtTokenProvider.generateToken(user);
        
        // When: se extraen los roles
        List<String> roles = jwtTokenProvider.extractRoles(token);
        
        // Then: debe contener el rol del usuario
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("manager", roles.get(0));
    }
    
    @Test
    void extractUserId_debeRetornarNullParaTokenInvalido() {
        // Given: un token inválido
        String invalidToken = "token.invalido.xyz";
        
        // When: se intenta extraer el userId
        UUID extractedUserId = jwtTokenProvider.extractUserId(invalidToken);
        
        // Then: debe retornar null
        assertNull(extractedUserId);
    }
    
    @Test
    void extractRoles_debeRetornarListaVaciaParaTokenInvalido() {
        // Given: un token inválido
        String invalidToken = "token.invalido.xyz";
        
        // When: se intentan extraer los roles
        List<String> roles = jwtTokenProvider.extractRoles(invalidToken);
        
        // Then: debe retornar lista vacía
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }
}

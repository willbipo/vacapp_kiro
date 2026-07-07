package mx.vacapp.users.internal.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para TenantContext.
 * <p>
 * Verifica el correcto funcionamiento del almacenamiento ThreadLocal
 * del tenant_id para garantizar aislamiento multi-tenant.
 * </p>
 */
class TenantContextTest {
    
    /**
     * Limpia el contexto después de cada test para evitar
     * contaminación entre tests.
     */
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }
    
    /**
     * Test: setTenantId y getTenantId funcionan correctamente.
     * <p>
     * Verifica que el tenant_id almacenado puede ser recuperado
     * correctamente en el mismo thread.
     * </p>
     */
    @Test
    void setTenantId_shouldStoreAndRetrieveTenantId() {
        // Given: Un tenant_id válido
        UUID tenantId = UUID.randomUUID();
        
        // When: Se establece el tenant_id en el contexto
        TenantContext.setTenantId(tenantId);
        
        // Then: Se puede recuperar el mismo tenant_id
        assertEquals(tenantId, TenantContext.getTenantId());
        assertTrue(TenantContext.hasTenantId());
    }
    
    /**
     * Test: setTenantId con null para usuarios SaaS.
     * <p>
     * Verifica que se puede establecer tenant_id como null
     * para usuarios con roles SaaS (super_admin, support).
     * </p>
     */
    @Test
    void setTenantId_shouldAllowNullForSaaSUsers() {
        // Given: Un tenant_id null (usuario SaaS)
        UUID tenantId = null;
        
        // When: Se establece tenant_id null en el contexto
        TenantContext.setTenantId(tenantId);
        
        // Then: getTenantId retorna null
        assertNull(TenantContext.getTenantId());
        assertFalse(TenantContext.hasTenantId());
    }
    
    /**
     * Test: clear limpia correctamente el contexto.
     * <p>
     * Verifica que después de limpiar el contexto, getTenantId
     * retorna null, previniendo memory leaks.
     * </p>
     */
    @Test
    void clear_shouldRemoveTenantIdFromContext() {
        // Given: Un contexto con tenant_id establecido
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        
        // When: Se limpia el contexto
        TenantContext.clear();
        
        // Then: getTenantId retorna null
        assertNull(TenantContext.getTenantId());
        assertFalse(TenantContext.hasTenantId());
    }
    
    /**
     * Test: hasTenantId retorna false cuando no hay contexto.
     * <p>
     * Verifica el estado inicial sin tenant_id establecido.
     * </p>
     */
    @Test
    void hasTenantId_shouldReturnFalseWhenNoTenantIdSet() {
        // Given: Contexto sin tenant_id establecido
        // (después del @AfterEach clear())
        
        // When/Then: hasTenantId retorna false
        assertFalse(TenantContext.hasTenantId());
        assertNull(TenantContext.getTenantId());
    }
    
    /**
     * Test: ThreadLocal aislamiento entre threads.
     * <p>
     * Verifica que cada thread tiene su propio tenant_id aislado,
     * garantizando seguridad multi-tenant en entornos concurrentes.
     * </p>
     */
    @Test
    void threadLocal_shouldIsolateContextBetweenThreads() throws InterruptedException {
        // Given: Dos tenant_ids diferentes
        UUID tenantId1 = UUID.randomUUID();
        UUID tenantId2 = UUID.randomUUID();
        
        // When: Se establecen en threads diferentes
        Thread thread1 = new Thread(() -> {
            TenantContext.setTenantId(tenantId1);
            assertEquals(tenantId1, TenantContext.getTenantId());
            TenantContext.clear();
        });
        
        Thread thread2 = new Thread(() -> {
            TenantContext.setTenantId(tenantId2);
            assertEquals(tenantId2, TenantContext.getTenantId());
            TenantContext.clear();
        });
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        // Then: El contexto principal no debe verse afectado
        assertNull(TenantContext.getTenantId());
    }
    
    /**
     * Test: Constructor privado no debe permitir instanciación.
     * <p>
     * Verifica que TenantContext es una clase de utilidad
     * que no puede ser instanciada.
     * </p>
     */
    @Test
    void constructor_shouldThrowExceptionWhenInstantiated() {
        // When/Then: Intentar instanciar lanza InvocationTargetException
        // que envuelve la UnsupportedOperationException del constructor
        var exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            // Usar reflexión para acceder al constructor privado
            var constructor = TenantContext.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        
        // Verificar que la causa es UnsupportedOperationException
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("TenantContext es una clase de utilidad y no debe ser instanciada", 
            exception.getCause().getMessage());
    }
    
    /**
     * Test: Actualización de tenant_id en el mismo thread.
     * <p>
     * Verifica que se puede actualizar el tenant_id múltiples veces
     * en el mismo thread (ej. para cambiar de contexto en tests).
     * </p>
     */
    @Test
    void setTenantId_shouldAllowUpdatingTenantIdInSameThread() {
        // Given: Un primer tenant_id
        UUID tenantId1 = UUID.randomUUID();
        TenantContext.setTenantId(tenantId1);
        assertEquals(tenantId1, TenantContext.getTenantId());
        
        // When: Se actualiza con un segundo tenant_id
        UUID tenantId2 = UUID.randomUUID();
        TenantContext.setTenantId(tenantId2);
        
        // Then: El nuevo tenant_id reemplaza al anterior
        assertEquals(tenantId2, TenantContext.getTenantId());
        assertNotEquals(tenantId1, TenantContext.getTenantId());
    }
}

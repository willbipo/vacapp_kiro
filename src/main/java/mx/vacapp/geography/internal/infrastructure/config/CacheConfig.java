package mx.vacapp.geography.internal.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuración de caching con Caffeine para el módulo Geography.
 * 
 * Configura:
 * - TTL de 5 minutos para estadísticas de rancho
 * - Tamaño máximo de 1000 entradas
 * - Key template: stats:rancho:{id}:tenant:{tenantId} para aislamiento multi-tenant
 * 
 * El cache se invalida automáticamente al crear, actualizar o archivar
 * secciones y potreros mediante @CacheEvict.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Configura el cache manager con Caffeine.
     * 
     * @return CacheManager configurado con políticas de expiración y tamaño
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)  // TTL de 5 minutos
            .maximumSize(1000)                      // Máximo 1000 entradas
            .recordStats());                        // Habilitar estadísticas de cache
        
        // Registrar nombres de cachés
        cacheManager.setCacheNames(java.util.List.of(CacheNames.RANCHO_STATS));
        
        return cacheManager;
    }
}

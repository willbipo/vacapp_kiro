package mx.vacapp.cattle.internal.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import mx.vacapp.cattle.internal.infrastructure.config.CattleProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuración de caché para el módulo cattle-inventory usando Caffeine.
 * La configuración base está en application.yml, pero aquí se puede personalizar si es necesario.
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {
    
    private final CattleProperties cattleProperties;
    
    /**
     * Configura el gestor de caché Caffeine para el módulo cattle.
     * La configuración se lee desde application.yml (vacapp.cattle.cache).
     * 
     * @return CacheManager configurado con Caffeine
     */
    @Bean
    public CacheManager cattleCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            CacheNames.CATTLE_STATS,
            CacheNames.ANIMAL_STATS,
            CacheNames.POTRERO_VALIDATION
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(cattleProperties.getCache().getStatsMaxSize())
            .expireAfterWrite(cattleProperties.getCache().getStatsTtlMinutes(), TimeUnit.MINUTES)
            .recordStats()
        );
        
        return cacheManager;
    }
}

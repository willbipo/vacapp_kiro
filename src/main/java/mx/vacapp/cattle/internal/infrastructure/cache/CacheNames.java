package mx.vacapp.cattle.internal.infrastructure.cache;

/**
 * Constantes de nombres de caché para el módulo cattle-inventory.
 * Estos nombres deben coincidir con los configurados en application.yml.
 */
public final class CacheNames {
    
    /**
     * Caché para estadísticas de ganado (GET /api/v1/cattle/stats).
     * TTL: 15 minutos (configurado en application.yml)
     */
    public static final String CATTLE_STATS = "cattleStats";
    
    /**
     * Caché para estadísticas de animales individuales.
     * TTL: 15 minutos (configurado en application.yml)
     */
    public static final String ANIMAL_STATS = "animalStats";
    
    /**
     * Caché para validación de potreros activos (integración con geography module).
     * TTL: 5 minutos (configurado en CacheConfig)
     */
    public static final String POTRERO_VALIDATION = "potreroValidation";
    
    private CacheNames() {
        // Clase de constantes, no debe instanciarse
        throw new UnsupportedOperationException("Cannot instantiate constants class");
    }
}

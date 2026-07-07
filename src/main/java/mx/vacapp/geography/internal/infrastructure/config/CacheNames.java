package mx.vacapp.geography.internal.infrastructure.config;

/**
 * Constantes para nombres de cachés del módulo Geography.
 * 
 * Centraliza los nombres de cachés para evitar strings mágicos.
 */
public final class CacheNames {
    
    /**
     * Cache para estadísticas de rancho.
     * Key pattern: stats:rancho:{id}:tenant:{tenantId}
     * TTL: 5 minutos
     */
    public static final String RANCHO_STATS = "rancho-stats";
    
    // Constructor privado para evitar instanciación
    private CacheNames() {
        throw new UnsupportedOperationException("Esta es una clase de utilidades y no puede ser instanciada");
    }
}

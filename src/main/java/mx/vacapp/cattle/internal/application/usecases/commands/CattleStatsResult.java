package mx.vacapp.cattle.internal.application.usecases.commands;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resultado inmutable que representa estadísticas agregadas del inventario de ganado.
 * <p>
 * Record utilizado para retornar métricas e indicadores clave del inventario desde casos de uso
 * hacia controladores. Contiene estadísticas globales y distribuciones por categorías.
 * </p>
 * 
 * <h2>Estadísticas Globales:</h2>
 * <ul>
 *   <li>{@code totalAnimales} - Total de animales en el inventario (todos los status)</li>
 *   <li>{@code totalActivos} - Total de animales activos (status IN: Activa, Preñada, En Reposo)</li>
 *   <li>{@code totalMachos} - Total de animales con sexo = Macho</li>
 *   <li>{@code totalHembras} - Total de animales con sexo = Hembra</li>
 *   <li>{@code promedioEdad} - Edad promedio en meses de animales activos</li>
 *   <li>{@code promedioPeso} - Peso promedio en kg de animales activos (últimos 30 días)</li>
 * </ul>
 * 
 * <h2>Distribuciones por Categorías:</h2>
 * <ul>
 *   <li>{@code distribucionPorRaza} - Map de raza → cantidad de animales</li>
 *   <li>{@code distribucionPorTipo} - Map de tipo comercial → cantidad de animales</li>
 *   <li>{@code distribucionPorStatus} - Map de status → cantidad de animales</li>
 *   <li>{@code distribucionPorPotrero} - Map de potrero_id → cantidad de animales actuales</li>
 * </ul>
 * 
 * <h2>Cálculos:</h2>
 * <p>
 * <b>Total Activos:</b> COUNT(animales) WHERE status IN ('activa', 'prenada', 'en_reposo')
 * </p>
 * <p>
 * <b>Promedio Edad:</b> AVG(meses) WHERE status IN ('activa', 'prenada', 'en_reposo')
 * </p>
 * <p>
 * <b>Promedio Peso:</b> AVG(peso_kg) FROM cattle_weights 
 * WHERE animal_id IN (animales activos) AND fecha_pesaje >= CURRENT_DATE - INTERVAL 30 DAY
 * </p>
 * <p>
 * <b>Distribución por Potrero:</b> Solo cuenta animales con ubicación actual (pasture_history.fecha_salida IS NULL)
 * </p>
 * 
 * <h2>Caché:</h2>
 * <p>
 * Las estadísticas se cachean durante 15 minutos para optimizar rendimiento.
 * Cache key: "stats:cattle:tenant:{tenantId}:rancho:{ranchoId}"
 * </p>
 * 
 * @param totalAnimales Total de animales en el inventario
 * @param totalActivos Total de animales activos (Activa, Preñada, En Reposo)
 * @param totalMachos Total de animales macho
 * @param totalHembras Total de animales hembra
 * @param distribucionPorRaza Map de raza → cantidad (ej: "charolais" → 45)
 * @param distribucionPorTipo Map de tipo → cantidad (ej: "venta" → 120)
 * @param distribucionPorStatus Map de status → cantidad (ej: "activa" → 200)
 * @param distribucionPorPotrero Map de potrero_id → cantidad de animales actuales
 * @param promedioEdad Edad promedio en meses de animales activos (puede ser null)
 * @param promedioPeso Peso promedio en kg de animales activos últimos 30 días (puede ser null)
 * 
 * @see mx.vacapp.cattle.internal.application.usecases.animal.GetAnimalStatsUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record CattleStatsResult(
    Integer totalAnimales,
    Integer totalActivos,
    Integer totalMachos,
    Integer totalHembras,
    Map<String, Integer> distribucionPorRaza,
    Map<String, Integer> distribucionPorTipo,
    Map<String, Integer> distribucionPorStatus,
    Map<String, Integer> distribucionPorPotrero,
    BigDecimal promedioEdad,
    BigDecimal promedioPeso
) {
    /**
     * Crea un resultado de estadísticas con valores por defecto para cuando no hay datos.
     * 
     * @return CattleStatsResult con todos los totales en 0 y mapas vacíos
     */
    public static CattleStatsResult empty() {
        return new CattleStatsResult(
            0,
            0,
            0,
            0,
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            null,
            null
        );
    }
    
    /**
     * Verifica si hay animales en el inventario.
     * 
     * @return true si totalAnimales > 0
     */
    public boolean hasAnimals() {
        return totalAnimales != null && totalAnimales > 0;
    }
    
    /**
     * Verifica si hay animales activos en el inventario.
     * 
     * @return true si totalActivos > 0
     */
    public boolean hasActiveAnimals() {
        return totalActivos != null && totalActivos > 0;
    }
}

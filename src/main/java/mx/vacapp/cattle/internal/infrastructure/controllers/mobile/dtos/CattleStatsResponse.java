package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import mx.vacapp.cattle.internal.application.usecases.commands.CattleStatsResult;

import java.util.Map;

/**
 * DTO de salida (Response) para representar estadísticas agregadas del inventario de ganado.
 * <p>
 * Este Record representa los datos que se retornan desde la API REST móvil cuando se consultan
 * estadísticas globales del inventario de ganado bovino. Incluye totales generales, distribuciones
 * por categorías y métricas agregadas.
 * </p>
 * 
 * <h2>Campos Incluidos:</h2>
 * <ul>
 *   <li><strong>totalAnimales</strong>: Total de animales en el inventario (todos los status)</li>
 *   <li><strong>totalActivos</strong>: Total de animales activos (status: Activa, Preñada, En Reposo)</li>
 *   <li><strong>totalVendidos</strong>: Total de animales con status Vendida</li>
 *   <li><strong>totalMuertos</strong>: Total de animales con status Muerta</li>
 *   <li><strong>totalMachos</strong>: Total de animales con sexo Macho</li>
 *   <li><strong>totalHembras</strong>: Total de animales con sexo Hembra</li>
 *   <li><strong>porTipo</strong>: Distribución de animales por tipo comercial (Map: tipo → cantidad)</li>
 *   <li><strong>porRaza</strong>: Distribución de animales por raza (Map: raza → cantidad)</li>
 * </ul>
 * 
 * <h2>Flujo de Uso:</h2>
 * <pre>
 * 1. UseCase retorna CattleStatsResult
 * 2. Controller convierte a CattleStatsResponse usando fromResult()
 * 3. Spring serializa a JSON y retorna al cliente móvil
 * </pre>
 * 
 * <h2>Ejemplo JSON:</h2>
 * <pre>
 * {
 *   "totalAnimales": 250,
 *   "totalActivos": 200,
 *   "totalVendidos": 30,
 *   "totalMuertos": 20,
 *   "totalMachos": 100,
 *   "totalHembras": 150,
 *   "porTipo": {
 *     "venta": 120,
 *     "vientre": 80,
 *     "engorda": 50
 *   },
 *   "porRaza": {
 *     "angus": 90,
 *     "charolais": 75,
 *     "brahman": 85
 *   }
 * }
 * </pre>
 * 
 * @param totalAnimales Total de animales en el inventario
 * @param totalActivos Total de animales activos (Activa, Preñada, En Reposo)
 * @param totalVendidos Total de animales vendidos
 * @param totalMuertos Total de animales muertos
 * @param totalMachos Total de animales macho
 * @param totalHembras Total de animales hembra
 * @param porTipo Map de tipo comercial → cantidad de animales
 * @param porRaza Map de raza → cantidad de animales
 * 
 * @see CattleStatsResult
 * @see mx.vacapp.cattle.internal.application.usecases.animal.GetAnimalStatsUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record CattleStatsResponse(
    Integer totalAnimales,
    Integer totalActivos,
    Integer totalVendidos,
    Integer totalMuertos,
    Integer totalMachos,
    Integer totalHembras,
    Map<String, Integer> porTipo,
    Map<String, Integer> porRaza
) {
    
    /**
     * Método factory para crear un CattleStatsResponse desde un CattleStatsResult de la capa de aplicación.
     * <p>
     * Este método transforma el resultado de un caso de uso (CattleStatsResult) en un DTO de respuesta
     * de infraestructura (CattleStatsResponse) listo para ser serializado a JSON y retornado al cliente móvil.
     * </p>
     * 
     * <h3>Conversión de Campos:</h3>
     * <ul>
     *   <li>Los totales generales se mapean directamente (totalAnimales, totalActivos, totalMachos, totalHembras)</li>
     *   <li>totalVendidos se extrae de distribucionPorStatus con key "vendida" (default 0)</li>
     *   <li>totalMuertos se extrae de distribucionPorStatus con key "muerta" (default 0)</li>
     *   <li>porTipo se mapea directamente desde distribucionPorTipo</li>
     *   <li>porRaza se mapea directamente desde distribucionPorRaza</li>
     *   <li>Los campos promedio de edad y peso se excluyen en esta versión de la respuesta</li>
     * </ul>
     * 
     * <h3>Uso:</h3>
     * <pre>
     * CattleStatsResult result = getAnimalStatsUseCase.execute();
     * CattleStatsResponse response = CattleStatsResponse.fromResult(result);
     * return ResponseEntity.ok(response);
     * </pre>
     * 
     * @param result CattleStatsResult retornado desde la capa de aplicación
     * @return CattleStatsResponse listo para ser serializado a JSON
     * 
     * @see CattleStatsResult
     * @see mx.vacapp.cattle.internal.application.usecases.animal.GetAnimalStatsUseCase
     */
    public static CattleStatsResponse fromResult(CattleStatsResult result) {
        // Extraer totalVendidos y totalMuertos desde distribucionPorStatus
        Integer totalVendidos = result.distribucionPorStatus().getOrDefault("vendida", 0);
        Integer totalMuertos = result.distribucionPorStatus().getOrDefault("muerta", 0);
        
        return new CattleStatsResponse(
            result.totalAnimales(),
            result.totalActivos(),
            totalVendidos,
            totalMuertos,
            result.totalMachos(),
            result.totalHembras(),
            result.distribucionPorTipo(),
            result.distribucionPorRaza()
        );
    }
}

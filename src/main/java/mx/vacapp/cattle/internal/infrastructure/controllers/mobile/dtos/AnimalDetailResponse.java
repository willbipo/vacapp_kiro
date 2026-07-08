package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta (Response) extendido con genealogía completa para vista detallada de animal.
 * <p>
 * Este Record representa la respuesta completa para el endpoint GET /api/v1/cattle/{id}
 * que incluye toda la información básica del animal más datos enriquecidos de genealogía
 * (detalles de madre y padre) y contadores de relaciones (hijos).
 * </p>
 * 
 * <h2>Campos Incluidos:</h2>
 * <ul>
 *   <li><strong>Identificación</strong>: animalId, arete, areteAnterior</li>
 *   <li><strong>Información Biológica</strong>: sexo, raza, fechaNacimiento, fechaAretado</li>
 *   <li><strong>Clasificación</strong>: tipo, status</li>
 *   <li><strong>Regulatorio</strong>: folioReemo</li>
 *   <li><strong>Observaciones</strong>: observaciones (nota)</li>
 *   <li><strong>Genealogía Completa</strong>: madreDetails, padreDetails, hijosCount</li>
 *   <li><strong>Auditoría</strong>: createdAt, updatedAt</li>
 * </ul>
 * 
 * <h2>Diferencia con AnimalResponse:</h2>
 * <p>
 * AnimalResponse (listado) incluye solo IDs de padres y conteo básico.
 * AnimalDetailResponse incluye objetos completos ParentSummary con detalles de padres.
 * </p>
 * 
 * <h2>Ejemplo JSON de Respuesta:</h2>
 * <pre>
 * {
 *   "animalId": "550e8400-e29b-41d4-a716-446655440000",
 *   "arete": "MX12345",
 *   "areteAnterior": "ABC123",
 *   "sexo": "hembra",
 *   "raza": "angus",
 *   "fechaNacimiento": "2023-03-15",
 *   "fechaAretado": "2023-03-20",
 *   "tipo": "vientre",
 *   "status": "activa",
 *   "folioReemo": "REEMO-2024-001",
 *   "observaciones": "Animal de buena genética",
 *   "madreDetails": {
 *     "animalId": "550e8400-e29b-41d4-a716-446655440001",
 *     "arete": "MX11111",
 *     "sexo": "hembra",
 *     "raza": "angus",
 *     "edadMeses": 48
 *   },
 *   "padreDetails": {
 *     "animalId": "550e8400-e29b-41d4-a716-446655440002",
 *     "arete": "MX22222",
 *     "sexo": "macho",
 *     "raza": "charolais",
 *     "edadMeses": 60
 *   },
 *   "hijosCount": 3,
 *   "createdAt": "2024-01-15T10:30:00",
 *   "updatedAt": "2024-01-20T14:45:00"
 * }
 * </pre>
 * 
 * @param animalId UUID único del animal
 * @param arete Identificador único del animal (global)
 * @param areteAnterior Arete previo del animal (opcional)
 * @param sexo Sexo del animal (macho/hembra)
 * @param raza Raza del animal
 * @param fechaNacimiento Fecha de nacimiento del animal
 * @param fechaAretado Fecha en que se colocó el arete (opcional)
 * @param tipo Tipo comercial del animal
 * @param status Estado actual del animal
 * @param folioReemo Folio REEMO para regulación mexicana (opcional)
 * @param observaciones Observaciones libres sobre el animal (opcional)
 * @param madreDetails Detalles resumidos de la madre (opcional)
 * @param padreDetails Detalles resumidos del padre (opcional)
 * @param hijosCount Cantidad de hijos registrados en el sistema
 * @param createdAt Fecha y hora de creación del registro
 * @param updatedAt Fecha y hora de última actualización del registro
 * 
 * @see AnimalResult
 * @see ParentSummary
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record AnimalDetailResponse(
    UUID animalId,
    String arete,
    String areteAnterior,
    String sexo,
    String raza,
    LocalDate fechaNacimiento,
    LocalDate fechaAretado,
    String tipo,
    String status,
    String folioReemo,
    String observaciones,
    ParentSummary madreDetails,
    ParentSummary padreDetails,
    Integer hijosCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    /**
     * Record interno que representa un resumen de información de un animal padre/madre.
     * <p>
     * Contiene únicamente los campos esenciales para mostrar en la vista de genealogía
     * sin exponer toda la información completa del animal relacionado.
     * </p>
     * 
     * @param animalId UUID del animal padre/madre
     * @param arete Identificador del animal padre/madre
     * @param sexo Sexo del animal padre/madre
     * @param raza Raza del animal padre/madre
     * @param edadMeses Edad actual en meses del animal padre/madre
     */
    public record ParentSummary(
        UUID animalId,
        String arete,
        String sexo,
        String raza,
        Integer edadMeses
    ) {
        /**
         * Crea un ParentSummary desde los datos básicos de un animal.
         * 
         * @param animalId UUID del animal
         * @param arete Arete del animal
         * @param sexo Sexo del animal
         * @param raza Raza del animal
         * @param edadMeses Edad en meses
         * @return ParentSummary con los datos proporcionados
         */
        public static ParentSummary of(UUID animalId, String arete, String sexo, 
                                       String raza, Integer edadMeses) {
            return new ParentSummary(animalId, arete, sexo, raza, edadMeses);
        }
    }
    
    /**
     * Método factory para crear un AnimalDetailResponse desde un AnimalResult.
     * <p>
     * Convierte el resultado del caso de uso de aplicación en un DTO de respuesta
     * listo para ser serializado a JSON y enviado al cliente móvil.
     * </p>
     * 
     * <h3>Conversiones Aplicadas:</h3>
     * <ul>
     *   <li>Instant (createdAt/updatedAt) → LocalDateTime para formato JSON más legible</li>
     *   <li>nota → observaciones (renombrado para API pública)</li>
     *   <li>Los campos de genealogía (madreDetails, padreDetails, hijosCount) deben 
     *       ser calculados previamente en el caso de uso y pasados como parámetros adicionales</li>
     * </ul>
     * 
     * <h3>Nota Importante:</h3>
     * <p>
     * Este método asume que AnimalResult tiene los datos básicos del animal.
     * Los datos de genealogía (madreDetails, padreDetails, hijosCount) deben ser
     * obtenidos separadamente en el caso de uso GetAnimalUseCase mediante consultas
     * adicionales a los repositorios y pasados como parámetros.
     * </p>
     * 
     * <h3>Uso en Controller:</h3>
     * <pre>
     * AnimalResult result = getAnimalUseCase.execute(animalId);
     * ParentSummary madre = obtenerMadreDetails(result.madreId());
     * ParentSummary padre = obtenerPadreDetails(result.padreId());
     * Integer hijos = contarHijos(result.animalId());
     * 
     * AnimalDetailResponse response = AnimalDetailResponse.fromResult(
     *     result, madre, padre, hijos
     * );
     * </pre>
     * 
     * @param result el AnimalResult desde la capa de aplicación
     * @param madreDetails resumen de la madre (null si no tiene madre)
     * @param padreDetails resumen del padre (null si no tiene padre)
     * @param hijosCount cantidad de hijos del animal
     * @return AnimalDetailResponse listo para serialización JSON
     * 
     * @see AnimalResult
     */
    public static AnimalDetailResponse fromResult(AnimalResult result, 
                                                   ParentSummary madreDetails,
                                                   ParentSummary padreDetails,
                                                   Integer hijosCount) {
        return new AnimalDetailResponse(
            result.animalId(),
            result.arete(),
            result.areteAnterior(),
            result.sexo(),
            result.raza(),
            result.fechaNacimiento(),
            result.fechaAretado(),
            result.tipo(),
            result.status(),
            result.folioReemo(),
            result.nota(),  // Nota interna se mapea a "observaciones" en la API
            madreDetails,
            padreDetails,
            hijosCount,
            result.createdAt() != null ? 
                LocalDateTime.ofInstant(result.createdAt(), java.time.ZoneOffset.UTC) : null,
            result.updatedAt() != null ? 
                LocalDateTime.ofInstant(result.updatedAt(), java.time.ZoneOffset.UTC) : null
        );
    }
    
    /**
     * Método factory sobrecargado para crear AnimalDetailResponse desde AnimalResult
     * sin datos de genealogía enriquecidos.
     * <p>
     * Útil cuando se quiere retornar la respuesta detallada pero sin consultar
     * los datos adicionales de padres e hijos (por ejemplo, en operaciones de
     * creación donde el animal recién se registró).
     * </p>
     * 
     * @param result el AnimalResult desde la capa de aplicación
     * @return AnimalDetailResponse con campos de genealogía en null/0
     */
    public static AnimalDetailResponse fromResult(AnimalResult result) {
        return fromResult(result, null, null, 0);
    }
}

package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import jakarta.validation.constraints.NotNull;
import mx.vacapp.cattle.internal.application.usecases.commands.MoveAnimalCommand;

import java.util.UUID;

/**
 * DTO de entrada (Request) para mover un animal bovino entre potreros.
 * <p>
 * Este Record representa los datos que llegan desde la API REST móvil para registrar
 * el movimiento de un animal desde su ubicación actual a un nuevo potrero destino.
 * El animalId se extrae del path parameter de la petición HTTP, no del body.
 * </p>
 * 
 * <h2>Validaciones Aplicadas:</h2>
 * <ul>
 *   <li><strong>potreroDestinoId</strong>: NOT NULL, UUID válido del potrero destino</li>
 * </ul>
 * 
 * <h2>Flujo de Uso:</h2>
 * <pre>
 * 1. Cliente móvil envía POST /api/v1/cattle/{animalId}/move con JSON
 * 2. Spring valida automáticamente con @Valid
 * 3. Controller extrae animalId del path parameter
 * 4. Controller mapea MoveAnimalRequest → MoveAnimalCommand usando toCommand(animalId, movedBy)
 * 5. MoveAnimalUseCase procesa el comando
 * </pre>
 * 
 * <h2>Validaciones de Negocio (en caso de uso):</h2>
 * <ul>
 *   <li>El animal debe existir y pertenecer al tenant del usuario</li>
 *   <li>El animal NO puede tener status Vendida o Muerta</li>
 *   <li>El potrero destino debe existir y estar activo (validado con GeographyService)</li>
 *   <li>El animal solo puede tener una ubicación actual (un registro con fecha_salida = null)</li>
 * </ul>
 * 
 * <h2>Proceso del Movimiento:</h2>
 * <ol>
 *   <li>Actualizar el registro actual en pasture_history con fecha_salida = NOW()</li>
 *   <li>Insertar nuevo registro en pasture_history con fecha_entrada = NOW() y fecha_salida = null</li>
 *   <li>Registrar el movimiento en cattle_audit con operation_type = MOVE_PASTURE</li>
 * </ol>
 * 
 * <h2>Ejemplo JSON:</h2>
 * <pre>
 * POST /api/v1/cattle/550e8400-e29b-41d4-a716-446655440000/move
 * {
 *   "potreroDestinoId": "550e8400-e29b-41d4-a716-446655440002"
 * }
 * </pre>
 * 
 * @param potreroDestinoId UUID del potrero destino al que se moverá el animal
 * 
 * @see MoveAnimalCommand
 * @see mx.vacapp.cattle.internal.application.usecases.movement.MoveAnimalUseCase
 * @see mx.vacapp.cattle.internal.domain.model.PastureHistory
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record MoveAnimalRequest(
    
    @NotNull(message = "El potrero destino es obligatorio")
    UUID potreroDestinoId
    
) {
    
    /**
     * Convierte este DTO de Request a un comando de aplicación.
     * <p>
     * Este método transforma el DTO de infraestructura (MoveAnimalRequest) en un comando
     * de la capa de aplicación (MoveAnimalCommand), añadiendo los campos de contexto
     * que no vienen en el body del request:
     * </p>
     * <ul>
     *   <li><strong>animalId</strong>: Se extrae del path parameter de la URL</li>
     *   <li><strong>movedBy</strong>: Se extrae del usuario autenticado actual (JWT)</li>
     * </ul>
     * 
     * <h3>Uso en Controller:</h3>
     * <pre>
     * {@code @PostMapping("/{animalId}/move")}
     * public ResponseEntity<Void> moveAnimal(
     *     {@code @PathVariable} UUID animalId,
     *     {@code @Valid @RequestBody} MoveAnimalRequest request,
     *     {@code @AuthenticationPrincipal} UserDetails userDetails
     * ) {
     *     UUID movedBy = extractUserIdFromUserDetails(userDetails);
     *     MoveAnimalCommand command = request.toCommand(animalId, movedBy);
     *     moveAnimalUseCase.execute(command);
     *     return ResponseEntity.ok().build();
     * }
     * </pre>
     * 
     * @param animalId UUID del animal que se va a mover (extraído del path parameter)
     * @param movedBy UUID del usuario que realiza el movimiento (extraído del JWT)
     * @return MoveAnimalCommand listo para ser procesado por el caso de uso
     * 
     * @see MoveAnimalCommand
     * @see mx.vacapp.cattle.internal.application.usecases.movement.MoveAnimalUseCase
     */
    public MoveAnimalCommand toCommand(UUID animalId, UUID movedBy) {
        return new MoveAnimalCommand(
            animalId,
            potreroDestinoId,
            movedBy,
            null  // tenantId will be set by the use case from security context
        );
    }
}

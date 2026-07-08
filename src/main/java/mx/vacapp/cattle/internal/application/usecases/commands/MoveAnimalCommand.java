package mx.vacapp.cattle.internal.application.usecases.commands;

import java.util.UUID;

/**
 * Comando que encapsula los datos necesarios para mover un animal de un potrero a otro.
 * <p>
 * Este comando es utilizado como entrada para el caso de uso {@code MoveAnimalUseCase}.
 * Contiene todos los campos requeridos para registrar el movimiento de un bovino
 * entre potreros, incluyendo el historial de ubicaciones.
 * </p>
 * 
 * <h2>Campos Requeridos:</h2>
 * <ul>
 *   <li>{@code animalId} - Identificador del animal que se va a mover</li>
 *   <li>{@code potreroDestinoId} - Identificador del potrero destino</li>
 *   <li>{@code movedBy} - Identificador del usuario que realiza el movimiento (extraído del contexto de seguridad)</li>
 *   <li>{@code tenantId} - Identificador del tenant para filtrado multi-tenant</li>
 * </ul>
 * 
 * <h2>Proceso del Movimiento:</h2>
 * <ol>
 *   <li>Validar que el animal existe y pertenece al tenant</li>
 *   <li>Validar que el animal no está vendido o muerto</li>
 *   <li>Validar que el potrero destino existe y está activo (consulta a geographic-control)</li>
 *   <li>Actualizar el registro actual en pasture_history con fecha_salida = NOW()</li>
 *   <li>Insertar nuevo registro en pasture_history con fecha_entrada = NOW() y fecha_salida = null</li>
 *   <li>Registrar el movimiento en cattle_audit</li>
 * </ol>
 * 
 * <h2>Validaciones:</h2>
 * <p>Las validaciones de negocio se realizan en el caso de uso:</p>
 * <ul>
 *   <li>El animal debe existir y pertenecer al tenant del usuario</li>
 *   <li>El animal NO puede tener status Vendida o Muerta</li>
 *   <li>El potrero destino debe existir y estar activo (validado con GeographyService)</li>
 *   <li>El animal solo puede tener una ubicación actual (un registro con fecha_salida = null)</li>
 * </ul>
 * 
 * <h2>Auditoría:</h2>
 * <p>El movimiento se registra en la tabla cattle_audit con:</p>
 * <ul>
 *   <li>operation_type: MOVE_PASTURE</li>
 *   <li>old_values: potrero_id anterior</li>
 *   <li>new_values: potrero_id nuevo</li>
 *   <li>timestamp: fecha y hora del movimiento</li>
 *   <li>modified_by: usuario que realiza el movimiento</li>
 * </ul>
 * 
 * @param animalId Identificador del animal que se va a mover
 * @param potreroDestinoId Identificador del potrero destino
 * @param movedBy Identificador del usuario que realiza el movimiento
 * @param tenantId Identificador del tenant (multi-tenancy)
 * 
 * @see mx.vacapp.cattle.internal.application.usecases.movement.MoveAnimalUseCase
 * @see mx.vacapp.cattle.internal.domain.model.PastureHistory
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record MoveAnimalCommand(
    UUID animalId,
    UUID potreroDestinoId,
    UUID movedBy,
    UUID tenantId
) {
}

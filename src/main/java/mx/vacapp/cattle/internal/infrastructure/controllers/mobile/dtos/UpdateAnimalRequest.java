package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mx.vacapp.cattle.internal.application.usecases.commands.UpdateAnimalCommand;
import mx.vacapp.cattle.internal.domain.model.CattleType;

import java.util.UUID;

/**
 * DTO de entrada (Request) para actualizar la información de un animal existente.
 * <p>
 * Este Record representa los datos que llegan desde la API REST móvil para actualizar
 * un animal ya registrado en el inventario de ganado. Solo contiene los campos que pueden
 * ser actualizados, todos opcionales. Incluye validaciones Bean Validation para garantizar
 * la integridad de los datos antes de procesarlos en la capa de aplicación.
 * </p>
 * 
 * <h2>Campos Actualizables (Todos Opcionales):</h2>
 * <ul>
 *   <li><strong>areteAnterior</strong>: Arete previo del animal (opcional, max 20 caracteres)</li>
 *   <li><strong>folioReemo</strong>: Folio REEMO (opcional, max 50 caracteres, alfanuméricos y guiones)</li>
 *   <li><strong>tipo</strong>: Tipo comercial del animal (opcional)</li>
 *   <li><strong>observaciones</strong>: Notas libres sobre el animal (opcional, max 2000 caracteres)</li>
 * </ul>
 * 
 * <h2>Validaciones Aplicadas:</h2>
 * <ul>
 *   <li><strong>areteAnterior</strong>: Máximo 20 caracteres</li>
 *   <li><strong>folioReemo</strong>: Máximo 50 caracteres, solo alfanuméricos y guiones</li>
 *   <li><strong>observaciones</strong>: Máximo 2000 caracteres</li>
 * </ul>
 * 
 * <h2>Campos NO Actualizables (requieren comandos dedicados):</h2>
 * <ul>
 *   <li><strong>arete</strong>: Identificador único, inmutable después de la creación</li>
 *   <li><strong>sexo</strong>: Inmutable después de la creación</li>
 *   <li><strong>raza</strong>: Inmutable después de la creación</li>
 *   <li><strong>fechaNacimiento</strong>: Inmutable después de la creación</li>
 *   <li><strong>status</strong>: Requiere comando dedicado (ChangeStatusCommand)</li>
 *   <li><strong>potrero</strong>: Requiere comando dedicado (MoveAnimalCommand)</li>
 * </ul>
 * 
 * <h2>Flujo de Uso:</h2>
 * <pre>
 * 1. Cliente móvil envía PUT /api/v1/cattle/{id} con JSON
 * 2. Spring valida automáticamente con @Valid
 * 3. Controller mapea UpdateAnimalRequest → UpdateAnimalCommand usando toCommand()
 * 4. UpdateAnimalUseCase procesa el comando
 * </pre>
 * 
 * <h2>Ejemplo JSON:</h2>
 * <pre>
 * {
 *   "areteAnterior": "ABC123",
 *   "folioReemo": "REEMO-2024-001",
 *   "tipo": "VIENTRE",
 *   "observaciones": "Animal de buena genética, actualizado en revisión"
 * }
 * </pre>
 * 
 * @param areteAnterior Arete previo del animal (opcional, max 20 caracteres)
 * @param folioReemo Folio REEMO para regulación mexicana (opcional, max 50 caracteres, alfanuméricos y guiones)
 * @param tipo Tipo comercial del animal (opcional: VENTA, CRIA, ENGORDA, SEMENTAL, VIENTRE)
 * @param observaciones Observaciones libres sobre el animal (opcional, max 2000 caracteres)
 * 
 * @see UpdateAnimalCommand
 * @see mx.vacapp.cattle.internal.application.usecases.animal.UpdateAnimalUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record UpdateAnimalRequest(
    
    @Size(max = 20, message = "El arete anterior no puede exceder 20 caracteres")
    String areteAnterior,
    
    @Size(max = 50, message = "El folio REEMO no puede exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9\\-]*$", message = "El folio REEMO solo puede contener letras, números y guiones")
    String folioReemo,
    
    CattleType tipo,
    
    @Size(max = 2000, message = "Las observaciones no pueden exceder 2000 caracteres")
    String observaciones
    
) {
    
    /**
     * Convierte este DTO de Request a un comando de aplicación.
     * <p>
     * Este método transforma el DTO de infraestructura (UpdateAnimalRequest) en un comando
     * de la capa de aplicación (UpdateAnimalCommand), añadiendo los campos de contexto
     * de seguridad (animalId y updatedBy) que se extraen del path parameter y del
     * SecurityContext actual.
     * </p>
     * 
     * <h3>Campos Contextuales:</h3>
     * <ul>
     *   <li><strong>animalId</strong>: Se extrae del path parameter {id} de la URL</li>
     *   <li><strong>updatedBy</strong>: Se extrae del usuario autenticado actual (JWT)</li>
     * </ul>
     * 
     * <h3>Mapeo de Campos:</h3>
     * <p>El campo {@code observaciones} del Request se mapea al campo {@code nota} del Command
     * para mantener consistencia con el modelo de dominio.</p>
     * 
     * <h3>Uso:</h3>
     * <pre>
     * UpdateAnimalCommand command = request.toCommand(animalId, updatedBy);
     * animalResult = updateAnimalUseCase.execute(command);
     * </pre>
     * 
     * @param animalId UUID del animal a actualizar (extraído del path parameter)
     * @param updatedBy UUID del usuario que actualiza el registro (extraído del JWT)
     * @return UpdateAnimalCommand listo para ser procesado por el caso de uso
     * 
     * @see UpdateAnimalCommand
     * @see mx.vacapp.cattle.internal.application.usecases.animal.UpdateAnimalUseCase
     */
    public UpdateAnimalCommand toCommand(UUID animalId, UUID updatedBy) {
        return new UpdateAnimalCommand(
            animalId,
            null,              // arete - no actualizable desde este request
            areteAnterior,
            null,              // fechaAretado - no actualizable desde este request
            tipo,
            folioReemo,
            observaciones,     // se mapea a 'nota' en el comando
            null,              // madreId - no actualizable desde este request
            null,              // padreId - no actualizable desde este request
            updatedBy,
            null               // tenantId - debe ser establecido por el controller desde el SecurityContext
        );
    }
}

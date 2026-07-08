package mx.vacapp.cattle.internal.application.usecases.commands;

import mx.vacapp.cattle.internal.domain.model.CattleType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Comando que encapsula los datos para actualizar la información de un animal existente.
 * <p>
 * Este comando es utilizado como entrada para el caso de uso {@code UpdateAnimalUseCase}.
 * Contiene los campos opcionales que pueden ser actualizados en un animal ya registrado.
 * </p>
 * 
 * <h2>Campos Requeridos:</h2>
 * <ul>
 *   <li>{@code animalId} - Identificador del animal que se va a actualizar</li>
 *   <li>{@code updatedBy} - Identificador del usuario que realiza la actualización (extraído del contexto de seguridad)</li>
 *   <li>{@code tenantId} - Identificador del tenant para filtrado multi-tenant</li>
 * </ul>
 * 
 * <h2>Campos Actualizables (Opcionales):</h2>
 * <ul>
 *   <li>{@code arete} - Puede actualizar el arete si es necesario</li>
 *   <li>{@code areteAnterior} - Arete previo del animal</li>
 *   <li>{@code fechaAretado} - Fecha en que se colocó el arete</li>
 *   <li>{@code tipo} - Tipo comercial del animal</li>
 *   <li>{@code folioReemo} - Folio de Registro Electrónico de Movilización</li>
 *   <li>{@code nota} - Observaciones libres sobre el animal</li>
 *   <li>{@code madreId} - Identificador de la madre (para genealogía)</li>
 *   <li>{@code padreId} - Identificador del padre (para genealogía)</li>
 * </ul>
 * 
 * <h2>Campos NO Actualizables:</h2>
 * <ul>
 *   <li>{@code sexo} - Inmutable después de la creación</li>
 *   <li>{@code raza} - Inmutable después de la creación</li>
 *   <li>{@code fechaNacimiento} - Inmutable después de la creación</li>
 *   <li>{@code status} - Requiere comando dedicado (ChangeStatusCommand)</li>
 *   <li>{@code potrero} - Requiere comando dedicado (MoveAnimalCommand)</li>
 * </ul>
 * 
 * <h2>Validaciones:</h2>
 * <p>Las validaciones de negocio se realizan en el caso de uso y en la capa de dominio.
 * El caso de uso validará que el animal existe, pertenece al tenant correcto, y que
 * las relaciones genealógicas son válidas si se proporcionan madreId o padreId.</p>
 * 
 * @param animalId Identificador del animal que se va a actualizar
 * @param arete Identificador único del animal (opcional, 4-20 caracteres alfanuméricos)
 * @param areteAnterior Arete previo del animal (opcional, max 20 caracteres)
 * @param fechaAretado Fecha en que se colocó el arete (opcional)
 * @param tipo Tipo comercial del animal (opcional)
 * @param folioReemo Folio REEMO para cumplimiento regulatorio mexicano (opcional)
 * @param nota Observaciones libres sobre el animal (opcional, max 2000 caracteres)
 * @param madreId Identificador de la madre (opcional, debe ser hembra del mismo rancho)
 * @param padreId Identificador del padre (opcional, debe ser macho del mismo rancho)
 * @param updatedBy Identificador del usuario que realiza la actualización
 * @param tenantId Identificador del tenant (multi-tenancy)
 * 
 * @see mx.vacapp.cattle.internal.application.usecases.animal.UpdateAnimalUseCase
 * @see mx.vacapp.cattle.internal.domain.model.Animal
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record UpdateAnimalCommand(
    UUID animalId,
    String arete,
    String areteAnterior,
    LocalDate fechaAretado,
    CattleType tipo,
    String folioReemo,
    String nota,
    UUID madreId,
    UUID padreId,
    UUID updatedBy,
    UUID tenantId
) {
    /**
     * Constructor compacto que aplica normalizaciones básicas.
     * <p>
     * El arete se convierte a mayúsculas para consistencia en el almacenamiento,
     * independientemente de cómo lo ingrese el usuario.
     * </p>
     */
    public UpdateAnimalCommand {
        if (arete != null) {
            arete = arete.toUpperCase();
        }
    }
}

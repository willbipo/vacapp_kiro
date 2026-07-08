package mx.vacapp.cattle.internal.application.usecases.commands;

import mx.vacapp.cattle.internal.domain.model.Breed;
import mx.vacapp.cattle.internal.domain.model.CattleType;
import mx.vacapp.cattle.internal.domain.model.Sex;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Comando que encapsula todos los datos necesarios para crear un nuevo animal en el sistema.
 * <p>
 * Este comando es utilizado como entrada para el caso de uso {@code CreateAnimalUseCase}.
 * Contiene todos los campos requeridos y opcionales para el registro de un nuevo bovino
 * en el inventario de ganado.
 * </p>
 * 
 * <h2>Campos Requeridos:</h2>
 * <ul>
 *   <li>{@code arete} - Identificador único del animal a nivel global</li>
 *   <li>{@code sexo} - Sexo del animal (MACHO o HEMBRA)</li>
 *   <li>{@code raza} - Raza del animal</li>
 *   <li>{@code fechaNacimiento} - Fecha de nacimiento del animal</li>
 *   <li>{@code potreroId} - Identificador del potrero donde se asignará inicialmente</li>
 *   <li>{@code ranchoId} - Identificador del rancho al que pertenece</li>
 *   <li>{@code tenantId} - Identificador del tenant (extraído del contexto de seguridad)</li>
 *   <li>{@code createdBy} - Identificador del usuario que crea el registro (extraído del contexto de seguridad)</li>
 * </ul>
 * 
 * <h2>Campos Opcionales:</h2>
 * <ul>
 *   <li>{@code areteAnterior} - Arete previo del animal (si existía)</li>
 *   <li>{@code fechaAretado} - Fecha en que se colocó el arete</li>
 *   <li>{@code tipo} - Tipo comercial del animal (por defecto: VENTA)</li>
 *   <li>{@code folioReemo} - Folio de Registro Electrónico de Movilización (regulación mexicana)</li>
 *   <li>{@code nota} - Observaciones libres sobre el animal</li>
 *   <li>{@code madreId} - Identificador de la madre (para genealogía)</li>
 *   <li>{@code padreId} - Identificador del padre (para genealogía)</li>
 * </ul>
 * 
 * <h2>Validaciones:</h2>
 * <p>Las validaciones de negocio se realizan en el caso de uso y en la capa de dominio,
 * no en este comando. Sin embargo, la validación de formato y nulidad se realiza
 * en los DTOs de la capa de infraestructura mediante Bean Validation.</p>
 * 
 * @param arete Identificador único del animal (4-20 caracteres alfanuméricos)
 * @param areteAnterior Arete previo del animal (opcional, max 20 caracteres)
 * @param sexo Sexo del animal (MACHO o HEMBRA)
 * @param raza Raza del animal
 * @param fechaNacimiento Fecha de nacimiento del animal (no puede ser futura)
 * @param fechaAretado Fecha en que se colocó el arete (opcional)
 * @param tipo Tipo comercial del animal (opcional, por defecto VENTA)
 * @param folioReemo Folio REEMO para cumplimiento regulatorio mexicano (opcional)
 * @param nota Observaciones libres sobre el animal (opcional, max 2000 caracteres)
 * @param madreId Identificador de la madre (opcional, debe ser hembra del mismo rancho)
 * @param padreId Identificador del padre (opcional, debe ser macho del mismo rancho)
 * @param potreroId Identificador del potrero de asignación inicial
 * @param ranchoId Identificador del rancho al que pertenece el animal
 * @param tenantId Identificador del tenant (multi-tenancy)
 * @param createdBy Identificador del usuario que crea el registro
 * 
 * @see mx.vacapp.cattle.internal.application.usecases.animal.CreateAnimalUseCase
 * @see mx.vacapp.cattle.internal.domain.model.Animal
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record CreateAnimalCommand(
    String arete,
    String areteAnterior,
    Sex sexo,
    Breed raza,
    LocalDate fechaNacimiento,
    LocalDate fechaAretado,
    CattleType tipo,
    String folioReemo,
    String nota,
    UUID madreId,
    UUID padreId,
    UUID potreroId,
    UUID ranchoId,
    UUID tenantId,
    UUID createdBy
) {
    /**
     * Constructor compacto que aplica normalizaciones básicas.
     * <p>
     * El arete se convierte a mayúsculas para consistencia en el almacenamiento,
     * independientemente de cómo lo ingrese el usuario.
     * </p>
     */
    public CreateAnimalCommand {
        if (arete != null) {
            arete = arete.toUpperCase();
        }
    }
}

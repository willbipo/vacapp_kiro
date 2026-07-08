package mx.vacapp.cattle.internal.application.usecases.commands;

import mx.vacapp.cattle.internal.domain.model.Sex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Comando que encapsula todos los datos necesarios para registrar un evento de nacimiento (parto).
 * <p>
 * Este comando es utilizado como entrada para el caso de uso {@code RecordBirthEventUseCase}.
 * Este caso de uso especial registra un evento de salud de tipo BIRTH para la madre y
 * automáticamente crea un nuevo animal (la cría) en el sistema.
 * </p>
 * 
 * <h2>Campos Requeridos:</h2>
 * <ul>
 *   <li>{@code madreId} - Identificador del animal madre (debe ser HEMBRA)</li>
 *   <li>{@code fechaNacimiento} - Fecha del nacimiento (no puede ser futura)</li>
 *   <li>{@code sexo} - Sexo de la cría recién nacida (Macho o Hembra)</li>
 *   <li>{@code areteHijo} - Número de arete para identificar la cría (debe ser único)</li>
 *   <li>{@code recordedBy} - Identificador del usuario que registra el evento</li>
 * </ul>
 * 
 * <h2>Campos Opcionales:</h2>
 * <ul>
 *   <li>{@code padreId} - Identificador del padre (opcional)</li>
 *   <li>{@code pesoNacimientoKg} - Peso de la cría al nacer en kilogramos (opcional)</li>
 *   <li>{@code observaciones} - Observaciones sobre el nacimiento (opcional)</li>
 * </ul>
 * 
 * <h2>Flujo del Caso de Uso:</h2>
 * <ol>
 *   <li>Validar que madre existe y es HEMBRA</li>
 *   <li>Validar que madre está activa (no Vendida/Muerta)</li>
 *   <li>Validar que fechaNacimiento no es futura</li>
 *   <li>Validar que areteHijo es único (no existe en el sistema)</li>
 *   <li>Obtener potrero actual de la madre</li>
 *   <li>Crear nueva cría con los datos proporcionados + datos heredados de la madre</li>
 *   <li>Asignar cría al mismo potrero que la madre</li>
 *   <li>Si madre está PRENADA, cambiar su status a ACTIVA</li>
 *   <li>Registrar evento de salud BIRTH para la madre</li>
 *   <li>Retornar tanto el evento de salud como los datos de la cría creada</li>
 * </ol>
 * 
 * <h2>Validaciones de Negocio:</h2>
 * <ul>
 *   <li>Madre (madreId) debe existir y ser HEMBRA (Requirement 8.4)</li>
 *   <li>Madre no puede estar Vendida/Muerta (Requirement 16.1)</li>
 *   <li>fechaNacimiento no puede ser futura (Requirement 8.4)</li>
 *   <li>areteHijo debe ser único a nivel global (Requirement 1.2)</li>
 *   <li>Si madre.status = PRENADA, se cambia automáticamente a ACTIVA (Requirement 8.4)</li>
 * </ul>
 * 
 * <h2>Creación Automática de Cría:</h2>
 * <p>La cría se crea con los siguientes datos:</p>
 * <ul>
 *   <li>arete = areteHijo</li>
 *   <li>sexo = sexo (del comando)</li>
 *   <li>raza = madre.raza (heredada de la madre)</li>
 *   <li>fechaNacimiento = fechaNacimiento</li>
 *   <li>tipo = CRIA</li>
 *   <li>status = ACTIVA</li>
 *   <li>madreId = madreId</li>
 *   <li>padreId = padreId (opcional)</li>
 *   <li>ranchoId = madre.ranchoId</li>
 *   <li>tenantId = madre.tenantId</li>
 *   <li>potreroActual = madre.potreroActual (copiado)</li>
 *   <li>pesoNacimientoKg = pesoNacimientoKg (si se proporciona)</li>
 *   <li>fechaAretado = fechaNacimiento</li>
 * </ul>
 * 
 * @param madreId Identificador de la madre (NOT NULL, debe ser HEMBRA)
 * @param fechaNacimiento Fecha del nacimiento (NOT NULL, no puede ser futura)
 * @param sexo Sexo de la cría recién nacida (NOT NULL)
 * @param areteHijo Número de arete para la cría (NOT NULL, debe ser único)
 * @param padreId Identificador del padre (opcional)
 * @param pesoNacimientoKg Peso al nacer en kilogramos (opcional, debe ser > 0)
 * @param observaciones Observaciones sobre el nacimiento (opcional)
 * @param recordedBy Identificador del usuario que registra el evento (NOT NULL)
 * 
 * @see mx.vacapp.cattle.internal.application.usecases.health.RecordBirthEventUseCase
 * @see mx.vacapp.cattle.internal.domain.model.HealthEvent
 * @see mx.vacapp.cattle.internal.domain.model.Animal
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record RecordBirthCommand(
    UUID madreId,
    LocalDate fechaNacimiento,
    Sex sexo,
    String areteHijo,
    UUID padreId,
    BigDecimal pesoNacimientoKg,
    String observaciones,
    UUID recordedBy
) {
}

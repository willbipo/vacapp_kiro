package mx.vacapp.cattle.internal.application.usecases.commands;

import mx.vacapp.cattle.internal.domain.model.CattleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Comando que encapsula todos los datos necesarios para cambiar el status de un animal.
 * <p>
 * Este comando es utilizado como entrada para el caso de uso {@code ChangeStatusUseCase}.
 * Contiene todos los campos requeridos y opcionales para cambiar el estado de un animal
 * en el ciclo de vida del inventario (Activa, Preñada, Vendida, Muerta, Prestada, En Reposo).
 * </p>
 * 
 * <h2>Campos Requeridos:</h2>
 * <ul>
 *   <li>{@code animalId} - Identificador del animal cuyo status se va a cambiar</li>
 *   <li>{@code newStatus} - Nuevo status del animal</li>
 *   <li>{@code changedBy} - Identificador del usuario que realiza el cambio (extraído del contexto de seguridad)</li>
 *   <li>{@code tenantId} - Identificador del tenant (multi-tenancy)</li>
 * </ul>
 * 
 * <h2>Campos Condicionales:</h2>
 * <ul>
 *   <li>{@code fechaVenta} - Requerida cuando newStatus = VENDIDA</li>
 *   <li>{@code precioVenta} - Opcional cuando newStatus = VENDIDA (debe ser > 0)</li>
 *   <li>{@code fechaMuerte} - Requerida cuando newStatus = MUERTA</li>
 *   <li>{@code motivoMuerte} - Opcional cuando newStatus = MUERTA (máx 500 caracteres)</li>
 *   <li>{@code reason} - Razón opcional del cambio de status (para auditoría, máx 500 caracteres)</li>
 * </ul>
 * 
 * <h2>Estados Soportados:</h2>
 * <ul>
 *   <li><b>ACTIVA</b>: Animal activo en el rancho</li>
 *   <li><b>VENDIDA</b>: Animal vendido (requiere fechaVenta)</li>
 *   <li><b>MUERTA</b>: Animal muerto (requiere fechaMuerte)</li>
 *   <li><b>PRESTADA</b>: Animal prestado temporalmente a otro rancho</li>
 *   <li><b>PRENADA</b>: Animal preñado (solo hembras)</li>
 *   <li><b>EN_REPOSO</b>: Animal en período de descanso reproductivo</li>
 * </ul>
 * 
 * <h2>Validaciones:</h2>
 * <p>Las validaciones de negocio se realizan en el caso de uso:</p>
 * <ul>
 *   <li>El animal debe existir y pertenecer al tenant del usuario</li>
 *   <li>Para PRENADA: el animal debe ser hembra (sexo = HEMBRA)</li>
 *   <li>Para VENDIDA: fechaVenta es obligatoria, precioVenta opcional pero > 0</li>
 *   <li>Para MUERTA: fechaMuerte es obligatoria, motivoMuerte opcional</li>
 *   <li>No se puede cambiar el status de un animal ya vendido o muerto</li>
 * </ul>
 * 
 * <h2>Efectos Secundarios:</h2>
 * <p>Cuando el status cambia a VENDIDA o MUERTA:</p>
 * <ul>
 *   <li>Se actualiza el registro actual en pasture_history con fecha_salida = NOW()</li>
 *   <li>El animal se saca del potrero actual</li>
 *   <li>Se registra el cambio en cattle_audit con la razón especificada</li>
 * </ul>
 * 
 * <h2>Auditoría:</h2>
 * <p>El cambio de status se registra en la tabla cattle_audit con:</p>
 * <ul>
 *   <li>operation_type: CHANGE_STATUS</li>
 *   <li>old_values: status anterior + campos relacionados</li>
 *   <li>new_values: status nuevo + campos relacionados</li>
 *   <li>reason: razón del cambio (si se proporciona)</li>
 *   <li>timestamp: fecha y hora del cambio</li>
 *   <li>modified_by: usuario que realiza el cambio</li>
 * </ul>
 * 
 * @param animalId Identificador del animal cuyo status se va a cambiar
 * @param newStatus Nuevo status del animal
 * @param fechaVenta Fecha de venta (requerida si newStatus = VENDIDA)
 * @param precioVenta Precio de venta (opcional, debe ser > 0)
 * @param fechaMuerte Fecha de muerte (requerida si newStatus = MUERTA)
 * @param motivoMuerte Motivo de muerte (opcional, máx 500 caracteres)
 * @param reason Razón del cambio de status (opcional, para auditoría)
 * @param changedBy Identificador del usuario que realiza el cambio
 * @param tenantId Identificador del tenant (multi-tenancy)
 * 
 * @see mx.vacapp.cattle.internal.application.usecases.animal.ChangeStatusUseCase
 * @see mx.vacapp.cattle.internal.domain.model.Animal
 * @see mx.vacapp.cattle.internal.domain.model.CattleStatus
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record ChangeStatusCommand(
    UUID animalId,
    CattleStatus newStatus,
    LocalDate fechaVenta,
    BigDecimal precioVenta,
    LocalDate fechaMuerte,
    String motivoMuerte,
    String reason,
    UUID changedBy,
    UUID tenantId
) {
}

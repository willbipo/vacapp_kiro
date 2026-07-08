package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import mx.vacapp.cattle.internal.application.usecases.commands.CreateAnimalCommand;
import mx.vacapp.cattle.internal.domain.model.Breed;
import mx.vacapp.cattle.internal.domain.model.CattleStatus;
import mx.vacapp.cattle.internal.domain.model.CattleType;
import mx.vacapp.cattle.internal.domain.model.Sex;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de entrada (Request) para crear un nuevo animal bovino en el sistema.
 * <p>
 * Este Record representa los datos que llegan desde la API REST móvil para registrar
 * un nuevo animal en el inventario de ganado. Incluye validaciones Bean Validation
 * para garantizar la integridad de los datos antes de procesarlos en la capa de aplicación.
 * </p>
 * 
 * <h2>Validaciones Aplicadas:</h2>
 * <ul>
 *   <li><strong>arete</strong>: NOT NULL, 1-50 caracteres, solo alfanuméricos (letras y números)</li>
 *   <li><strong>areteAnterior</strong>: Opcional, max 20 caracteres</li>
 *   <li><strong>sexo</strong>: NOT NULL</li>
 *   <li><strong>raza</strong>: NOT NULL</li>
 *   <li><strong>fechaNacimiento</strong>: NOT NULL, debe ser fecha pasada</li>
 *   <li><strong>fechaAretado</strong>: Opcional, debe ser fecha pasada si se proporciona</li>
 *   <li><strong>tipo</strong>: NOT NULL</li>
 *   <li><strong>status</strong>: NOT NULL</li>
 *   <li><strong>folioReemo</strong>: Opcional, max 50 caracteres, solo alfanuméricos y guiones</li>
 *   <li><strong>nota</strong>: Opcional, max 2000 caracteres</li>
 *   <li><strong>madreId</strong>: Opcional, UUID válido</li>
 *   <li><strong>padreId</strong>: Opcional, UUID válido</li>
 *   <li><strong>potreroId</strong>: NOT NULL, UUID válido</li>
 *   <li><strong>ranchoId</strong>: NOT NULL, UUID válido</li>
 * </ul>
 * 
 * <h2>Flujo de Uso:</h2>
 * <pre>
 * 1. Cliente móvil envía POST /api/v1/cattle con JSON
 * 2. Spring valida automáticamente con @Valid
 * 3. Controller mapea CreateAnimalRequest → CreateAnimalCommand usando toCommand()
 * 4. CreateAnimalUseCase procesa el comando
 * </pre>
 * 
 * <h2>Ejemplo JSON:</h2>
 * <pre>
 * {
 *   "arete": "MX12345",
 *   "areteAnterior": "ABC123",
 *   "sexo": "HEMBRA",
 *   "raza": "ANGUS",
 *   "fechaNacimiento": "2023-03-15",
 *   "fechaAretado": "2023-03-20",
 *   "tipo": "VIENTRE",
 *   "status": "ACTIVA",
 *   "folioReemo": "REEMO-2024-001",
 *   "nota": "Animal de buena genética",
 *   "madreId": "550e8400-e29b-41d4-a716-446655440000",
 *   "padreId": "550e8400-e29b-41d4-a716-446655440001",
 *   "potreroId": "550e8400-e29b-41d4-a716-446655440002",
 *   "ranchoId": "550e8400-e29b-41d4-a716-446655440003"
 * }
 * </pre>
 * 
 * @param arete Identificador único del animal (4-50 caracteres alfanuméricos)
 * @param areteAnterior Arete previo del animal (opcional, max 20 caracteres)
 * @param sexo Sexo del animal (MACHO o HEMBRA)
 * @param raza Raza del animal
 * @param fechaNacimiento Fecha de nacimiento del animal (debe ser pasada)
 * @param fechaAretado Fecha en que se colocó el arete (opcional, debe ser pasada)
 * @param tipo Tipo comercial del animal (VENTA, CRIA, ENGORDA, SEMENTAL, VIENTRE)
 * @param status Estado del animal (ACTIVA, VENDIDA, MUERTA, PRESTADA, PRENADA, EN_REPOSO)
 * @param folioReemo Folio REEMO para regulación mexicana (opcional, max 50 caracteres)
 * @param nota Observaciones libres sobre el animal (opcional, max 2000 caracteres)
 * @param madreId UUID de la madre (opcional)
 * @param padreId UUID del padre (opcional)
 * @param potreroId UUID del potrero de asignación inicial (requerido)
 * @param ranchoId UUID del rancho al que pertenece (requerido)
 * @param meses Edad calculada en meses (opcional, para casos donde se calcula en frontend)
 * 
 * @see CreateAnimalCommand
 * @see mx.vacapp.cattle.internal.application.usecases.animal.CreateAnimalUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record CreateAnimalRequest(
    
    @NotNull(message = "El arete es obligatorio")
    @Size(min = 1, max = 50, message = "El arete debe tener entre 1 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El arete solo puede contener letras y números")
    String arete,
    
    @Size(max = 20, message = "El arete anterior no puede exceder 20 caracteres")
    String areteAnterior,
    
    @NotNull(message = "El sexo es obligatorio")
    Sex sexo,
    
    @NotNull(message = "La raza es obligatoria")
    Breed raza,
    
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser futura")
    LocalDate fechaNacimiento,
    
    @Past(message = "La fecha de aretado no puede ser futura")
    LocalDate fechaAretado,
    
    @NotNull(message = "El tipo es obligatorio")
    CattleType tipo,
    
    @NotNull(message = "El status es obligatorio")
    CattleStatus status,
    
    @Size(max = 50, message = "El folio REEMO no puede exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9\\-]*$", message = "El folio REEMO solo puede contener letras, números y guiones")
    String folioReemo,
    
    @Size(max = 2000, message = "La nota no puede exceder 2000 caracteres")
    String nota,
    
    UUID madreId,
    
    UUID padreId,
    
    @NotNull(message = "El potrero es obligatorio")
    UUID potreroId,
    
    @NotNull(message = "El rancho es obligatorio")
    UUID ranchoId,
    
    @PositiveOrZero(message = "Los meses deben ser cero o positivos")
    Integer meses
    
) {
    
    /**
     * Convierte este DTO de Request a un comando de aplicación.
     * <p>
     * Este método transforma el DTO de infraestructura (CreateAnimalRequest) en un comando
     * de la capa de aplicación (CreateAnimalCommand), añadiendo los campos de contexto
     * de seguridad (tenantId y createdBy) que se extraen del SecurityContext actual.
     * </p>
     * 
     * <h3>Campos Contextuales:</h3>
     * <ul>
     *   <li><strong>tenantId</strong>: Se extrae del contexto de seguridad (JWT) - multi-tenancy</li>
     *   <li><strong>createdBy</strong>: Se extrae del usuario autenticado actual</li>
     * </ul>
     * 
     * <h3>Uso:</h3>
     * <pre>
     * CreateAnimalCommand command = request.toCommand(tenantId, createdBy);
     * animalResult = createAnimalUseCase.execute(command);
     * </pre>
     * 
     * @param tenantId UUID del tenant extraído del JWT (contexto de seguridad)
     * @param createdBy UUID del usuario que crea el registro (extraído del JWT)
     * @return CreateAnimalCommand listo para ser procesado por el caso de uso
     * 
     * @see CreateAnimalCommand
     * @see mx.vacapp.cattle.internal.application.usecases.animal.CreateAnimalUseCase
     */
    public CreateAnimalCommand toCommand(UUID tenantId, UUID createdBy) {
        return new CreateAnimalCommand(
            arete,
            areteAnterior,
            sexo,
            raza,
            fechaNacimiento,
            fechaAretado,
            tipo,
            folioReemo,
            nota,
            madreId,
            padreId,
            potreroId,
            ranchoId,
            tenantId,
            createdBy
        );
    }
}

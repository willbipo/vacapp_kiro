package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import mx.vacapp.cattle.internal.application.usecases.commands.HealthEventResult;
import mx.vacapp.cattle.internal.domain.model.HealthEventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida (Response) para representar información de un evento de salud de un animal.
 * <p>
 * Este Record representa los datos que se retornan desde la API REST móvil cuando se consulta
 * información de eventos de salud (vacunaciones, tratamientos, partos, diagnósticos) del inventario de ganado.
 * Incluye todos los campos del evento incluyendo información opcional como medicamento, dosis, veterinario, etc.
 * </p>
 * 
 * <h2>Campos Incluidos:</h2>
 * <ul>
 *   <li><strong>eventId</strong>: Identificador único del evento de salud (UUID)</li>
 *   <li><strong>animalId</strong>: Identificador del animal (UUID)</li>
 *   <li><strong>tipoEvento</strong>: Tipo de evento (VACCINATION, TREATMENT, BIRTH, DIAGNOSIS)</li>
 *   <li><strong>fecha</strong>: Fecha en que ocurrió el evento</li>
 *   <li><strong>descripcion</strong>: Descripción del evento</li>
 *   <li><strong>medicamento</strong>: Nombre del medicamento aplicado (opcional)</li>
 *   <li><strong>dosis</strong>: Dosis del medicamento (opcional)</li>
 *   <li><strong>veterinario</strong>: Nombre del veterinario que realizó el evento (opcional)</li>
 *   <li><strong>proximaFecha</strong>: Fecha programada para próximo evento relacionado (opcional)</li>
 *   <li><strong>costo</strong>: Costo económico del evento (opcional)</li>
 *   <li><strong>observaciones</strong>: Observaciones adicionales del evento (opcional)</li>
 *   <li><strong>recordedAt</strong>: Timestamp de cuando se registró el evento</li>
 * </ul>
 * 
 * <h2>Tipos de Eventos:</h2>
 * <ul>
 *   <li><b>VACCINATION</b>: Vacunación del animal</li>
 *   <li><b>TREATMENT</b>: Tratamiento médico</li>
 *   <li><b>BIRTH</b>: Evento de parto (el animal es la madre)</li>
 *   <li><b>DIAGNOSIS</b>: Diagnóstico veterinario</li>
 * </ul>
 * 
 * <h2>Flujo de Uso:</h2>
 * <pre>
 * 1. UseCase retorna HealthEventResult
 * 2. Controller convierte a HealthEventResponse usando fromResult()
 * 3. Spring serializa a JSON y retorna al cliente móvil
 * </pre>
 * 
 * <h2>Ejemplo JSON:</h2>
 * <pre>
 * {
 *   "eventId": "550e8400-e29b-41d4-a716-446655440000",
 *   "animalId": "550e8400-e29b-41d4-a716-446655440001",
 *   "tipoEvento": "VACCINATION",
 *   "fecha": "2024-01-15",
 *   "descripcion": "Vacunación contra brucelosis",
 *   "medicamento": "Brucella Abortus RB51",
 *   "dosis": "2ml subcutánea",
 *   "veterinario": "Dr. Juan Pérez",
 *   "proximaFecha": "2024-07-15",
 *   "costo": 250.00,
 *   "observaciones": "Animal respondió bien al tratamiento",
 *   "recordedAt": "2024-01-15T10:30:00"
 * }
 * </pre>
 * 
 * @param eventId Identificador único del evento de salud
 * @param animalId Identificador del animal
 * @param tipoEvento Tipo de evento (HealthEventType)
 * @param fecha Fecha en que ocurrió el evento
 * @param descripcion Descripción del evento
 * @param medicamento Nombre del medicamento aplicado (opcional)
 * @param dosis Dosis del medicamento (opcional)
 * @param veterinario Nombre del veterinario (opcional)
 * @param proximaFecha Fecha programada para próximo evento (opcional)
 * @param costo Costo económico del evento (opcional)
 * @param observaciones Observaciones adicionales (opcional)
 * @param recordedAt Timestamp de registro
 * 
 * @see HealthEventResult
 * @see mx.vacapp.cattle.internal.application.usecases.health.RecordHealthEventUseCase
 * @see mx.vacapp.cattle.internal.application.usecases.health.GetHealthHistoryUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record HealthEventResponse(
    UUID eventId,
    UUID animalId,
    HealthEventType tipoEvento,
    LocalDate fecha,
    String descripcion,
    String medicamento,
    String dosis,
    String veterinario,
    LocalDate proximaFecha,
    BigDecimal costo,
    String observaciones,
    LocalDateTime recordedAt
) {
    
    /**
     * Método factory para crear un HealthEventResponse desde un HealthEventResult de la capa de aplicación.
     * <p>
     * Este método transforma el resultado de un caso de uso (HealthEventResult) en un DTO de respuesta
     * de infraestructura (HealthEventResponse) listo para ser serializado a JSON y retornado al cliente móvil.
     * </p>
     * 
     * <h3>Conversión de Campos:</h3>
     * <ul>
     *   <li>Todos los campos se mapean directamente desde HealthEventResult</li>
     *   <li>Los campos opcionales (medicamento, dosis, veterinario, proximaFecha, costo, observaciones) pueden ser null</li>
     *   <li>El campo recordedBy no se incluye en la respuesta por privacidad</li>
     * </ul>
     * 
     * <h3>Uso:</h3>
     * <pre>
     * HealthEventResult result = recordHealthEventUseCase.execute(command);
     * HealthEventResponse response = HealthEventResponse.fromResult(result);
     * return ResponseEntity.status(HttpStatus.CREATED).body(response);
     * </pre>
     * 
     * @param result HealthEventResult retornado desde la capa de aplicación
     * @return HealthEventResponse listo para ser serializado a JSON
     * @throws NullPointerException si result es null
     * 
     * @see HealthEventResult
     * @see HealthEventResult#fromDomain
     */
    public static HealthEventResponse fromResult(HealthEventResult result) {
        if (result == null) {
            throw new NullPointerException("result no puede ser null");
        }
        
        return new HealthEventResponse(
            result.eventId(),
            result.animalId(),
            result.tipoEvento(),
            result.fecha(),
            result.descripcion(),
            result.medicamento(),
            result.dosis(),
            result.veterinario(),
            result.proximaFecha(),
            result.costo(),
            result.observaciones(),
            result.recordedAt()
        );
    }
}

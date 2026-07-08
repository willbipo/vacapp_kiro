package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.domain.model.Breed;
import mx.vacapp.cattle.internal.domain.model.CattleStatus;
import mx.vacapp.cattle.internal.domain.model.CattleType;
import mx.vacapp.cattle.internal.domain.model.Sex;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de salida (Response) para representar información básica de un animal bovino.
 * <p>
 * Este Record representa los datos que se retornan desde la API REST móvil cuando se consulta
 * información de un animal en el inventario de ganado. Incluye los campos esenciales del animal
 * más campos enriquecidos calculados en tiempo real (peso actual, potrero actual, nombres de padres).
 * </p>
 * 
 * <h2>Campos Incluidos:</h2>
 * <ul>
 *   <li><strong>animal_id</strong>: Identificador único del animal (UUID)</li>
 *   <li><strong>arete</strong>: Número identificador único del animal</li>
 *   <li><strong>sexo</strong>: Sexo del animal (Macho/Hembra) como enum</li>
 *   <li><strong>raza</strong>: Raza del animal como enum</li>
 *   <li><strong>edad_meses</strong>: Edad calculada en meses desde fecha de nacimiento</li>
 *   <li><strong>tipo</strong>: Tipo comercial del animal como enum</li>
 *   <li><strong>status</strong>: Estado actual del animal como enum</li>
 *   <li><strong>potrero_actual</strong>: UUID del potrero donde se encuentra (opcional)</li>
 *   <li><strong>peso_actual</strong>: Último peso registrado en kg (opcional)</li>
 *   <li><strong>nombre_madre</strong>: Arete de la madre (opcional)</li>
 *   <li><strong>nombre_padre</strong>: Arete del padre (opcional)</li>
 * </ul>
 * 
 * <h2>Flujo de Uso:</h2>
 * <pre>
 * 1. UseCase retorna AnimalResult
 * 2. Controller convierte a AnimalResponse usando fromResult()
 * 3. Spring serializa a JSON y retorna al cliente móvil
 * </pre>
 * 
 * <h2>Ejemplo JSON:</h2>
 * <pre>
 * {
 *   "animal_id": "550e8400-e29b-41d4-a716-446655440000",
 *   "arete": "MX12345",
 *   "sexo": "HEMBRA",
 *   "raza": "ANGUS",
 *   "edad_meses": 24,
 *   "tipo": "VIENTRE",
 *   "status": "ACTIVA",
 *   "potrero_actual": "550e8400-e29b-41d4-a716-446655440002",
 *   "peso_actual": 450.50,
 *   "nombre_madre": "MX98765",
 *   "nombre_padre": "MX54321"
 * }
 * </pre>
 * 
 * @param animal_id Identificador único del animal (UUID)
 * @param arete Número identificador único del animal (4-50 caracteres alfanuméricos)
 * @param sexo Sexo del animal (MACHO o HEMBRA)
 * @param raza Raza del animal
 * @param edad_meses Edad calculada en meses desde fecha de nacimiento
 * @param tipo Tipo comercial del animal (VENTA, CRIA, ENGORDA, SEMENTAL, VIENTRE)
 * @param status Estado del animal (ACTIVA, VENDIDA, MUERTA, PRESTADA, PRENADA, EN_REPOSO)
 * @param potrero_actual UUID del potrero donde se encuentra actualmente (opcional)
 * @param peso_actual Último peso registrado en kilogramos (opcional)
 * @param nombre_madre Arete de la madre (opcional)
 * @param nombre_padre Arete del padre (opcional)
 * 
 * @see AnimalResult
 * @see mx.vacapp.cattle.internal.application.usecases.animal.GetAnimalUseCase
 * @see mx.vacapp.cattle.internal.application.usecases.animal.ListAnimalsUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record AnimalResponse(
    UUID animal_id,
    String arete,
    Sex sexo,
    Breed raza,
    Integer edad_meses,
    CattleType tipo,
    CattleStatus status,
    UUID potrero_actual,
    BigDecimal peso_actual,
    String nombre_madre,
    String nombre_padre
) {
    
    /**
     * Método factory para crear un AnimalResponse desde un AnimalResult de la capa de aplicación.
     * <p>
     * Este método transforma el resultado de un caso de uso (AnimalResult) en un DTO de respuesta
     * de infraestructura (AnimalResponse) listo para ser serializado a JSON y retornado al cliente móvil.
     * </p>
     * 
     * <h3>Conversión de Campos:</h3>
     * <ul>
     *   <li>Los campos String del AnimalResult (sexo, raza, tipo, status) se convierten de vuelta a enums</li>
     *   <li>Se utiliza el método fromValue() de cada enum para parsear los valores</li>
     *   <li>Los campos enriquecidos (pesoActual, potreroActual, nombreMadre, nombrePadre) se mapean directamente</li>
     *   <li>Se usa el campo meses calculado en tiempo real del AnimalResult</li>
     * </ul>
     * 
     * <h3>Uso:</h3>
     * <pre>
     * AnimalResult result = getAnimalUseCase.execute(animalId);
     * AnimalResponse response = AnimalResponse.fromResult(result);
     * return ResponseEntity.ok(response);
     * </pre>
     * 
     * @param result AnimalResult retornado desde la capa de aplicación
     * @return AnimalResponse listo para ser serializado a JSON
     * @throws IllegalArgumentException si algún valor de enum no es válido
     * 
     * @see AnimalResult
     * @see AnimalResult#fromDomainEnriched
     */
    public static AnimalResponse fromResult(AnimalResult result) {
        return new AnimalResponse(
            result.animalId(),
            result.arete(),
            Sex.fromValue(result.sexo()),
            Breed.fromValue(result.raza()),
            result.meses(),
            CattleType.fromValue(result.tipo()),
            CattleStatus.fromValue(result.status()),
            result.potreroActual(),
            result.pesoActual(),
            result.nombreMadre(),
            result.nombrePadre()
        );
    }
}

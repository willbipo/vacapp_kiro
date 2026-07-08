package mx.vacapp.cattle.internal.infrastructure.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidPastureException;
import mx.vacapp.cattle.internal.infrastructure.cache.CacheNames;
import mx.vacapp.geography.GeographyService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Cliente para integración con el módulo geographic-control.
 * 
 * <p>Este cliente actúa como adaptador de infraestructura para invocar
 * el servicio público de geografía desde el módulo cattle-inventory.
 * Se utiliza principalmente para validar que los potreros existen y están
 * activos antes de asignar animales a ellos (Requirement 3).</p>
 * 
 * <p>Implementa comunicación módulo-a-módulo usando Spring Modulith mediante
 * llamadas directas al API público de GeographyService, sin necesidad de HTTP REST.</p>
 * 
 * <p>Incluye caché para optimizar consultas frecuentes de potreros activos,
 * reduciendo la carga en el módulo de geografía.</p>
 * 
 * @see mx.vacapp.geography.GeographyService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeographyServiceClient {
    
    private final GeographyService geographyService;
    
    /**
     * Valida que un potrero existe y está activo.
     * 
     * <p>Este método es invocado desde casos de uso antes de asignar
     * animales a potreros (ver Requirement 3 del spec). Si el potrero
     * no existe o está inactivo, retorna false.</p>
     * 
     * <p>Manejo de errores: Si ocurre alguna excepción al consultar el
     * servicio de geografía (ej. el módulo está temporalmente indisponible),
     * se registra el error en logs y se retorna false de forma defensiva
     * para prevenir asignaciones a potreros potencialmente inválidos.</p>
     * 
     * <p>Los resultados se cachean durante 5 minutos para reducir la carga
     * en el módulo de geografía para consultas repetidas del mismo potrero.</p>
     * 
     * @param potreroId UUID del potrero a validar
     * @return true si el potrero existe y está activo, false en caso contrario
     *         o si ocurre algún error al consultar el servicio
     */
    @Cacheable(value = CacheNames.POTRERO_VALIDATION, key = "#potreroId", unless = "#result == false")
    public boolean isPotreroActive(UUID potreroId) {
        try {
            log.debug("Validando potrero activo: potreroId={}", potreroId);
            boolean isActive = geographyService.isPotreroActive(potreroId);
            
            if (!isActive) {
                log.warn("Potrero no existe o está inactivo: potreroId={}", potreroId);
            } else {
                log.debug("Potrero validado exitosamente: potreroId={}", potreroId);
            }
            
            return isActive;
        } catch (Exception e) {
            log.error("Error al validar potrero con geography service: potreroId={}. " +
                      "Retornando false de forma defensiva.", potreroId, e);
            return false;
        }
    }
    
    /**
     * Valida que un potrero existe y está activo, lanzando excepción si no lo está.
     * 
     * <p>Este método proporciona una API de validación fail-fast que lanza
     * {@link InvalidPastureException} inmediatamente si el potrero no está activo,
     * evitando la necesidad de verificar manualmente valores booleanos en los
     * casos de uso.</p>
     * 
     * <p>Internamente delega en {@link #isPotreroActive(UUID)} para reutilizar
     * la lógica de caché y manejo de errores existente.</p>
     * 
     * <p>Casos de uso típicos:</p>
     * <ul>
     *   <li>Validación antes de registrar un nuevo animal en un potrero</li>
     *   <li>Validación antes de mover un animal entre potreros</li>
     *   <li>Cualquier operación que requiera garantizar la existencia y
     *       disponibilidad del potrero</li>
     * </ul>
     * 
     * @param potreroId UUID del potrero a validar
     * @throws InvalidPastureException si el potrero no existe, está inactivo,
     *         o si ocurre un error al consultar el servicio de geografía
     * @see Requirement 3.1 y 3.2 del spec
     */
    public void validatePotreroActive(UUID potreroId) {
        if (!isPotreroActive(potreroId)) {
            throw new InvalidPastureException("Potrero no existe o está inactivo");
        }
    }
}

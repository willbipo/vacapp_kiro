package mx.vacapp.cattle.internal.application.usecases.animal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.users.internal.infrastructure.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso para búsqueda de animales por arete con coincidencia parcial.
 * 
 * <p>Este caso de uso implementa la funcionalidad de búsqueda case-insensitive
 * de animales cuyo arete contenga la cadena de búsqueda especificada. Es útil
 * para funcionalidades de autocompletado, búsqueda rápida, y filtrado en tiempo
 * real en interfaces de usuario.</p>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Extraer tenant_id del contexto de seguridad (TenantContext)</li>
 *   <li>Normalizar la consulta de búsqueda a minúsculas para búsqueda case-insensitive</li>
 *   <li>Llamar a AnimalRepository.findByAreteContaining() con pattern matching ILIKE/LOWER</li>
 *   <li>Filtrar automáticamente por tenant_id para garantizar aislamiento multi-tenant</li>
 *   <li>Calcular meses (edad) en tiempo real para cada resultado</li>
 *   <li>Enriquecer con peso_actual, potrero_actual (opcional, según necesidad)</li>
 *   <li>Ordenar resultados por arete ASC</li>
 *   <li>Retornar lista de AnimalResult</li>
 * </ol>
 * 
 * <h2>Características:</h2>
 * <ul>
 *   <li><b>Case-insensitive:</b> Búsqueda insensible a mayúsculas/minúsculas usando
 *       LOWER() en SQL o IgnoreCase en JPA</li>
 *   <li><b>Pattern matching:</b> Usa LIKE '%query%' para coincidencias parciales en cualquier
 *       posición del arete</li>
 *   <li><b>Multi-tenancy:</b> Filtra automáticamente por tenant_id del contexto de seguridad</li>
 *   <li><b>Edad calculada:</b> El campo meses se calcula en tiempo real desde fecha_nacimiento</li>
 *   <li><b>Ordenamiento:</b> Resultados ordenados alfabéticamente por arete ASC</li>
 * </ul>
 * 
 * <h2>Ejemplo de Uso:</h2>
 * <pre>
 * // Búsqueda de animales con arete que contenga "123"
 * List&lt;AnimalResult&gt; results = searchUseCase.execute("123");
 * // Puede retornar: A123, A1234, 123B, X123Y, etc.
 * </pre>
 * 
 * <h2>Requisitos Relacionados:</h2>
 * <ul>
 *   <li><b>Requirement 1.7:</b> Búsqueda case-insensitive por arete</li>
 *   <li><b>Requirement 2.10:</b> Cálculo de meses en tiempo real</li>
 * </ul>
 * 
 * @see AnimalRepository#findByAreteContaining(UUID, String)
 * @see AnimalResult
 * @see Animal
 * @see TenantContext
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAnimalsByAreteUseCase {
    
    private final AnimalRepository animalRepository;
    
    /**
     * Ejecuta la búsqueda de animales por arete con coincidencia parcial.
     * 
     * <p>Realiza una búsqueda case-insensitive de animales cuyo arete contenga
     * la cadena de búsqueda especificada. Los resultados se filtran automáticamente
     * por tenant_id del contexto de seguridad y se ordenan por arete ASC.</p>
     * 
     * <p><b>Comportamiento de búsqueda:</b></p>
     * <ul>
     *   <li>Query "123" encuentra: A123, 123B, X123Y, 1234</li>
     *   <li>Query "a12" encuentra: A123, A124, XA12Y (case-insensitive)</li>
     *   <li>Query vacía o null: lanza IllegalArgumentException</li>
     * </ul>
     * 
     * <p><b>Multi-tenancy:</b></p>
     * <ul>
     *   <li>Extrae tenant_id del TenantContext (SecurityContext)</li>
     *   <li>Solo retorna animales del tenant del usuario autenticado</li>
     *   <li>Garantiza aislamiento de datos entre tenants</li>
     * </ul>
     * 
     * <p><b>Cálculo en tiempo real:</b></p>
     * <ul>
     *   <li>El campo meses se calcula dinámicamente desde fecha_nacimiento</li>
     *   <li>No se almacena en base de datos para garantizar precisión</li>
     * </ul>
     * 
     * @param areteQuery cadena de búsqueda para el arete (puede contener mayúsculas/minúsculas)
     * @return lista de AnimalResult que coinciden con la búsqueda, ordenada por arete ASC
     * @throws IllegalArgumentException si areteQuery es null o vacío
     * @throws IllegalStateException si no hay tenant_id en el contexto de seguridad
     */
    @Transactional(readOnly = true)
    public List<AnimalResult> execute(String areteQuery) {
        log.info("Iniciando búsqueda de animales por arete: query={}", areteQuery);
        
        // Validar parámetro de entrada
        if (areteQuery == null || areteQuery.isBlank()) {
            log.warn("Intento de búsqueda con query vacía o null");
            throw new IllegalArgumentException("La consulta de búsqueda no puede estar vacía");
        }
        
        // Extraer tenant_id del contexto de seguridad
        UUID currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null) {
            log.error("No hay tenant_id en el contexto de seguridad");
            throw new IllegalStateException("No hay tenant_id en el contexto de seguridad");
        }
        
        log.debug("Ejecutando búsqueda: tenantId={}, areteQuery={}", currentTenantId, areteQuery);
        
        // Ejecutar búsqueda con pattern matching ILIKE/LOWER
        // El repositorio normaliza la consulta a minúsculas y filtra por tenant_id
        List<Animal> animals = animalRepository.findByAreteContaining(currentTenantId, areteQuery);
        
        log.info("Búsqueda completada: encontrados {} animales con arete que contiene '{}'", 
                 animals.size(), areteQuery);
        
        // Transformar a AnimalResult
        // El campo meses se calcula automáticamente en tiempo real
        List<AnimalResult> results = animals.stream()
            .map(AnimalResult::fromDomain)
            .collect(Collectors.toList());
        
        log.debug("Resultados transformados: {} AnimalResult generados", results.size());
        
        return results;
    }
}

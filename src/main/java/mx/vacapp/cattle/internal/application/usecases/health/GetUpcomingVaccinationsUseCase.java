package mx.vacapp.cattle.internal.application.usecases.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.UpcomingVaccinationResult;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.HealthEvent;
import mx.vacapp.cattle.internal.domain.model.HealthEventType;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.HealthEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso para obtener vacunaciones próximas de un animal.
 * 
 * <p>Este caso de uso calcula las vacunaciones próximas basándose en el historial
 * de vacunaciones del animal. Para cada evento de vacunación donde se haya registrado
 * una {@code proximaFecha}, se incluye en el resultado si la fecha es futura (>= HOY).</p>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Validar que el animal existe y pertenece al tenant del usuario</li>
 *   <li>Obtener todos los eventos de salud de tipo VACUNACION para el animal</li>
 *   <li>Para cada evento de vacunación:
 *     <ul>
 *       <li>Si tiene proximaFecha establecida, incluirla en el resultado</li>
 *       <li>Extraer nombreVacuna del campo descripcion</li>
 *       <li>Calcular diasRestantes desde HOY hasta proximaFecha</li>
 *     </ul>
 *   </li>
 *   <li>Filtrar solo las vacunaciones con fecha >= HOY (futuras o de hoy)</li>
 *   <li>Ordenar por fecha ASC (más próximas primero)</li>
 * </ol>
 * 
 * <h2>Cálculo de Próximas Fechas:</h2>
 * <p>Este caso de uso NO calcula intervalos estándar. Solo utiliza las fechas
 * programadas explícitamente en el campo {@code proximaFecha} de cada evento
 * de vacunación. Si un evento no tiene {@code proximaFecha}, se omite del resultado.</p>
 * 
 * <h2>Extracción de Nombre de Vacuna:</h2>
 * <p>El nombre de la vacuna se extrae del campo {@code descripcion} del evento.
 * Si la descripción contiene múltiples líneas o detalles adicionales, se toma
 * la primera línea o los primeros 100 caracteres como nombre.</p>
 * 
 * <h2>Ordenamiento:</h2>
 * <p>Los resultados se ordenan por {@code proximaFecha} ASC (ascendente), de modo
 * que las vacunaciones más urgentes aparecen primero en la lista.</p>
 * 
 * <h2>Validaciones:</h2>
 * <ul>
 *   <li><b>Animal existe:</b> El animal debe existir y pertenecer al tenant</li>
 *   <li><b>Fecha futura:</b> Solo se incluyen vacunaciones con proximaFecha >= HOY</li>
 * </ul>
 * 
 * <h2>Casos Especiales:</h2>
 * <ul>
 *   <li>Si el animal no tiene eventos de vacunación, retorna lista vacía</li>
 *   <li>Si todos los eventos no tienen proximaFecha, retorna lista vacía</li>
 *   <li>Si todas las fechas son pasadas, retorna lista vacía</li>
 * </ul>
 * 
 * @see UpcomingVaccinationResult
 * @see HealthEvent
 * @see HealthEventRepository
 * @see AnimalRepository
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetUpcomingVaccinationsUseCase {
    
    private final AnimalRepository animalRepository;
    private final HealthEventRepository healthEventRepository;
    
    /**
     * Ejecuta el caso de uso de obtener vacunaciones próximas.
     * 
     * @param animalId UUID del animal
     * @return lista de vacunaciones próximas ordenadas por fecha (más próximas primero)
     * @throws AnimalNotFoundException si el animal no existe o no pertenece al tenant
     */
    @Transactional(readOnly = true)
    public List<UpcomingVaccinationResult> execute(UUID animalId) {
        log.info("Obteniendo vacunaciones próximas para animal: animalId={}", animalId);
        
        // 1. Validar que el animal existe y pertenece al tenant
        Animal animal = animalRepository.findById(animalId)
            .orElseThrow(() -> {
                log.warn("Animal no encontrado o no pertenece al tenant: animalId={}", animalId);
                return new AnimalNotFoundException("Animal no encontrado");
            });
        
        log.debug("Animal encontrado: animalId={}, arete={}", animal.getAnimalId(), animal.getArete());
        
        // 2. Obtener todos los eventos de salud del animal
        List<HealthEvent> allHealthEvents = healthEventRepository.findByAnimalId(animalId);
        log.debug("Total de eventos de salud encontrados: count={}", allHealthEvents.size());
        
        // 3. Filtrar solo eventos de tipo VACUNACION
        List<HealthEvent> vaccinationEvents = allHealthEvents.stream()
            .filter(event -> event.getTipoEvento() == HealthEventType.Vacunacion)
            .collect(Collectors.toList());
        
        log.debug("Eventos de vacunación encontrados: count={}", vaccinationEvents.size());
        
        // 4. Para cada evento de vacunación, si tiene proximaFecha, crear resultado
        LocalDate today = LocalDate.now();
        List<UpcomingVaccinationResult> upcomingVaccinations = new ArrayList<>();
        
        for (HealthEvent event : vaccinationEvents) {
            // Solo procesar si tiene proximaFecha establecida
            if (event.getProximaFecha() != null) {
                LocalDate proximaFecha = event.getProximaFecha();
                
                // 5. Filtrar solo fechas futuras o de hoy (>= TODAY)
                if (!proximaFecha.isBefore(today)) {
                    String nombreVacuna = extractVaccineName(event.getDescripcion());
                    
                    UpcomingVaccinationResult result = UpcomingVaccinationResult.of(
                        event.getEventId(),
                        event.getAnimalId(),
                        nombreVacuna,
                        event.getFecha(),  // ultimaFecha = fecha del evento
                        proximaFecha
                    );
                    
                    upcomingVaccinations.add(result);
                    
                    log.debug("Vacunación próxima agregada: vacuna={}, proximaFecha={}, diasRestantes={}", 
                              nombreVacuna, proximaFecha, result.diasRestantes());
                }
            }
        }
        
        // 6. Ordenar por fecha ASC (más próximas primero)
        upcomingVaccinations.sort(Comparator.comparing(UpcomingVaccinationResult::proximaFecha));
        
        log.info("Vacunaciones próximas calculadas: animalId={}, count={}", 
                 animalId, upcomingVaccinations.size());
        
        return upcomingVaccinations;
    }
    
    /**
     * Extrae el nombre de la vacuna del campo descripcion.
     * 
     * <p>El nombre de la vacuna se considera la primera línea de la descripción
     * o los primeros 100 caracteres si no hay saltos de línea.</p>
     * 
     * <p>Ejemplos:</p>
     * <ul>
     *   <li>"Triple viral" → "Triple viral"</li>
     *   <li>"Rabia\nDosis de refuerzo" → "Rabia"</li>
     *   <li>"Brucelosis (RB51)" → "Brucelosis (RB51)"</li>
     * </ul>
     * 
     * @param descripcion descripción completa del evento de vacunación
     * @return nombre de la vacuna extraído
     */
    private String extractVaccineName(String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return "Vacuna sin nombre";
        }
        
        // Tomar la primera línea
        String[] lines = descripcion.split("\\r?\\n");
        String firstLine = lines[0].trim();
        
        // Limitar a 100 caracteres máximo
        if (firstLine.length() > 100) {
            return firstLine.substring(0, 100) + "...";
        }
        
        return firstLine;
    }
}

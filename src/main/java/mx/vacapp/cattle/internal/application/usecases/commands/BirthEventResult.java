package mx.vacapp.cattle.internal.application.usecases.commands;

/**
 * Resultado inmutable que representa el resultado completo de un evento de nacimiento.
 * <p>
 * Este record encapsula tanto el evento de salud registrado para la madre como
 * los datos del animal recién nacido (cría) que fue creado automáticamente.
 * </p>
 * 
 * <h2>Propósito:</h2>
 * <p>
 * El evento de nacimiento es único porque no solo registra un evento de salud,
 * sino que también crea automáticamente un nuevo animal en el sistema. Por lo tanto,
 * el resultado debe incluir información de ambas operaciones.
 * </p>
 * 
 * <h2>Componentes:</h2>
 * <ul>
 *   <li><b>healthEvent</b>: El evento de salud de tipo BIRTH registrado para la madre</li>
 *   <li><b>offspring</b>: Los datos completos del animal recién creado (la cría)</li>
 * </ul>
 * 
 * <h2>Uso Típico:</h2>
 * <pre>{@code
 * BirthEventResult result = recordBirthEventUseCase.execute(command);
 * 
 * // Acceder al evento de salud registrado en la madre
 * HealthEventResult birthEvent = result.healthEvent();
 * System.out.println("Evento registrado: " + birthEvent.eventId());
 * 
 * // Acceder a los datos de la cría creada
 * AnimalResult cria = result.offspring();
 * System.out.println("Cría creada con arete: " + cria.arete());
 * System.out.println("Madre: " + cria.madreId());
 * System.out.println("Potrero: " + cria.potreroActual());
 * }</pre>
 * 
 * @param healthEvent El evento de salud de tipo BIRTH registrado para la madre
 * @param offspring Los datos del animal recién creado (la cría)
 * 
 * @see HealthEventResult
 * @see AnimalResult
 * @see mx.vacapp.cattle.internal.application.usecases.health.RecordBirthEventUseCase
 * @see mx.vacapp.cattle.internal.domain.model.HealthEvent
 * @see mx.vacapp.cattle.internal.domain.model.Animal
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record BirthEventResult(
    HealthEventResult healthEvent,
    AnimalResult offspring
) {
    /**
     * Constructor canónico con validaciones.
     * Garantiza que ninguno de los componentes sea null.
     * 
     * @param healthEvent El evento de salud registrado (NOT NULL)
     * @param offspring Los datos de la cría creada (NOT NULL)
     * @throws NullPointerException si algún parámetro es null
     */
    public BirthEventResult {
        if (healthEvent == null) {
            throw new NullPointerException("healthEvent no puede ser null");
        }
        if (offspring == null) {
            throw new NullPointerException("offspring no puede ser null");
        }
    }
}

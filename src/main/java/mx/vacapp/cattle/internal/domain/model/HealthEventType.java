package mx.vacapp.cattle.internal.domain.model;

/**
 * Enum que representa los tipos de eventos de salud soportados en el sistema.
 * 
 * Tipos de eventos:
 * - Vacunacion: Vacunación del animal (preventivo)
 * - Desparasitacion: Desparasitación del animal
 * - Tratamiento: Tratamiento médico (curativo)
 * - Diagnostico: Diagnóstico veterinario (evaluación de salud)
 * - Cirugia: Intervención quirúrgica
 * - Revision: Revisión general de salud
 * - Birth: Evento de nacimiento/parto (la madre da a luz)
 */
public enum HealthEventType {
    Vacunacion("vacunacion"),
    Desparasitacion("desparasitacion"),
    Tratamiento("tratamiento"),
    Diagnostico("diagnostico"),
    Cirugia("cirugia"),
    Revision("revision"),
    Birth("birth");
    
    private final String value;
    
    HealthEventType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static HealthEventType fromValue(String value) {
        for (HealthEventType type : HealthEventType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de evento inválido: " + value);
    }
}

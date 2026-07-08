package mx.vacapp.cattle.internal.domain.model;

/**
 * Enum que representa el sexo de un animal bovino.
 * 
 * Los valores posibles son:
 * - MACHO: animal masculino
 * - HEMBRA: animal femenino
 * 
 * Cada valor tiene una representación en minúsculas (lowercase) para almacenamiento en base de datos.
 */
public enum Sex {
    
    /**
     * Animal masculino (macho).
     */
    MACHO("macho"),
    
    /**
     * Animal femenino (hembra).
     */
    HEMBRA("hembra");
    
    private final String value;
    
    /**
     * Constructor del enum.
     * 
     * @param value representación en minúsculas del sexo
     */
    Sex(String value) {
        this.value = value;
    }
    
    /**
     * Obtiene la representación en minúsculas del sexo.
     * 
     * @return valor en minúsculas ("macho" o "hembra")
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Convierte un string a un valor del enum.
     * 
     * @param value valor en minúsculas ("macho" o "hembra")
     * @return el enum Sex correspondiente
     * @throws IllegalArgumentException si el valor no es válido
     */
    public static Sex fromValue(String value) {
        for (Sex sex : Sex.values()) {
            if (sex.value.equalsIgnoreCase(value)) {
                return sex;
            }
        }
        throw new IllegalArgumentException("Valor de sexo inválido: " + value);
    }
}

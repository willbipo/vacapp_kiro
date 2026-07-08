package mx.vacapp.cattle.internal.domain.model;

/**
 * Enum que representa el estado del animal bovino en su ciclo de vida.
 * 
 * Los valores posibles son:
 * - ACTIVA: animal activo en el rancho
 * - VENDIDA: animal vendido y fuera del rancho
 * - MUERTA: animal fallecido
 * - PRESTADA: animal prestado temporalmente
 * - PRENADA: animal hembra en estado de preñez
 * - EN_REPOSO: animal en período de reposo
 * 
 * Cada valor tiene una representación en minúsculas (lowercase) para almacenamiento en base de datos.
 */
public enum CattleStatus {
    
    /**
     * Animal activo en el rancho.
     */
    ACTIVA("activa"),
    
    /**
     * Animal vendido y fuera del rancho.
     */
    VENDIDA("vendida"),
    
    /**
     * Animal fallecido.
     */
    MUERTA("muerta"),
    
    /**
     * Animal prestado temporalmente.
     */
    PRESTADA("prestada"),
    
    /**
     * Animal hembra en estado de preñez.
     */
    PRENADA("prenada"),
    
    /**
     * Animal en período de reposo.
     */
    EN_REPOSO("en_reposo");
    
    private final String value;
    
    /**
     * Constructor del enum.
     * 
     * @param value representación en minúsculas del estado
     */
    CattleStatus(String value) {
        this.value = value;
    }
    
    /**
     * Obtiene la representación en minúsculas del estado.
     * 
     * @return valor en minúsculas ("activa", "vendida", "muerta", "prestada", "prenada", "en_reposo")
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Convierte un string a un valor del enum.
     * 
     * @param value valor en minúsculas
     * @return el enum CattleStatus correspondiente
     * @throws IllegalArgumentException si el valor no es válido
     */
    public static CattleStatus fromValue(String value) {
        for (CattleStatus status : CattleStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Valor de estado inválido: " + value);
    }
}

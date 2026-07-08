package mx.vacapp.cattle.internal.domain.model;

/**
 * Enum que representa el tipo comercial de un animal bovino.
 * 
 * Los valores posibles son:
 * - VENTA: animal destinado a venta
 * - CRIA: animal destinado a cría
 * - ENGORDA: animal destinado a engorda
 * - SEMENTAL: animal macho reproductor
 * - VIENTRE: animal hembra reproductora
 * 
 * Cada valor tiene una representación en minúsculas (lowercase) para almacenamiento en base de datos.
 */
public enum CattleType {
    
    /**
     * Animal destinado a venta.
     */
    VENTA("venta"),
    
    /**
     * Animal destinado a cría.
     */
    CRIA("cria"),
    
    /**
     * Animal destinado a engorda.
     */
    ENGORDA("engorda"),
    
    /**
     * Animal macho reproductor (semental).
     */
    SEMENTAL("semental"),
    
    /**
     * Animal hembra reproductora (vientre).
     */
    VIENTRE("vientre");
    
    private final String value;
    
    /**
     * Constructor del enum.
     * 
     * @param value representación en minúsculas del tipo comercial
     */
    CattleType(String value) {
        this.value = value;
    }
    
    /**
     * Obtiene la representación en minúsculas del tipo comercial.
     * 
     * @return valor en minúsculas ("venta", "cria", "engorda", "semental", "vientre")
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Convierte un string a un valor del enum.
     * 
     * @param value valor en minúsculas ("venta", "cria", "engorda", "semental", "vientre")
     * @return el enum CattleType correspondiente
     * @throws IllegalArgumentException si el valor no es válido
     */
    public static CattleType fromValue(String value) {
        for (CattleType type : CattleType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Valor de tipo comercial inválido: " + value);
    }
}

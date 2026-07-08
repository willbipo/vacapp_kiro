package mx.vacapp.cattle.internal.domain.model;

/**
 * Enum que representa la raza de un animal bovino.
 * 
 * Los valores posibles son:
 * - CHAROLAIS: raza francesa de carne
 * - ANGUS: raza escocesa de carne, conocida por su calidad
 * - BRAHMAN: raza índica resistente al calor
 * - HEREFORD: raza británica de carne
 * - SIMMENTAL: raza suiza de doble propósito
 * - LIMOUSIN: raza francesa de carne
 * - CRIOLLO: raza criolla adaptada a condiciones locales
 * - BRANGUS: cruce entre Brahman y Angus
 * - SANTA_GERTRUDIS: raza estadounidense desarrollada en Texas
 * - CRUZADA: animales de cruce sin raza definida
 * 
 * Cada valor tiene una representación en minúsculas (lowercase) para almacenamiento en base de datos.
 */
public enum Breed {
    
    /**
     * Raza Charolais.
     */
    CHAROLAIS("charolais"),
    
    /**
     * Raza Angus.
     */
    ANGUS("angus"),
    
    /**
     * Raza Brahman.
     */
    BRAHMAN("brahman"),
    
    /**
     * Raza Hereford.
     */
    HEREFORD("hereford"),
    
    /**
     * Raza Simmental.
     */
    SIMMENTAL("simmental"),
    
    /**
     * Raza Limousin.
     */
    LIMOUSIN("limousin"),
    
    /**
     * Raza Criollo.
     */
    CRIOLLO("criollo"),
    
    /**
     * Raza Brangus (cruce Brahman x Angus).
     */
    BRANGUS("brangus"),
    
    /**
     * Raza Santa Gertrudis.
     */
    SANTA_GERTRUDIS("santa_gertrudis"),
    
    /**
     * Animal cruzado sin raza definida.
     */
    CRUZADA("cruzada");
    
    private final String value;
    
    /**
     * Constructor del enum.
     * 
     * @param value representación en minúsculas de la raza
     */
    Breed(String value) {
        this.value = value;
    }
    
    /**
     * Obtiene la representación en minúsculas de la raza.
     * 
     * @return valor en minúsculas de la raza
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Convierte un string a un valor del enum.
     * 
     * @param value valor en minúsculas de la raza
     * @return el enum Breed correspondiente
     * @throws IllegalArgumentException si el valor no es válido
     */
    public static Breed fromValue(String value) {
        for (Breed breed : Breed.values()) {
            if (breed.value.equalsIgnoreCase(value)) {
                return breed;
            }
        }
        throw new IllegalArgumentException("Valor de raza inválido: " + value);
    }
}

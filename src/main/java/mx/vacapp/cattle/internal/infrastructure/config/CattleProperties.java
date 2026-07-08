package mx.vacapp.cattle.internal.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades de configuración para el módulo cattle-inventory.
 * Lee valores desde application.yml bajo la clave "vacapp.cattle".
 */
@Configuration
@ConfigurationProperties(prefix = "vacapp.cattle")
@Data
public class CattleProperties {
    
    private Cache cache = new Cache();
    private Validation validation = new Validation();
    private Pagination pagination = new Pagination();
    
    @Data
    public static class Cache {
        /**
         * TTL (Time To Live) en minutos para el caché de estadísticas.
         */
        private int statsTtlMinutes = 15;
        
        /**
         * Tamaño máximo del caché de estadísticas.
         */
        private int statsMaxSize = 1000;
    }
    
    @Data
    public static class Validation {
        /**
         * Longitud mínima del arete (identificación del animal).
         */
        private int areteMinLength = 4;
        
        /**
         * Longitud máxima del arete.
         */
        private int areteMaxLength = 20;
        
        /**
         * Longitud máxima del folio REEMO.
         */
        private int folioReemoMaxLength = 50;
    }
    
    @Data
    public static class Pagination {
        /**
         * Tamaño de página por defecto para listados de ganado.
         */
        private int defaultPageSize = 100;
        
        /**
         * Tamaño máximo de página permitido.
         */
        private int maxPageSize = 500;
    }
}

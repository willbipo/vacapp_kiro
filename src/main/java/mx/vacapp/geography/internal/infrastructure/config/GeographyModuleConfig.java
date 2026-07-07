package mx.vacapp.geography.internal.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuración principal del módulo Geography.
 * 
 * Este módulo gestiona la jerarquía geográfica de terrenos ganaderos:
 * Rancho → Sección → Potrero
 * 
 * Beans públicos del módulo:
 * - GeographyService: API pública para otros módulos
 * 
 * Configuraciones internas:
 * - CacheConfig: Configuración de caching con Caffeine para estadísticas
 * - OpenApiConfig: Documentación OpenAPI para endpoints REST
 * 
 * Flyway migrations:
 * - V1.3__create_geography_tables.sql: Tablas ranchos, secciones, potreros, geography_audit
 * 
 * Características:
 * - Multi-tenancy: Aislamiento por tenant_id
 * - Auditoría: Registro de todas las operaciones de escritura
 * - Caching: Estadísticas de rancho cacheadas por 5 minutos
 * - Validaciones: Superficie, nombres únicos, jerarquía consistente
 */
@Configuration
public class GeographyModuleConfig {
    // Esta clase documenta la configuración del módulo.
    // Los beans específicos están en CacheConfig y OpenApiConfig.
}

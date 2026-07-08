# Configuración del Módulo Cattle-Inventory

Este directorio contiene la configuración del módulo de inventario de ganado bovino (cattle-inventory).

## Archivos de Configuración

### CattleProperties.java

Clase de propiedades que lee la configuración del módulo desde `application.yml` bajo la clave `vacapp.cattle`.

**Propiedades disponibles:**

```yaml
vacapp:
  cattle:
    cache:
      stats-ttl-minutes: 15        # TTL del caché de estadísticas (minutos)
      stats-max-size: 1000         # Tamaño máximo del caché de estadísticas
    
    validation:
      arete-min-length: 4          # Longitud mínima del arete
      arete-max-length: 20         # Longitud máxima del arete
      folio-reemo-max-length: 50   # Longitud máxima del folio REEMO
    
    pagination:
      default-page-size: 100       # Tamaño de página por defecto
      max-page-size: 500           # Tamaño máximo de página permitido
```

**Uso en código:**

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final CattleProperties cattleProperties;
    
    public void example() {
        int minLength = cattleProperties.getValidation().getAreteMinLength();
        int pageSize = cattleProperties.getPagination().getDefaultPageSize();
    }
}
```

## Configuración de Caché

El módulo utiliza **Caffeine** como implementación de caché con la siguiente configuración:

### Nombres de Caché (CacheNames.java)

- `cattleStats`: Caché para estadísticas generales de ganado (GET /api/v1/cattle/stats)
- `animalStats`: Caché para estadísticas de animales individuales

### Configuración de TTL

El TTL (Time To Live) del caché está configurado en **15 minutos** según el requisito 15 del documento de requerimientos:

> THE Cattle_Inventory_Module SHALL cachear estadísticas durante 15 minutos usando @Cacheable con key = "stats:cattle:tenant:{tenantId}:rancho:{ranchoId}"

**Ejemplo de uso en un Use Case:**

```java
@Service
@RequiredArgsConstructor
public class GetAnimalStatsUseCase {
    
    @Cacheable(value = CacheNames.CATTLE_STATS, 
               key = "#tenantId + ':' + #ranchoId")
    public CattleStatsResult execute(UUID tenantId, UUID ranchoId) {
        // Cálculos de estadísticas...
        return result;
    }
}
```

## Configuración de Spring Cache (application.yml)

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=15m
    cache-names:
      - cattleStats
      - animalStats
```

## Logging

El módulo tiene su propia configuración de logging:

```yaml
logging:
  level:
    mx.vacapp.cattle: ${CATTLE_LOG_LEVEL:INFO}
```

Para habilitar logs de debug del módulo cattle en desarrollo:

```bash
export CATTLE_LOG_LEVEL=DEBUG
```

## Variables de Entorno

Las propiedades pueden sobrescribirse mediante variables de entorno:

| Variable | Descripción | Default |
|----------|-------------|---------|
| `CATTLE_LOG_LEVEL` | Nivel de logging del módulo cattle | `INFO` |
| `CACHE_LOG_LEVEL` | Nivel de logging del sistema de caché | `INFO` |

## Perfiles de Spring

### Perfil `dev`

En desarrollo, el caché está habilitado con las mismas configuraciones que producción para pruebas realistas.

### Perfil `prod`

En producción, el caché es crítico para reducir la carga en la base de datos al calcular estadísticas complejas.

## Referencias

- [Requirement 15: Estadísticas e Indicadores](/.kiro/specs/cattle-inventory/requirements.md#requirement-15-estadísticas-e-indicadores)
- [Design Document: Cache Configuration](/.kiro/specs/cattle-inventory/design.md#cache-configuration)
- [Spring Cache Documentation](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Caffeine Cache Documentation](https://github.com/ben-manes/caffeine/wiki)

# Geographic Control Module

## Responsabilidad

El módulo **geographic-control** (paquete `mx.vacapp.geography`) es responsable de gestionar la estructura jerárquica de terrenos ganaderos en Vacapp. Implementa un sistema de tres niveles:

1. **Rancho**: Terreno total del usuario (nivel superior)
2. **Sección**: División opcional para terrenos grandes (nivel intermedio)
3. **Potrero**: Unidad mínima de pastoreo donde se alberga el ganado (nivel inferior)

## Arquitectura

El módulo sigue **Spring Modulith** con **Clean Architecture**:

```
mx.vacapp.geography/
├── GeographyService.java          ← API PÚBLICA (único punto de acceso)
│
└── internal/                      ← TODO lo demás es PRIVADO
    ├── domain/                    ← Lógica de negocio pura
    │   ├── model/                 ← Entidades (POJOs sin JPA)
    │   └── repository/            ← Puertos (interfaces)
    ├── application/               ← Casos de uso
    │   └── usecases/
    └── infrastructure/            ← Adaptadores
        ├── controllers/           ← REST API y Web
        ├── persistence/           ← JPA entities, repositories
        ├── cache/                 ← Configuración de caché
        └── config/                ← Configuración del módulo
```

## API Pública

### GeographyService

Interfaz pública que expone operaciones para otros módulos:

```java
// Validar si un potrero existe y está activo
boolean isPotreroActive(UUID potreroId);

// Obtener el tenant_id de un rancho
Optional<UUID> getRanchoTenantId(UUID ranchoId);

// Verificar capacidad disponible en un potrero
boolean hasCapacity(UUID potreroId, int cantidadGanado);

// Obtener superficie de un potrero
Optional<BigDecimal> getPotreroSurface(UUID potreroId);
```

### Ejemplo de uso desde otro módulo

```java
@Service
@RequiredArgsConstructor
public class CattleService {
    private final GeographyService geographyService;
    
    public void assignCattleToField(UUID potreroId, int count) {
        // Validar que el potrero existe y está activo
        if (!geographyService.isPotreroActive(potreroId)) {
            throw new InvalidFieldException("Potrero no disponible");
        }
        
        // Validar capacidad
        if (!geographyService.hasCapacity(potreroId, count)) {
            throw new InsufficientCapacityException("Potrero sin capacidad");
        }
        
        // Asignar ganado...
    }
}
```

## Endpoints REST

### Ranchos
- `POST /api/v1/geography/ranchos` - Crear rancho
- `GET /api/v1/geography/ranchos` - Listar ranchos (paginado)
- `GET /api/v1/geography/ranchos/{id}` - Obtener rancho
- `PUT /api/v1/geography/ranchos/{id}` - Actualizar rancho
- `DELETE /api/v1/geography/ranchos/{id}` - Archivar rancho
- `GET /api/v1/geography/ranchos/{id}/estadisticas` - Obtener estadísticas

### Secciones
- `POST /api/v1/geography/secciones` - Crear sección
- `GET /api/v1/geography/secciones` - Listar secciones
- `GET /api/v1/geography/secciones/{id}` - Obtener sección
- `PUT /api/v1/geography/secciones/{id}` - Actualizar sección
- `DELETE /api/v1/geography/secciones/{id}` - Archivar sección
- `GET /api/v1/geography/ranchos/{ranchoId}/secciones` - Secciones de un rancho

### Potreros
- `POST /api/v1/geography/potreros` - Crear potrero
- `GET /api/v1/geography/potreros` - Listar potreros
- `GET /api/v1/geography/potreros/{id}` - Obtener potrero
- `PUT /api/v1/geography/potreros/{id}` - Actualizar potrero
- `DELETE /api/v1/geography/potreros/{id}` - Archivar potrero
- `GET /api/v1/geography/ranchos/{ranchoId}/potreros` - Potreros de un rancho
- `GET /api/v1/geography/secciones/{seccionId}/potreros` - Potreros de una sección

## Reglas de Negocio

### Validación de Superficie Jerárquica
La regla **crítica** del módulo: la suma de superficies de hijos nunca debe exceder la superficie del padre.

- **Rancho sin secciones**: `SUM(superficie_potreros) ≤ rancho.superficie_total`
- **Rancho con secciones**: `SUM(superficie_secciones) ≤ rancho.superficie_total`
- **Sección**: `SUM(superficie_potreros) ≤ seccion.superficie`

### Configuraciones Soportadas

**Configuración Simple** (Rancho → Potreros):
```
Rancho (1000 m²)
├── Potrero A (300 m²)
├── Potrero B (400 m²)
└── Potrero C (250 m²)
```

**Configuración Compleja** (Rancho → Secciones → Potreros):
```
Rancho (10000 m²)
├── Sección Norte (4000 m²)
│   ├── Potrero N1 (1500 m²)
│   └── Potrero N2 (2000 m²)
└── Sección Sur (5000 m²)
    ├── Potrero S1 (2500 m²)
    └── Potrero S2 (2000 m²)
```

**Importante**: Una vez que un rancho tiene secciones, NO se pueden crear potreros directamente vinculados al rancho.

### Archivado (Soft Delete)

- No hay eliminación física de entidades
- El archivado cambia el status a `ARCHIVED`
- Reglas de archivado:
  - **Rancho**: No puede tener secciones ni potreros activos
  - **Sección**: No puede tener potreros activos
  - **Potrero**: No puede tener ganado asignado (`cattleCount > 0`)

### Multi-Tenancy

- Todos los datos están aislados por `tenant_id`
- Los repositorios filtran automáticamente por el tenant del usuario autenticado
- No es posible acceder a recursos de otro tenant (error 403)

## Caching

Las estadísticas de ranchos se cachean durante 5 minutos usando Caffeine:

- Cache key: `stats:rancho:{ranchoId}:tenant:{tenantId}`
- TTL: 5 minutos
- Invalidación automática al modificar superficies

## Base de Datos

### Tablas
- `ranchos`: Almacena terrenos principales
- `secciones`: Divisiones opcionales de ranchos
- `potreros`: Unidades mínimas de pastoreo
- `geography_audit`: Log de auditoría de cambios

### Índices Principales
- `(tenant_id)` en todas las tablas
- `(rancho_id, tenant_id)` para lookups jerárquicos
- `(seccion_id, tenant_id)` para potreros de sección
- `(status)` para filtrado por estado

## Dependencias

### Módulos Requeridos
- `users` - Para autenticación JWT y contexto de tenant

### Módulos Dependientes (futuros)
- `cattle` - Gestión de ganado (usa validación de potreros)
- `production` - Eventos de producción vinculados a ubicaciones
- `health` - Eventos de salud vinculados a potreros

## Testing

- **Unit tests**: Entidades de dominio y casos de uso
- **Integration tests**: Repositorios y controladores
- **E2E tests**: Flujos completos de creación jerárquica
- **Property-based tests**: Validación matemática de superficies
- **Performance tests**: Cálculo de estadísticas con grandes datasets

## Documentación OpenAPI

La especificación OpenAPI completa está disponible en:
- YAML: `src/main/resources/openapi/openapi-geography.yaml`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- JSON: `http://localhost:8080/v3/api-docs`

## Configuración

Variables de entorno requeridas:
```properties
# Base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/vacapp
spring.datasource.username=vacapp_user
spring.datasource.password=***

# Cache (Caffeine)
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=5m
```

## Notas de Implementación

- Nombres de clases/métodos/variables en **inglés** (backend)
- Comentarios en **español**
- DTOs como **Java Records**
- Inyección de dependencias por **constructor** (Lombok `@RequiredArgsConstructor`)
- Validación con **Bean Validation** solo en DTOs de Request
- OpenAPI **Design-First**: YAML primero, luego implementación
- Sin `@Autowired` en campos
- Sin mezclar capas: entidades JPA != entidades de dominio

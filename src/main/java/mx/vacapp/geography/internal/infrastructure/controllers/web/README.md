# Web Controllers - Geography Module (FASE 5)

## Descripción

Esta carpeta contiene los **controladores web MVC** del módulo geographic-control, implementados con **Spring MVC + Thymeleaf** para renderizar vistas HTML del lado del servidor.

## Arquitectura

```
web/
├── dtos/                           ← Form DTOs con Bean Validation
│   ├── RanchoFormDto.java
│   ├── SeccionFormDto.java
│   └── PotreroFormDto.java
├── RanchoWebController.java        ← Controlador web de ranchos
├── SeccionWebController.java       ← Controlador web de secciones
└── PotreroWebController.java       ← Controlador web de potreros
```

## Controladores Implementados

### RanchoWebController

**Rutas:**
- `GET /geography/ranchos` → Lista de ranchos con paginación
- `GET /geography/ranchos/{id}` → Detalle de rancho con tabs (Info, Secciones, Potreros, Estadísticas)
- `GET /geography/ranchos/nuevo` → Formulario de creación
- `POST /geography/ranchos` → Procesar creación
- `GET /geography/ranchos/{id}/editar` → Formulario de edición
- `POST /geography/ranchos/{id}` → Procesar edición

### SeccionWebController

**Rutas:**
- `GET /geography/secciones` → Lista de secciones (opcionalmente filtradas por rancho)
- `GET /geography/secciones/{id}` → Detalle de sección
- `GET /geography/secciones/nuevo` → Formulario con selector de rancho
- `POST /geography/secciones` → Procesar creación
- `GET /geography/secciones/{id}/editar` → Formulario de edición
- `POST /geography/secciones/{id}` → Procesar edición

### PotreroWebController

**Rutas:**
- `GET /geography/potreros` → Lista de potreros en tabla (filtros por rancho/sección)
- `GET /geography/potreros/{id}` → Detalle con warning si tiene ganado
- `GET /geography/potreros/nuevo` → Formulario con selectores cascada (rancho → sección)
- `POST /geography/potreros` → Procesar creación
- `GET /geography/potreros/{id}/editar` → Formulario de edición
- `POST /geography/potreros/{id}` → Procesar edición

## Form DTOs

Todos los DTOs de formulario:
- Son **Java Records** inmutables
- Incluyen **Bean Validation** (`@NotNull`, `@NotBlank`, `@Size`, `@DecimalMin`, `@DecimalMax`, `@Digits`)
- Validación del lado del servidor antes de ejecutar Use Cases
- Errores mostrados en formularios con `BindingResult` y `th:errors`

## Plantillas Thymeleaf

### Ubicación
`src/main/resources/templates/geography/`

### Estructura

```
geography/
├── ranchos/
│   ├── lista.html          ← Grid de cards con búsqueda
│   ├── detalle.html        ← Vista con tabs (Info, Secciones, Potreros, Stats)
│   └── formulario.html     ← Formulario con validación HTML5
├── secciones/
│   ├── lista.html          ← Cards agrupadas por rancho
│   ├── detalle.html        ← Info + potreros de la sección
│   └── formulario.html     ← Selector de rancho
├── potreros/
│   ├── lista.html          ← Tabla con columnas (nombre, superficie, rancho, sección, ganado)
│   ├── detalle.html        ← Info + warning de ganado
│   └── formulario.html     ← Selectores cascada (rancho → sección)
└── fragments/
    ├── geo-card.html       ← Componente card reutilizable
    ├── geo-tree.html       ← Árbol jerárquico expandible
    └── geo-stats.html      ← Panel de estadísticas con gráficos CSS
```

## Características Implementadas

### 1. JavaScript Vanilla Integrado
- **Sin frameworks** (sin React, Vue, Angular)
- **Sin Node.js** ni npm
- Todo el código JS integrado en `<script>` dentro de cada HTML
- **Fetch API** para consumir endpoints REST
- **Token JWT** desde `sessionStorage.getItem('vacapp_token')`

### 2. Búsqueda en Tiempo Real
- **Debounce de 300ms** para evitar búsquedas excesivas
- Filtrado client-side por nombre
- Atributo `data-nombre` en elementos para búsqueda

### 3. Tabs Dinámicos (Vista Detalle Rancho)
- Tabs: Información General, Secciones, Potreros, Estadísticas
- Carga lazy de datos al hacer clic en tab
- JavaScript vanilla para mostrar/ocultar contenido

### 4. Validación de Formularios
- **HTML5 validation**: `required`, `minlength`, `maxlength`, `min`, `max`, `step`
- **Bean Validation** del lado del servidor
- Contador de caracteres para `textarea` (descripción)
- Validación JavaScript adicional antes de enviar

### 5. Selectores en Cascada (Formulario Potrero)
- Selector de Rancho → carga Secciones dinámicamente
- Fetch API para obtener secciones del rancho seleccionado
- Permite crear potrero directo al rancho (sin sección)

### 6. Warnings de Ganado
- Vista de detalle de Potrero muestra warning si `cattleCount > 0`
- Validación antes de archivar: no permitir si tiene ganado asignado

## Estilos CSS

### Ubicación
`src/main/resources/static/css/geography/`

### Archivos Creados

| Archivo | Descripción |
|---|---|
| `ranchos.css` | Estilos base (grid, cards, tabs, formularios, alertas) |
| `secciones.css` | Importa `ranchos.css` + ajustes para secciones |
| `potreros.css` | Importa `ranchos.css` + tabla + badges de ganado |
| `geo-tree.css` | Árbol jerárquico expandible/colapsable |
| `geo-stats.css` | Panel de estadísticas con gráficos de barras CSS |

### Características CSS

- **CSS Vanilla** (sin preprocesadores, sin Sass/Less)
- **Variables CSS Custom** en `:root` para colores, espaciados, bordes
- **CSS Grid** para layouts responsivos
- **Responsive** con `@media (max-width: 768px)`
- **Animaciones CSS**: `fadeIn`, `slideIn`, `translateY`
- **Gráficos de barras** con `<div>` y `width: X%` (sin librerías)

### Variables CSS Principales

```css
:root {
    --color-primary: #2563eb;
    --color-success: #10b981;
    --color-warning: #f59e0b;
    --color-danger: #ef4444;
    --bg-main: #f9fafb;
    --bg-card: #ffffff;
    --text-primary: #111827;
    --border-color: #e5e7eb;
    --spacing-md: 1rem;
    --radius-md: 0.5rem;
}
```

## Fragmentos Thymeleaf Reutilizables

### geo-card.html
**Uso:**
```html
<div th:replace="~{geography/fragments/geo-card :: geo-card(${entity}, 'rancho', true)}"></div>
```

**Parámetros:**
- `entity`: objeto con campos (id, nombre, superficie, status)
- `type`: 'rancho', 'seccion', 'potrero'
- `showLink`: boolean para mostrar botón "Ver Detalle"

### geo-tree.html
**Uso:**
```html
<div th:replace="~{geography/fragments/geo-tree :: geo-tree(${ranchoId}, ${ranchoNombre})}"></div>
```

**Características:**
- Árbol expandible/colapsable con botones `▶`/`▼`
- Carga dinámica de secciones y potreros via Fetch API
- Íconos: 🏠 Rancho, 📂 Sección, 🌾 Potrero

### geo-stats.html
**Uso:**
```html
<div th:replace="~{geography/fragments/geo-stats :: geo-stats(${stats}, 'rancho')}"></div>
```

**Métricas:**
- Total secciones/potreros
- Superficie total/usada/disponible
- Porcentaje de uso con barra visual
- Distribución por sección (gráfico de barras)
- Leyenda de colores: OK (0-75%), Warning (75-90%), Crítico (>90%)

## Flujo de Datos

```
Vista HTML (Thymeleaf)
  ↓
Controller Web (@Controller)
  ↓
Form DTO + @Valid + BindingResult
  ↓
UseCase (Application Layer)
  ↓
Command → Domain → Repository
  ↓
Result → Mapper → Model (Thymeleaf)
  ↓
Vista HTML renderizada
```

## Integración con API REST

Algunas funcionalidades consumen la API REST desde JavaScript:

**Ejemplo: Cargar Secciones de un Rancho**
```javascript
const token = sessionStorage.getItem('vacapp_token');
const response = await fetch(`/api/v1/geography/ranchos/${ranchoId}/secciones`, {
    headers: {
        'Authorization': `Bearer ${token}`
    }
});
const secciones = await response.json();
```

**Endpoints Consumidos:**
- `GET /api/v1/geography/ranchos/{id}/secciones`
- `GET /api/v1/geography/ranchos/{id}/potreros`
- `GET /api/v1/geography/secciones/{id}/potreros`
- `GET /api/v1/geography/ranchos/{id}/estadisticas`
- `DELETE /api/v1/geography/ranchos/{id}` (archivar)
- `DELETE /api/v1/geography/secciones/{id}` (archivar)
- `DELETE /api/v1/geography/potreros/{id}` (archivar)

## Validaciones Implementadas

### Lado del Cliente (HTML5 + JavaScript)
- `required`: Campos obligatorios
- `minlength="2"`, `maxlength="100"`: Longitud de nombre
- `min="0.01"`, `max="999999999.99"`: Rango de superficie
- `step="0.01"`: Decimales para superficie
- Validación de formulario antes de enviar con `addEventListener('submit')`

### Lado del Servidor (Bean Validation)
- `@NotNull`: Campo no nulo
- `@NotBlank`: String no vacío ni solo espacios
- `@Size(min, max)`: Longitud de cadena
- `@DecimalMin`, `@DecimalMax`: Rango de números
- `@Digits(integer, fraction)`: Precisión de BigDecimal

### Validaciones de Negocio (Use Cases)
- Nombre único dentro del tenant (case-insensitive)
- Suma de superficies de hijos <= superficie del padre
- No archivar entidad con hijos activos
- No archivar potrero con ganado asignado (`cattleCount > 0`)

## Mensajes Flash

Usando `RedirectAttributes`:
```java
redirectAttributes.addFlashAttribute("successMessage", "Rancho creado exitosamente");
```

Mostrados en vistas con:
```html
<div th:if="${successMessage}" class="alert alert-success" th:text="${successMessage}"></div>
```

## TODO

- [ ] Extraer `tenantId` y `userId` del contexto de seguridad (actualmente mock con UUIDs hardcodeados)
- [ ] Integrar con módulo de autenticación para obtener JWT token real
- [ ] Implementar paginación real con `Pageable` en `ListRanchosUseCase`
- [ ] Añadir tests E2E con Selenium para validar flujos completos
- [ ] Integración con módulo `cattle` para mostrar ganado asignado en potreros
- [ ] Implementar soft deletes con confirmación modal (actualmente comentado)

## Convenciones de Nomenclatura

✅ **Correcto (ESPAÑOL para frontend):**
- Carpetas: `geography/`, `ranchos/`, `secciones/`, `potreros/`
- Archivos: `lista.html`, `detalle.html`, `formulario.html`
- CSS: `ranchos.css`, `secciones.css`, `potreros.css`
- JavaScript: Variables en español dentro de `<script>`

❌ **Incorrecto:**
- NO usar inglés en nombres de archivos frontend: `list.html`, `detail.html`, `form.html`
- NO usar carpetas en inglés: `geo/`, `ranches/`, `sections/`, `paddocks/`

## Referencias

- **AGENTS.md**: Reglas generales del proyecto
- **tasks.md**: Task 5.1-5.9 implementadas
- **requirements.md**: Requirement 7 (Interfaz Web)
- **Thymeleaf Docs**: https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html
- **CSS Grid**: https://developer.mozilla.org/es/docs/Web/CSS/CSS_Grid_Layout
- **Fetch API**: https://developer.mozilla.org/es/docs/Web/API/Fetch_API

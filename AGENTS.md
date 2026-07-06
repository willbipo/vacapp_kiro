# AGENTS.md — Vacapp

> Instrucciones siempre activas para cualquier agente de IA (GitHub Copilot, Claude Code, Codex CLI).
> Complementa este archivo con la referencia de arquitectura: `.github/instructions/arquitecture.instructions.md`

---

## Identidad del Proyecto

- **Nombre**: Vacapp
- **Dominio**: Gestión ganadera SaaS
- **Tipo**: Monolito Modular — Spring Modulith + Clean Architecture
- **Stack**: Java 21 + Spring Boot 4.1.0 + Spring Modulith + MySQL + Spring Data JDBC + Spring Security (JWT) + Lombok + Thymeleaf + CSS Vanilla

---

## Reglas Generales

1. **Idioma — Backend**: Nombres de clases, interfaces, métodos, variables, campos, paquetes y archivos Java en **inglés**. Comentarios en español. Las palabras clave de Java/Spring (`class`, `public`, `@Service`, etc.) permanecen en inglés por defecto.
2. **Idioma — Frontend**: Carpetas, archivos HTML/CSS, variables JavaScript y comentarios en **español**. No aplica la regla de inglés.
3. **Arquitectura obligatoria**: Todo módulo nuevo sigue la estructura Spring Modulith. Cada módulo expone una única **API pública** (`ModuleService.java`) en su raíz y oculta todo el resto bajo `internal/`.
4. **Encapsulamiento `internal/`**: Ningún otro módulo puede importar clases de `internal/`. Solo se puede usar la API pública del módulo.
5. **Sin mezclar capas**: Nunca usar `@Entity` JPA en `internal/domain/model/`. Siempre usar `Mapper` en `infrastructure/persistence/` para transformar entre capas.
6. **Sin `@Autowired` en campos**: Toda inyección de dependencias por constructor (Lombok `@RequiredArgsConstructor` + campos `final`).
7. **DTOs como Records**: Todos los DTOs de Request/Response deben ser Java Records.
8. **Multi-tenancy**: Todo repositorio JPA debe filtrar por `tenant_id` extraído del contexto de seguridad.
9. **Respuestas HTTP**: Los controladores retornan `ResponseEntity<T>` con código HTTP semánticamente correcto.
10. **Validación**: Anotaciones Bean Validation (`@NotNull`, `@Size`, etc.) solo en DTOs de Request (en `infrastructure/controllers/*/dtos/`), nunca en entidades de dominio.
11. **Swagger Design-First (YAML-First)**:
    - Todo endpoint REST en `internal/infrastructure/controllers/mobile/` se define en un YAML en `src/main/resources/openapi/openapi-[modulo].yaml` (NO anotaciones Swagger en código Java).
    - DTOs Request/Response son Records sin `@Schema`. Las definiciones viven en los YAMLs.
    - Los controllers implementan interfaces generadas por `openapi-generator-maven-plugin` a partir de los YAMLs.
    - **Workflow**: (1) Modificar YAML, (2) Ejecutar `mvn compile` para regenerar interfaces, (3) Implementar cambios en el controller.
    - No hay `@GetMapping`, `@PostMapping`, `@PutMapping` en controllers. Las rutas están en el YAML.
    - El Swagger UI en `/swagger-ui/index.html` se actualiza automáticamente al compilar.
12. **No sobre-ingeniería**: Solo implementar lo que se solicita explícitamente. No añadir features no pedidas.

---

## Estructura de un Módulo (Spring Modulith)

```
com.vacapp/
│
├── [Module]/                          ← Raíz del módulo (ej. users, cattle, health)
│   ├── [Module]Service.java           ← API PÚBLICA: único punto de entrada para otros módulos
│   │
│   └── internal/                      ← PRIVADO: inaccesible para otros módulos
│       ├── domain/
│       │   ├── model/                 ← Entidades de negocio puras (sin JPA, sin Spring)
│       │   └── repository/            ← Puertos de salida (interfaces)
│       │
│       ├── application/
│       │   └── usecases/              ← Casos de uso (orquestación, sin DTOs de infraestructura)
│       │
│       └── infrastructure/
│           ├── controllers/
│           │   ├── web/               ← Controladores MVC (HTML/Thymeleaf)
│           │   │   └── dtos/          ← Form DTOs (Records)
│           │   └── mobile/            ← Controladores REST API (JSON/JWT)
│           │       └── dtos/          ← Request/Response Records
│           ├── persistence/           ← @Entity JPA, JpaRepository, Impl, Mapper
│           └── config/                ← Beans de configuración del módulo
```

## Flujo de una Petición

```
HTTP Request
  → Controller (internal/infrastructure/controllers/mobile/ o web/)   ← valida DTO con @Valid, mapea a comando
  → UseCase    (internal/application/usecases/)                       ← orquesta lógica de negocio
  → Repository Port (internal/domain/repository/)                     ← interfaz pura
  → JPA Impl   (internal/infrastructure/persistence/)                 ← filtra por tenant_id
  → Base de datos MySQL
```

---

## Frontend Thymeleaf + HTML + CSS + JavaScript Vanilla

El frontend es **server-rendered** con Thymeleaf, sin frameworks JavaScript complejos:

- **Motor de plantillas**: Thymeleaf — archivos en `src/main/resources/templates/`
- **Estilos**: CSS vanilla con variables custom en `src/main/resources/static/css/`
- **JavaScript**: Vanilla integrado en HTML (sin Node, sin npm, sin React, sin Tailwind)
- **Token JWT**: Guardado en `sessionStorage` tras login
- **API**: Consumida con Fetch API desde JavaScript vanilla

### Estructura de vistas

```
templates/
├── auth/
│   ├── login.html        ← Formulario de login con JS integrado
│   └── logout.html       ← Confirmación de logout
├── dashboard/
│   └── index.html        ← Dashboard principal
└── fragments/            ← Componentes reutilizables
    ├── navbard.html
    └── sidebard.html

static/css/
├── global.css            ← Estilos globales (reset, variables CSS)
├── login.css
├── dashboard.css
├── navbard.css
└── sidebard.css
```

### Reglas clave

- **Nombres de archivos y carpetas en español**
- **JavaScript integrado**: Todo código JS dentro de `<script>` al final del HTML
- **Thymeleaf helpers**: `th:href`, `th:action`, `th:text` para URLs dinámicas
- **Sin frameworks**: HTML + CSS + JS vanilla, sin React/Vue/Angular
- **Sin preprocesadores CSS**: Usar CSS vanilla o variables CSS custom

---

## Convenciones de Nomenclatura (Backend — en inglés)

| Artefacto | Convención | Ejemplo |
|---|---|---|
| Módulo raíz | `ModuleService.java` (API pública) | `UsersService.java` |
| Entidad de dominio | `ModelName.java` | `Animal.java` |
| Excepción de dominio | `NameException.java` | `InvalidCredentialsException.java` |
| Puerto (interfaz) | `NameRepository.java` | `AnimalRepository.java` |
| Entidad JPA | `NameEntity.java` | `AnimalEntity.java` |
| Repositorio JPA | `NameJpaRepository.java` | `AnimalJpaRepository.java` |
| Caso de uso | `VerbNameUseCase.java` | `RegisterAnimalUseCase.java` |
| Comando (record) | `NameCommand.java` | `RegisterAnimalCommand.java` |
| Resultado (record) | `NameResult.java` | `AuthResult.java` |
| DTO entrada | `NameRequest.java` | `RegisterAnimalRequest.java` |
| DTO salida | `NameResponse.java` | `AnimalResponse.java` |
| Mapper | `NameMapper.java` | `AnimalMapper.java` |
| Controlador REST | `NameRestController.java` | `AnimalRestController.java` |
| Controlador Web | `NameWebController.java` | `AuthWebController.java` |
| Ruta base API | `/api/v1/plural-name` | `/api/v1/animals` |

---

## Referencias Completas

- **Arquitectura detallada con patrones de código**: Ver [`.github/instructions/arquitecture.instructions.md`](.github/instructions/arquitecture.instructions.md)
- **Reglas de encapsulación, estructura de módulos, patrones Double DTO, y ejemplos ejecutables**: Ver la guía de arquitectura
- **Prompts SDD para scaffolding rápido**: Ver [`.github/prompts/`](.github/prompts/)

---

## Arquitectura del Frontend Thymeleaf

Cada vista es un archivo `.html` con:

1. **Head**: Links a CSS en `static/css/`
2. **Body**: HTML semántico con `xmlns:th="http://www.thymeleaf.org"`
3. **Script**: JavaScript vanilla al final para interactividad

Ejemplo estructura:

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head>
  <link rel="stylesheet" th:href="@{/css/global.css}" />
  <link rel="stylesheet" th:href="@{/css/login.css}" />
</head>
<body>
  <!-- Contenido HTML semántico -->
  <form id="formulario-login">
    <input type="text" id="username" placeholder="Usuario" />
    <input type="password" id="password" placeholder="Contraseña" />
    <button type="submit">Iniciar sesión</button>
  </form>

  <script>
    // JavaScript vanilla integrado
    document.getElementById('formulario-login').addEventListener('submit', async (e) => {
      e.preventDefault();
      const res = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      if (res.ok) {
        const data = await res.json();
        sessionStorage.setItem('vacapp_token', data.token);
        window.location.href = '/dashboard';
      }
    });
  </script>
</body>
</html>
```

---

## Cómo Usar los Prompts (SDD)

1. En el chat de Copilot, escribe `/` para ver los prompts disponibles.
2. Usa `/crear-modulo` para generar el scaffolding completo de un nuevo módulo (ej. `ganado`, `salud`).
3. Usa `/crear-feature` para añadir un caso de uso a un módulo existente.
4. Siempre revisa y ajusta el código generado antes de hacer commit.

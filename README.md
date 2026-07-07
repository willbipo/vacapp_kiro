# Vacapp - Gestión Ganadera SaaS

> Plataforma integral para gestión de ganado bovino, geografía territorial y salud animal en operaciones ganaderas profesionales.

![Java](https://img.shields.io/badge/Java-21-blue?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 🎯 Descripción

Vacapp es una aplicación monolítica modular (Spring Modulith) diseñada para ganaderos profesionales. Proporciona:

- ✅ **Gestión de Usuarios** - Autenticación JWT, autorización basada en roles, auditoría
- ✅ **Control Geográfico** - Ranchos, secciones, potreros con jerarquía completa
- ✅ **Inventario de Ganado** - Identificación única, genealogía, trazabilidad (próxima fase)
- ✅ **Salud Animal** - Eventos médicos, vacunaciones, diagnósticos (próxima fase)

---

## 🚀 Inicio Rápido

### Opción 1: Automatizado (Recomendado)

#### macOS / Linux
```bash
chmod +x setup.sh
./setup.sh
./mvnw spring-boot:run
```

#### Windows
```bash
setup.bat
mvnw.cmd spring-boot:run
```

### Opción 2: Manual

```bash
# 1. Crear base de datos
mysql -u root -p < scripts/setup_db.sql

# 2. Compilar
./mvnw clean compile

# 3. Ejecutar
./mvnw spring-boot:run
```

**Acceso**: http://localhost:8080

Para instrucciones detalladas, ver [QUICKSTART.md](QUICKSTART.md)

---

## 📋 Requisitos Previos

- **Java 21+**
- **MySQL 8.0+**
- **Maven 3.8+** (incluido con `mvnw`)

---

## 🗄️ Base de Datos

| Parámetro | Valor |
|-----------|-------|
| **Database** | `vacapp` |
| **Usuario** | `vacapp_user` |
| **Contraseña** | `vacapp_password` |
| **Port** | `3306` |

**Crear manualmente** (si el setup automático falla):
```sql
CREATE DATABASE vacapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'vacapp_user'@'localhost' IDENTIFIED BY 'vacapp_password';
GRANT ALL PRIVILEGES ON vacapp.* TO 'vacapp_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## 📊 Arquitectura

```
┌─────────────────────────────────────┐
│     Spring Boot Application         │
│  (Port 8080, JWT Auth)              │
└──────────────┬──────────────────────┘
               │
        ┌──────┴──────┐
        │             │
    ┌───▼─────┐  ┌───▼──────┐
    │  Users  │  │ Geography│
    │ Module  │  │ Module   │
    └─────────┘  └──────────┘
        │             │
    ┌───▼──────────────▼────┐
    │   MySQL Database      │
    │   (vacapp)            │
    └───────────────────────┘
```

### Módulos

| Módulo | Responsabilidad | API |
|--------|-----------------|-----|
| **users** | Auth, usuarios, auditoría | `/api/v1/auth/*`, `/api/v1/users/*` |
| **geography** | Ranchos, secciones, potreros | `/api/v1/geography/*` |
| **cattle** | Inventario (próximo) | `/api/v1/cattle/*` |

---

## 🛣️ URLs Principales

| Función | URL |
|---------|-----|
| **Web Frontend** | http://localhost:8080 |
| **Login** | http://localhost:8080/auth/login |
| **Dashboard** | http://localhost:8080/dashboard |
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **API Docs (JSON)** | http://localhost:8080/api-docs |
| **Admin Users** | http://localhost:8080/admin/usuarios |
| **Geography** | http://localhost:8080/geography/ranchos |

---

## 🧪 Tests

```bash
# Tests unitarios (sin Docker - 97 tests)
./mvnw test -Dtest='!*IntegrationTest'

# Todos los tests (requiere Docker)
./mvnw test

# Tests específicos
./mvnw test -Dtest='mx.vacapp.users.**'
./mvnw test -Dtest='*PropertiesTest'
```

---

## 📁 Estructura de Carpetas

```
vacapp/
├── src/
│   ├── main/
│   │   ├── java/mx/vacapp/
│   │   │   ├── geography/          # Módulo Geographic Control
│   │   │   │   └── internal/       # (Privado: domain, application, infrastructure)
│   │   │   ├── users/              # Módulo User Management
│   │   │   │   └── internal/       # (Privado)
│   │   │   └── infrastructure/     # Infraestructura compartida
│   │   ├── resources/
│   │   │   ├── application.yml     # Configuración principal
│   │   │   ├── db/migration/       # Scripts Flyway
│   │   │   ├── openapi/            # Especificaciones OpenAPI
│   │   │   ├── templates/          # Vistas Thymeleaf
│   │   │   └── static/             # CSS, JS, imágenes
│   │   └── test/                   # Tests
│   └── test/java/mx/vacapp/        # Tests
├── .kiro/                          # Configuración Kiro
│   ├── specs/                      # Especificaciones (SDD)
│   └── SETUP_GUIDE.md              # Guía detallada
├── pom.xml                         # Dependencias Maven
├── mvnw / mvnw.cmd                 # Maven Wrapper
├── setup.sh / setup.bat            # Scripts de setup
├── QUICKSTART.md                   # Inicio rápido
└── README.md                       # Este archivo
```

---

## 🔒 Seguridad

### Autenticación
- **JWT**: Token-based authentication con HMAC-SHA256
- **Expiración**: 24 horas (configurable)
- **Roles**: SUPER_ADMIN, SUPPORT, ADMIN, MANAGER, VETERINARIAN, WORKER

### Multi-Tenancy
- Cada usuario asignado a un tenant
- Filtrado automático de datos por tenant
- Aislamiento completo de datos

### Auditoría
- Registro de todas las operaciones (CREATE, UPDATE, DELETE)
- Historial completo con usuario, fecha y cambios
- Retención: 730 días (configurable)

---

## 📚 Documentación

| Documento | Contenido |
|-----------|-----------|
| [QUICKSTART.md](QUICKSTART.md) | Inicio rápido (5 minutos) |
| [.kiro/SETUP_GUIDE.md](.kiro/SETUP_GUIDE.md) | Guía detallada de instalación |
| [AGENTS.md](AGENTS.md) | Reglas de arquitectura |
| [API Docs](#) | http://localhost:8080/swagger-ui.html |

---

## 🛠️ Comandos Útiles

```bash
# Compilar
./mvnw clean compile

# Compilar + tests
./mvnw clean install

# Ejecutar aplicación
./mvnw spring-boot:run

# Ejecutar con perfil de producción
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"

# Compilar JAR
./mvnw clean package -DskipTests

# Ejecutar JAR
java -jar target/vacapp-0.0.1-SNAPSHOT.jar

# Tests unitarios
./mvnw test -Dtest='!*IntegrationTest'

# Limpiar caché
./mvnw clean

# Ver versión
./mvnw -version
```

---

## 🌍 Variables de Entorno

Ver `.env.example` para lista completa.

```bash
# Base de Datos
DATABASE_URL=jdbc:mysql://localhost:3306/vacapp?useSSL=false&serverTimezone=UTC
DATABASE_USERNAME=vacapp_user
DATABASE_PASSWORD=vacapp_password

# JWT (cambiar en producción)
JWT_SECRET=vacapp-default-secret-key-minimum-256-bits-32-bytes-change-in-production

# Servidor
SERVER_PORT=8080

# Perfil
SPRING_PROFILES_ACTIVE=dev  # dev o prod
```

---

## 🐛 Solución de Problemas

| Problema | Solución |
|----------|----------|
| "No suitable driver" | Verifica que MySQL está corriendo |
| "Access denied" | Verifica credenciales en `application.yml` o `.env` |
| "Port 8080 already in use" | Cambia el puerto: `--server.port=8081` |
| "logs/ not found" | Crea el directorio: `mkdir -p logs` |

Para más soluciones, ver [.kiro/SETUP_GUIDE.md](.kiro/SETUP_GUIDE.md)

---

## 📝 Funcionalidades Implementadas

### ✅ Fase 1: User Management
- Autenticación JWT completa
- Gestión de usuarios con roles
- Auditoría de operaciones
- Tests unitarios (97 tests ✓)
- Property-based tests (10 propiedades ✓)

### ✅ Fase 2: Geographic Control
- Gestión de ranchos, secciones, potreros
- Cálculo de superficies
- Auditoría de cambios geográficos
- APIs REST y Web (Thymeleaf)
- Caché de estadísticas

### ⏳ Próximas Fases
- **Cattle Inventory**: Registro de ganado (arete, genealogía, movimientos)
- **Health Management**: Vacunaciones, diagnósticos, eventos de salud
- **Analytics & Reporting**: Dashboards, exportación de datos

---

## 🤝 Contribuciones

Las contribuciones están limitadas al equipo de desarrollo. Para reportar bugs o sugerencias, contacta al equipo.

---

## 📄 Licencia

MIT License - Ver LICENSE.md

---

## 📞 Contacto

- **Email**: info@vacapp.mx
- **Soporte**: support@vacapp.mx

---

## 🎓 Stack Técnico

| Componente | Versión |
|-----------|---------|
| Java | 21 |
| Spring Boot | 4.1.0 |
| Spring Modulith | 2.1.0 |
| MySQL | 8.0+ |
| JWT | 0.12.6 |
| Lombok | 1.18.46 |
| Thymeleaf | Included |
| Swagger/OpenAPI | 3.0 |

---

**Última actualización**: Julio 7, 2026


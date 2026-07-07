# 🚀 Vacapp - Quick Start Guide

## ⚡ Inicio Rápido (5 minutos)

### 1. Prerequisitos
- **Java 21+**: `java -version`
- **MySQL 8.0+**: `mysql --version`
- **Git**: `git --version`

### 2. Crear Base de Datos
```bash
mysql -u root -p
```

```sql
-- Copiar y pegar en MySQL:
CREATE DATABASE vacapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'vacapp_user'@'localhost' IDENTIFIED BY 'vacapp_password';
GRANT ALL PRIVILEGES ON vacapp.* TO 'vacapp_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 3. Ejecutar la Aplicación
```bash
# Opción A: Con Maven (recomendado)
./mvnw spring-boot:run

# Opción B: Compilar y ejecutar JAR
./mvnw clean package -DskipTests
java -jar target/vacapp-0.0.1-SNAPSHOT.jar
```

### 4. Acceder a la Aplicación
```
✓ Web: http://localhost:8080
✓ API Docs: http://localhost:8080/swagger-ui.html
✓ Login: http://localhost:8080/auth/login
```

---

## 📊 Arquitectura

**Stack**: Java 21 + Spring Boot 4.1 + Spring Modulith + MySQL + JWT

**Módulos**:
- `users` - Autenticación, autorización, gestión de usuarios
- `geography` - Gestión de ranchos, secciones y potreros

**Modelos**: Clean Architecture + Spring Modulith (Domain → Application → Infrastructure)

---

## 📁 Estructura de Carpetas

```
src/main/
├── java/mx/vacapp/
│   ├── geography/          # Módulo Geographic Control
│   │   └── internal/       # Privado (domain, application, infrastructure)
│   └── users/              # Módulo User Management
│       └── internal/       # Privado
├── resources/
│   ├── application.yml     # Configuración
│   ├── db/migration/       # Scripts SQL (Flyway)
│   ├── openapi/            # Especificaciones REST
│   ├── templates/          # Vistas HTML (Thymeleaf)
│   └── static/             # CSS, JS, imágenes
└── test/                   # Tests unitarios
```

---

## 🗄️ Base de Datos

| Parámetro | Valor |
|-----------|-------|
| **Database** | `vacapp` |
| **Usuario** | `vacapp_user` |
| **Contraseña** | `vacapp_password` |
| **Host** | `localhost` |
| **Puerto** | `3306` |

---

## 🧪 Ejecutar Tests

```bash
# Tests unitarios (sin Docker)
./mvnw test -Dtest='!*IntegrationTest'

# Resultado esperado: 97 tests ✓
```

---

## 📝 Configuración

**Variables de Entorno** (ver `.env.example`):
```bash
DATABASE_URL=jdbc:mysql://localhost:3306/vacapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DATABASE_USERNAME=vacapp_user
DATABASE_PASSWORD=vacapp_password
JWT_SECRET=vacapp-default-secret-key-minimum-256-bits-32-bytes-change-in-production
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

**Perfiles**:
- `dev` - Desarrollo (logs DEBUG)
- `prod` - Producción (logs INFO)

---

## 🔑 Crear Usuario de Prueba

### Credenciales Predefinidas
- **Email**: `admin@vacapp.mx`
- **Contraseña**: `Admin123456!`
- **Rol**: Administrador
- **Nota**: Este usuario se crea automáticamente en la migración `V5__insert_test_admin_user.sql`

### Vía API REST
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@vacapp.com","password":"AdminPassword123!"}'
```

### Vía Web UI
1. Ve a http://localhost:8080/auth/login
2. Haz clic en "Crear Nueva Cuenta"
3. Completa el formulario de registro

---

## 🐛 Troubleshooting

| Error | Solución |
|-------|----------|
| "No suitable driver" | MySQL no está corriendo |
| "Access denied" | Verifica credenciales en `application.yml` |
| "Port 8080 already in use" | `./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"` |
| "logs/ not found" | `mkdir -p logs` |

---

## 📚 Documentación Completa

Para instrucciones detalladas, ver `.kiro/SETUP_GUIDE.md`

---

## 🎯 URLs Principales

| Función | URL |
|---------|-----|
| **Home** | http://localhost:8080 |
| **Login** | http://localhost:8080/auth/login |
| **Dashboard** | http://localhost:8080/dashboard |
| **Admin Users** | http://localhost:8080/admin/usuarios |
| **Geography (Ranchos)** | http://localhost:8080/geography/ranchos |
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **API Docs** | http://localhost:8080/api-docs |

---

## 💡 Comandos Útiles

```bash
# Compilar
./mvnw clean compile

# Compilar + tests
./mvnw clean install

# Tests sin integración
./mvnw test -Dtest='!*IntegrationTest'

# Limpiar caché
./mvnw clean

# Ver versión
./mvnw -version
```

---

**¿Preguntas?** Ver `.kiro/SETUP_GUIDE.md` para documentación detallada.


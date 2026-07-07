# Guía de Setup - Vacapp

## 📋 Requisitos Previos

- **Java 21** o superior
- **MySQL 8.0+**
- **Maven 3.8+** (incluido con el proyecto: `./mvnw`)
- **Git**

---

## 🗄️ Configuración de Base de Datos

### Nombre de la Base de Datos
```
vacapp
```

### Pasos para Crear la Base de Datos

#### 1. Conectar a MySQL
```bash
mysql -u root -p
```

#### 2. Crear la base de datos
```sql
CREATE DATABASE vacapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 3. Crear el usuario de aplicación
```sql
CREATE USER 'vacapp_user'@'localhost' IDENTIFIED BY 'vacapp_password';
GRANT ALL PRIVILEGES ON vacapp.* TO 'vacapp_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Variables de Entorno (Producción)

Si usas variables de entorno en lugar de valores por defecto:

```bash
# Base de Datos
export DATABASE_URL="jdbc:mysql://localhost:3306/vacapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DATABASE_USERNAME="vacapp_user"
export DATABASE_PASSWORD="vacapp_password"

# JWT (Cambiar en producción)
export JWT_SECRET="tu-secret-key-de-minimo-256-bits-32-bytes"
export JWT_EXPIRATION="86400000"  # 24 horas en ms

# Servidor
export SERVER_PORT="8080"

# Logging
export APP_LOG_LEVEL="INFO"

# CORS (Desarrollo)
export CORS_ALLOWED_ORIGINS="http://localhost:8080,http://localhost:3000"
```

---

## 🚀 Pasos para Ejecutar la Aplicación

### Opción 1: Desarrollo (Recomendado)

#### 1. Limpiar caché y compilar
```bash
./mvnw clean compile
```

#### 2. Ejecutar la aplicación
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

O simplemente:
```bash
./mvnw spring-boot:run
```

La aplicación iniciará con perfil `dev` por defecto.

#### 3. Verificar que la aplicación está corriendo
```
La aplicación estará disponible en: http://localhost:8080
```

---

### Opción 2: Construir JAR y Ejecutar

#### 1. Construir el proyecto
```bash
./mvnw clean package -DskipTests
```

#### 2. Ejecutar el JAR
```bash
java -jar target/vacapp-0.0.1-SNAPSHOT.jar
```

#### 3. Con perfil de producción
```bash
java -Dspring.profiles.active=prod \
     -DDATABASE_URL="jdbc:mysql://localhost:3306/vacapp" \
     -DDATABASE_USERNAME="vacapp_user" \
     -DDATABASE_PASSWORD="vacapp_password" \
     -DJWT_SECRET="tu-secret-de-produccion" \
     -jar target/vacapp-0.0.1-SNAPSHOT.jar
```

---

## 📱 Acceso a la Aplicación

### URLs Principales

| Componente | URL |
|-----------|-----|
| **Web Frontend** | http://localhost:8080 |
| **Login** | http://localhost:8080/auth/login |
| **Dashboard** | http://localhost:8080/dashboard |
| **Admin Users** | http://localhost:8080/admin/usuarios |
| **Geography** | http://localhost:8080/geography/ranchos |
| **Swagger UI (API REST)** | http://localhost:8080/swagger-ui.html |
| **API Docs (JSON)** | http://localhost:8080/api-docs |

---

## 👤 Usuario de Prueba por Defecto

Después de que las migraciones de Flyway creen las tablas, podrás crear un usuario:

### Vía API REST (POST)
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@vacapp.com",
    "name": "Admin User",
    "password": "AdminPassword123!",
    "phone": "+5215551234567"
  }'
```

### Vía Web (Interfaz)
1. Ve a http://localhost:8080/auth/login
2. Haz clic en "Registrarse"
3. Completa el formulario

---

## 🗄️ Migraciones de Base de Datos (Flyway)

Las migraciones se ejecutan **automáticamente** al iniciar la aplicación:

- **V1__create_users_table.sql** - Tabla de usuarios
- **V2__create_users_audit_table.sql** - Auditoría de usuarios
- **V3__create_authentication_log_table.sql** - Logs de autenticación
- **V4__create_geography_tables.sql** - Tablas geográficas (Ranchos, Secciones, Potreros)

Ubicación: `src/main/resources/db/migration/`

---

## 🧪 Ejecutar Tests

### Tests Unitarios (Recomendado)
```bash
./mvnw test -Dtest='!*IntegrationTest'
```

Resultado esperado: **97 tests exitosos**

### Todos los Tests (Requiere Docker)
```bash
./mvnw test
```

Nota: Los tests de integración requieren Testcontainers + Docker

### Tests Específicos
```bash
# Tests del módulo users
./mvnw test -Dtest='mx.vacapp.users.**'

# Tests del módulo geography
./mvnw test -Dtest='mx.vacapp.geography.**'

# Tests property-based
./mvnw test -Dtest='*PropertiesTest'
```

---

## 🐛 Troubleshooting

### Error: "No suitable driver found for jdbc:mysql"
**Solución**: Verifica que MySQL está corriendo y la URL en `application.yml` es correcta.

### Error: "Access denied for user 'vacapp_user'"
**Solución**: Verifica credenciales en `application.yml` o variables de entorno:
```bash
echo $DATABASE_USERNAME
echo $DATABASE_PASSWORD
```

### Error: "Caused by: java.nio.file.NoSuchFileException: logs/"
**Solución**: Crea el directorio de logs:
```bash
mkdir -p logs
```

### Puerta 8080 ya está en uso
**Solución**: Cambia el puerto:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

---

## 📊 Estructura de Carpetas

```
vacapp/
├── src/main/
│   ├── java/mx/vacapp/
│   │   ├── geography/          # Módulo Geographic Control
│   │   └── users/              # Módulo User Management
│   ├── resources/
│   │   ├── application.yml     # Configuración principal
│   │   ├── db/migration/       # Scripts de Flyway
│   │   ├── openapi/            # Especificaciones OpenAPI (YAML)
│   │   ├── templates/          # Vistas Thymeleaf
│   │   └── static/             # CSS, JS, imágenes
│   └── test/
├── pom.xml                     # Dependencias Maven
├── mvnw                        # Maven Wrapper (Linux/Mac)
├── mvnw.cmd                    # Maven Wrapper (Windows)
└── logs/                       # Logs de la aplicación
```

---

## 🔒 Configuración de Seguridad

### JWT
- **Token Expiration**: 24 horas (configurable)
- **Algorithm**: HMAC-SHA256
- **Secret**: Cambiar en producción (mín. 32 bytes)

### Multi-Tenancy
- Filtrado automático por `tenant_id` en todas las operaciones
- Extraído del contexto de seguridad JWT

### Auditoría
- Todos los cambios se registran en tablas de auditoría
- Retención: 730 días (configurable)

---

## 📝 Notas Importantes

1. **Perfiles de Spring**:
   - `dev` - Desarrollo (Logs DEBUG, Swagger habilitado)
   - `prod` - Producción (Logs INFO, stack traces deshabilitados)

2. **Logging**:
   - Archivos: `logs/vacapp.log`
   - Rotación: 10MB por archivo, máximo 30 archivos
   - Datos sensibles (passwords) se ocultan automáticamente

3. **Validación**:
   - Bean Validation en DTOs de entrada (Request)
   - Validaciones de negocio en casos de uso

4. **Cache**:
   - Caffeine Cache para estadísticas de ranchos
   - TTL: 5 minutos (configurable)

---

## 📚 Documentación Adicional

- **Arquitectura**: Ver `AGENTS.md`
- **Specs**: Ver `.kiro/specs/`
- **OpenAPI**: http://localhost:8080/swagger-ui.html


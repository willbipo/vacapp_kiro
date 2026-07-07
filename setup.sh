#!/bin/bash

#############################################
# Vacapp - Setup Automático
# Script para configurar la aplicación
#############################################

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funciones
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
    exit 1
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# 1. Validar requisitos
print_header "Validando Requisitos"

# Verificar Java
if ! command -v java &> /dev/null; then
    print_error "Java no está instalado. Por favor instala Java 21 o superior."
fi

JAVA_VERSION=$(java -version 2>&1 | grep -oE 'version "[^"]*"' | head -1)
print_success "Java encontrado: $JAVA_VERSION"

# Verificar MySQL
if ! command -v mysql &> /dev/null; then
    print_warning "MySQL no encontrado en PATH. Por favor verifica que MySQL está instalado."
    print_info "Puedes instalarlo con: brew install mysql (macOS) o apt-get install mysql-server (Linux)"
else
    MYSQL_VERSION=$(mysql --version)
    print_success "MySQL encontrado: $MYSQL_VERSION"
fi

# 2. Crear directorio de logs
print_header "Configurando Directorios"

if [ ! -d "logs" ]; then
    mkdir -p logs
    print_success "Directorio 'logs' creado"
else
    print_info "Directorio 'logs' ya existe"
fi

# 3. Crear archivo .env si no existe
print_header "Configurando Variables de Entorno"

if [ ! -f ".env" ]; then
    cp .env.example .env
    print_success "Archivo .env creado desde .env.example"
    print_warning "IMPORTANTE: Edita .env y actualiza las variables de entorno si es necesario"
else
    print_info "Archivo .env ya existe"
fi

# 4. Ofrecer crear base de datos
print_header "Configuración de Base de Datos"

read -p "¿Deseas crear la base de datos 'vacapp'? (s/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Ss]$ ]]; then
    read -sp "Ingresa la contraseña de root de MySQL: " MYSQL_ROOT_PASSWORD
    echo
    
    # SQL commands
    SQL_COMMANDS="
    CREATE DATABASE IF NOT EXISTS vacapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE USER IF NOT EXISTS 'vacapp_user'@'localhost' IDENTIFIED BY 'vacapp_password';
    GRANT ALL PRIVILEGES ON vacapp.* TO 'vacapp_user'@'localhost';
    FLUSH PRIVILEGES;
    "
    
    if echo "$SQL_COMMANDS" | mysql -u root -p"$MYSQL_ROOT_PASSWORD" 2>/dev/null; then
        print_success "Base de datos 'vacapp' creada correctamente"
    else
        print_error "Error al crear la base de datos. Verifica la contraseña de root."
    fi
else
    print_info "Configuración de base de datos omitida"
    print_warning "Recuerda crear manualmente la base de datos 'vacapp' antes de ejecutar la aplicación"
fi

# 5. Compilar el proyecto
print_header "Compilando el Proyecto"

if [ ! -d "target" ]; then
    print_info "Compilando por primera vez (esto puede tomar varios minutos)..."
    ./mvnw clean compile
    print_success "Proyecto compilado correctamente"
else
    print_info "Directorio target ya existe"
    read -p "¿Deseas recompilar? (s/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Ss]$ ]]; then
        ./mvnw clean compile
        print_success "Proyecto recompilado correctamente"
    fi
fi

# 6. Resumen final
print_header "Setup Completado ✓"

echo ""
echo -e "${GREEN}Próximos pasos:${NC}"
echo ""
echo "1. Inicia la aplicación:"
echo "   ${BLUE}./mvnw spring-boot:run${NC}"
echo ""
echo "2. Accede a la aplicación:"
echo "   ${BLUE}http://localhost:8080${NC}"
echo ""
echo "3. Accede a la documentación de API:"
echo "   ${BLUE}http://localhost:8080/swagger-ui.html${NC}"
echo ""
echo "4. Para crear un usuario, ve a:"
echo "   ${BLUE}http://localhost:8080/auth/login${NC}"
echo ""
echo "Para más información, ver QUICKSTART.md o .kiro/SETUP_GUIDE.md"
echo ""


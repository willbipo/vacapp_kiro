@echo off
REM ============================================
REM Vacapp - Setup Automatico (Windows)
REM ============================================

setlocal enabledelayedexpansion
color 0A

echo.
echo ========================================
echo Vacapp - Setup Automatico para Windows
echo ========================================
echo.

REM 1. Validar Java
echo Validando Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java no esta instalado. Por favor instala Java 21 o superior.
    pause
    exit /b 1
)
echo [OK] Java encontrado
java -version

REM 2. Validar MySQL
echo.
echo Validando MySQL...
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ADVERTENCIA] MySQL no encontrado en PATH.
    echo Por favor verifica que MySQL esta instalado y agregado al PATH.
) else (
    echo [OK] MySQL encontrado
    mysql --version
)

REM 3. Crear directorio de logs
echo.
echo Configurando directorios...
if not exist "logs" (
    mkdir logs
    echo [OK] Directorio 'logs' creado
) else (
    echo [INFO] Directorio 'logs' ya existe
)

REM 4. Crear archivo .env
echo.
echo Configurando variables de entorno...
if not exist ".env" (
    copy .env.example .env >nul
    echo [OK] Archivo .env creado desde .env.example
    echo [ADVERTENCIA] Por favor edita .env y actualiza las variables si es necesario
) else (
    echo [INFO] Archivo .env ya existe
)

REM 5. Compilar el proyecto
echo.
echo ========================================
echo Compilando el Proyecto
echo ========================================
echo.

if not exist "target" (
    echo [INFO] Compilando por primera vez...
    echo Esto puede tomar varios minutos...
    call mvnw.cmd clean compile
    if %errorlevel% neq 0 (
        echo [ERROR] Error durante la compilacion
        pause
        exit /b 1
    )
    echo [OK] Proyecto compilado correctamente
) else (
    set /p recompile="[?] Directorio target ya existe. ^!Deseas recompilar? (s/n): "
    if /i "!recompile!"=="s" (
        call mvnw.cmd clean compile
        if %errorlevel% neq 0 (
            echo [ERROR] Error durante la compilacion
            pause
            exit /b 1
        )
        echo [OK] Proyecto recompilado correctamente
    )
)

REM 6. Resumen final
echo.
echo ========================================
echo Setup Completado Exitosamente
echo ========================================
echo.

echo Proximos pasos:
echo.
echo 1. Inicia la aplicacion:
echo    mvnw.cmd spring-boot:run
echo.
echo 2. Accede a la aplicacion:
echo    http://localhost:8080
echo.
echo 3. Accede a la documentacion de API:
echo    http://localhost:8080/swagger-ui.html
echo.
echo 4. Para crear un usuario, ve a:
echo    http://localhost:8080/auth/login
echo.
echo Para mas informacion, ver QUICKSTART.md o .kiro/SETUP_GUIDE.md
echo.

pause


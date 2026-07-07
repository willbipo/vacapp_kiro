package mx.vacapp.geography.internal.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI (Swagger) para el módulo Geography.
 * 
 * Documenta todos los endpoints REST del módulo:
 * - /api/v1/geography/ranchos
 * - /api/v1/geography/secciones
 * - /api/v1/geography/potreros
 * 
 * Security:
 * - JWT Bearer token en header Authorization
 * 
 * Swagger UI disponible en: /swagger-ui/index.html
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Geographic Control API",
        version = "1.0",
        description = "API REST para gestión de jerarquía geográfica de terrenos ganaderos (Rancho → Sección → Potrero). " +
                     "Incluye operaciones CRUD, estadísticas, validaciones de superficie y aislamiento multi-tenant.",
        contact = @Contact(
            name = "Vacapp Development Team",
            email = "dev@vacapp.mx"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "Servidor de desarrollo local"
        ),
        @Server(
            url = "https://api.vacapp.mx",
            description = "Servidor de producción"
        )
    }
)
@SecurityScheme(
    name = "bearerAuth",
    description = "Autenticación JWT mediante Bearer token. Incluir token en header: Authorization: Bearer {token}",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
    // Esta clase configura la documentación OpenAPI del módulo.
    // Los endpoints específicos se definen en openapi-geography.yaml
}

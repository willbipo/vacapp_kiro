package mx.vacapp;

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Clase base para tests de integración que requieren una base de datos MySQL real.
 * <p>
 * Levanta un único contenedor MySQL compartido por toda la JVM de test (patrón
 * "singleton container" de Testcontainers): se inicia una sola vez en un bloque
 * estático y nunca se detiene explícitamente, evitando reinicios costosos y
 * problemas de conexión entre clases de test. Sus credenciales se exponen a
 * Spring Boot mediante {@code @DynamicPropertySource}. Flyway aplica las
 * migraciones reales de {@code src/main/resources/db/migration} contra este
 * contenedor.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class AbstractIntegrationTest {

    static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("vacapp_test")
            .withUsername("vacapp_test")
            .withPassword("vacapp_test");
        MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL_CONTAINER::getDriverClassName);
    }
}

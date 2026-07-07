package mx.vacapp.users.internal.properties;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import mx.vacapp.infrastructure.logging.SensitiveDataMaskingLayout;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 10: Password Redaction in Logs.
 * <p>
 * Para todo mensaje de log que contenga un campo cuyo nombre incluya "password" o
 * "secret" (en formato JSON {@code "campo":"valor"}), {@link SensitiveDataMaskingLayout}
 * debe reemplazar el valor por {@code [REDACTED]}, sin alterar el resto del mensaje.
 * </p>
 */
class PasswordRedactionPropertiesTest {

    private SensitiveDataMaskingLayout newLayout() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        SensitiveDataMaskingLayout layout = new SensitiveDataMaskingLayout();
        layout.setContext(context);
        layout.setPattern("%msg");
        layout.addSensitivePattern("password");
        layout.addSensitivePattern("secret");
        layout.start();
        return layout;
    }

    @Property
    void passwordFieldValuesAreAlwaysRedacted(@ForAll("sensitiveValues") String secretValue) {
        SensitiveDataMaskingLayout layout = newLayout();
        String message = "{\"email\":\"user@example.com\",\"password\":\"" + secretValue + "\"}";

        String output = layout.doLayout(loggingEventWithMessage(message));

        assertThat(output).contains("[REDACTED]");
        assertThat(output).doesNotContain(secretValue);
        assertThat(output).contains("user@example.com");
    }

    @Property
    void nonSensitiveFieldsAreNeverRedacted(@ForAll("plainValues") String plainValue) {
        SensitiveDataMaskingLayout layout = newLayout();
        String message = "{\"name\":\"" + plainValue + "\"}";

        String output = layout.doLayout(loggingEventWithMessage(message));

        assertThat(output).contains(plainValue);
        assertThat(output).doesNotContain("[REDACTED]");
    }

    private ILoggingEvent loggingEventWithMessage(String message) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerName("property-test-logger");
        event.setLevel(Level.INFO);
        event.setMessage(message);
        event.setTimeStamp(System.currentTimeMillis());
        return event;
    }

    @Provide
    Arbitrary<String> sensitiveValues() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(4).ofMaxLength(20);
    }

    @Provide
    Arbitrary<String> plainValues() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(4).ofMaxLength(20);
    }
}

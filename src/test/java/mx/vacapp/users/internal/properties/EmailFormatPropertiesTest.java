package mx.vacapp.users.internal.properties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.LoginRequest;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 8: Email Format Validation.
 * <p>
 * Para toda cadena candidata a email, el sistema la acepta si y solo si cumple un
 * formato básico razonable (usuario@dominio.tld) y su longitud no excede 254
 * caracteres. Emails inválidos o demasiado largos deben producir una violación de
 * Bean Validation.
 * </p>
 */
class EmailFormatPropertiesTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Property
    void validEmailsAreAlwaysAccepted(@ForAll("validEmails") String email) {
        LoginRequest request = new LoginRequest(email, "ValidP@ss1");
        Set<ConstraintViolation<LoginRequest>> violations = VALIDATOR.validate(request);

        boolean hasEmailViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email"));

        assertThat(hasEmailViolation).isFalse();
    }

    @Property
    void emailsWithoutAtSymbolAreAlwaysRejected(@ForAll("stringsWithoutAt") String invalidEmail) {
        LoginRequest request = new LoginRequest(invalidEmail, "ValidP@ss1");
        Set<ConstraintViolation<LoginRequest>> violations = VALIDATOR.validate(request);

        boolean hasEmailViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email"));

        assertThat(hasEmailViolation).isTrue();
    }

    @Property
    void emailsExceeding254CharactersAreAlwaysRejected(@ForAll("oversizedEmails") String oversizedEmail) {
        LoginRequest request = new LoginRequest(oversizedEmail, "ValidP@ss1");
        Set<ConstraintViolation<LoginRequest>> violations = VALIDATOR.validate(request);

        boolean hasEmailViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email"));

        assertThat(hasEmailViolation).isTrue();
    }

    @Provide
    Arbitrary<String> validEmails() {
        Arbitrary<String> localPart = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20);
        Arbitrary<String> domain = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(15);
        return net.jqwik.api.Combinators.combine(localPart, domain)
            .as((local, dom) -> local + "@" + dom + ".com");
    }

    @Provide
    Arbitrary<String> stringsWithoutAt() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> oversizedEmails() {
        Arbitrary<String> localPart = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(250).ofMaxLength(300);
        return localPart.map(local -> local + "@example.com");
    }
}

package mx.vacapp.users.internal.properties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.CreateUserRequest;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.CharRange;
import net.jqwik.api.constraints.StringLength;

import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 3: Password Complexity Enforcement.
 * <p>
 * Para toda contraseña generada, el resultado de la validación Bean Validation de
 * {@link CreateUserRequest} debe coincidir exactamente con la evaluación manual de
 * la regla de complejidad: longitud 8-128, con mayúscula, minúscula, dígito y
 * carácter especial de {@code @$!%*?&}.
 * </p>
 */
class PasswordComplexityPropertiesTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static final Pattern COMPLEXITY_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,128}$"
    );

    @Property
    void validationResultMatchesComplexityRuleExactly(
        @ForAll @StringLength(min = 1, max = 140) @CharRange(from = '!', to = 'z') String candidatePassword
    ) {
        CreateUserRequest request = new CreateUserRequest(
            "property-test@example.com", "Property Tester", "5551234567", candidatePassword, "worker"
        );

        Set<ConstraintViolation<CreateUserRequest>> violations = VALIDATOR.validate(request);
        boolean passwordAccepted = violations.stream()
            .noneMatch(v -> v.getPropertyPath().toString().equals("password"));

        boolean shouldBeAccepted = COMPLEXITY_PATTERN.matcher(candidatePassword).matches();

        assertThat(passwordAccepted).isEqualTo(shouldBeAccepted);
    }

    @Property
    void passwordsMissingAnyRequiredCharacterClassAreRejected(
        @ForAll("passwordsMissingOneRule") String password
    ) {
        CreateUserRequest request = new CreateUserRequest(
            "property-test@example.com", "Property Tester", "5551234567", password, "worker"
        );

        Set<ConstraintViolation<CreateUserRequest>> violations = VALIDATOR.validate(request);
        boolean hasPasswordViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password"));

        assertThat(hasPasswordViolation).isTrue();
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<String> passwordsMissingOneRule() {
        // Contraseñas de longitud válida pero sin dígitos (falla una regla de complejidad)
        return net.jqwik.api.Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(8)
            .ofMaxLength(20)
            .map(s -> "ABC" + s); // garantiza mayúsculas + minúsculas, sin dígito ni carácter especial
    }
}

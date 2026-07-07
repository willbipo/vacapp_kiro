package mx.vacapp.users.internal;

import mx.vacapp.users.UsersService;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del módulo Users.
 * <p>
 * Registra el bean de la API pública {@link UsersService} para que otros módulos
 * de Vacapp puedan consumirla mediante inyección de dependencias.
 * </p>
 */
@Configuration
public class UsersModuleConfig {

    @Bean
    UsersService usersService(UserRepository userRepository) {
        return new UsersServiceImpl(userRepository);
    }
}

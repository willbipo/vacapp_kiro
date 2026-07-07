package mx.vacapp.users.internal.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuración del codificador de contraseñas.
 * Proporciona un bean BCryptPasswordEncoder con factor de trabajo 12 para cifrado seguro de contraseñas.
 */
@Configuration
public class PasswordEncoderConfig {
    
    /**
     * Crea un bean PasswordEncoder usando BCrypt con strength 12.
     * El strength de 12 proporciona un balance óptimo entre seguridad y rendimiento,
     * cumpliendo con el requisito de factor de trabajo entre 10 y 12 (Requirements 1.4, 2.6).
     * 
     * @return PasswordEncoder configurado con BCrypt strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

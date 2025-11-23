package com.preparcial.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Inyectamos el manejador de errores (para detectar cuenta desactivada)
    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Recursos estáticos
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/Profiles/**").permitAll()
                // Login y Registro públicos
                .requestMatchers("/login", "/register").permitAll()
                
                // --- RESTAURADO AL ORIGINAL ---
                // Usamos "ROLE_ADMINISTRADOR" porque así funcionaba tu código original
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMINISTRADOR") 
                
                // El resto requiere estar logueado
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/redirectByRole", true)
                
                // MANTENEMOS LA MEJORA: Manejador de errores personalizado
                .failureHandler(customAuthenticationFailureHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
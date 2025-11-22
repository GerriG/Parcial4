package com.preparcial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException; // IMPORTANTE
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMINISTRADOR")
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/redirectByRole", true)
                
                // --- CAMBIO IMPORTANTE AQUÍ ---
                // En lugar de failureUrl fijo, usamos un handler lógico
                .failureHandler((request, response, exception) -> {
                    String targetUrl = "/login?error"; // Error genérico por defecto
                    
                    // Si el error es por cuenta deshabilitada
                    if (exception instanceof DisabledException) {
                        targetUrl = "/login?disabled"; 
                    }
                    
                    response.sendRedirect(targetUrl);
                })
                // ------------------------------
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
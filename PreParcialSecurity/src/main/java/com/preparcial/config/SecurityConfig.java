package com.preparcial.config;

import jakarta.servlet.http.HttpServletResponse; 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. DESHABILITAR CSRF (Necesario para que funcionen los POST sin token)
            .csrf(csrf -> csrf.disable()) 

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                // Rutas públicas (Login, Registro y Reset Password)
                .requestMatchers("/login", "/register", "/reset-password").permitAll()          
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMINISTRADOR")
                .anyRequest().authenticated()
            )
                
            .formLogin(login -> login
                .loginPage("/login")
                .permitAll()
                .successHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\": \"success\", \"url\": \"/redirectByRole\"}");
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    
                    String type = "error"; 
                    if (exception instanceof DisabledException) {
                        type = "disabled";
                    }
                    
                    response.getWriter().write("{\"status\": \"error\", \"type\": \"" + type + "\"}");
                })
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
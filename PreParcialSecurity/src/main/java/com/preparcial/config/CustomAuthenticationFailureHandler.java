package com.preparcial.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        AuthenticationException exception) throws IOException, ServletException {
        
        // Si el error es "DisabledException" (usuario existe pero estado = false)
        if (exception instanceof DisabledException) {
            response.sendRedirect("/login?disabled");
        } else {
            // Cualquier otro error (contraseña incorrecta, usuario no existe)
            response.sendRedirect("/login?error");
        }
    }
}
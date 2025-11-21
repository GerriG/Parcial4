package com.preparcial.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configuración para que Spring pueda leer los archivos subidos en tiempo real
        // Mapea la URL "/uploads/**" a la ruta física del sistema de archivos
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:src/main/resources/static/Profiles/");
    }
}

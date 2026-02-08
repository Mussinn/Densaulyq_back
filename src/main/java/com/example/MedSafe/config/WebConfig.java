package com.example.MedSafe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // Применяем CORS ко всем API-эндпоинтам
                        .allowedOrigins("http://localhost:5173", "http://192.168.8.9:5173") // Порт фронтенда
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("Content-Type", "Authorization", "X-Requested-With") // Явно указываем заголовки
                        .allowCredentials(true)
                        .maxAge(3600); // Кэшируем предварительные запросы на 1 час
            }
        };
    }
}
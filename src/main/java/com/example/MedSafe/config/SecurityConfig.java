package com.example.MedSafe.config;

import com.example.MedSafe.service.CustomUserDetailsService;
import com.example.MedSafe.service.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .csrf(AbstractHttpConfigurer::disable)
//                .securityMatcher(request -> {
//                    String uri = request.getRequestURI();
//                    return !uri.startsWith("/ws") && !uri.startsWith("/ws-call");
//                })
//                .authorizeHttpRequests(auth -> auth
//                        // WebSocket
//                        .requestMatchers("/ws/**", "/ws-call/**").permitAll()
//
//                        // Swagger UI - ВАЖНО! Все пути:
//                        .requestMatchers(
//                                "/swagger-ui/**",
//                                "/swagger-ui.html",
//                                "/v3/api-docs/**",
//                                "/api-docs/**",
//                                "/swagger-resources/**",
//                                "/swagger-resources",
//                                "/webjars/**",
//                                "/configuration/ui",
//                                "/configuration/security",
//                                "/swagger-config"
//                        ).permitAll()
//
//                        // Ваши API
//                        .requestMatchers(
//                                "/api/auth/**",
//                                "/api/v1/diagnosis/decrypt",
//                                "/api/medical-card",
//                                "/api/v1/diagnosis",
//                                "/api/agora/token"
//                        ).permitAll()
//
//                        .anyRequest().authenticated()
//                )
//                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authenticationProvider(authenticationProvider())
//                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }

    // Остальные бины без изменений
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://localhost:8080"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .securityMatcher(request -> {
                    String uri = request.getRequestURI();
                    return !uri.startsWith("/ws") && !uri.startsWith("/ws-call");
                })
                .authorizeHttpRequests(auth -> auth
                        // OPTIONS для всех CORS preflight запросов
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // WebSocket
                        .requestMatchers("/ws/**", "/ws-call/**").permitAll()

                        // Swagger UI
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-resources/**",
                                "/swagger-resources",
                                "/webjars/**",
                                "/configuration/ui",
                                "/configuration/security",
                                "/swagger-config"
                        ).permitAll()

                        // Ваши API (ДОБАВЬТЕ appointments)
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/v1/diagnosis/decrypt",
                                "/api/medical-card",
                                "/api/v1/diagnosis",
                                "/api/agora/token",
                                "/api/appointments"  // ДОБАВЬТЕ ЭТО!
                        ).permitAll()

                        .anyRequest().authenticated()
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
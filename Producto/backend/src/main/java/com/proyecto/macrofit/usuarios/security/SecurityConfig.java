package com.proyecto.macrofit.usuarios.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private JwtAuthFilter jwtAuthFilter;

        // Orígenes permitidos (separados por coma), configurables por variable de entorno CORS_ALLOWED_ORIGINS
        @Value("${app.cors.allowed-origins}")
        private String[] allowedOrigins;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())

                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth

                                                .requestMatchers("/health").permitAll()

                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/usuarios/login",
                                                                "/api/usuarios/registro")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/usuarios/*/perfil")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/usuarios/**").hasRole("ADMIN")

                                                // Catálogos compartidos: solo el ADMIN puede crear, editar o borrar.
                                                // Así un visitante de la demo no puede romper el catálogo.
                                                .requestMatchers(HttpMethod.POST,
                                                                "/ejercicios",
                                                                "/ejercicios/importar-excel",
                                                                "/rutinas/catalogo",
                                                                "/api/nutricion/comidas",
                                                                "/api/nutricion/admin/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT,
                                                                "/ejercicios/*",
                                                                "/api/nutricion/comidas/*",
                                                                "/api/nutricion/recetas-cache/*")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE,
                                                                "/ejercicios/*",
                                                                "/api/nutricion/comidas/*",
                                                                "/api/nutricion/recetas-cache/*")
                                                .hasRole("ADMIN")

                                                .anyRequest().authenticated())

                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOriginPatterns(List.of(allowedOrigins));

                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
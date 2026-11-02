package com.proyecto.macrofit.usuarios.security;

import com.proyecto.macrofit.usuarios.model.Usuario;
import com.proyecto.macrofit.usuarios.model.Entity.UsuarioEntity;
import com.proyecto.macrofit.usuarios.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Value("${app.cors.allowed-origins:*}")
        private String[] allowedOrigins;

        @Autowired
        private UsuarioRepository usuarioRepository;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .anyRequest().permitAll());

                return http.build();
        }

        // Servicio para buscar al usuario en Aiven
        @Bean
        public UserDetailsService userDetailsService() {
                return correo -> {
                        UsuarioEntity usuario = usuarioRepository.findByCorreo(correo)
                                        .orElseThrow(() -> new UsernameNotFoundException(
                                                        "Usuario no encontrado: " + correo));

                        return org.springframework.security.core.userdetails.User
                                        .withUsername(usuario.getCorreo())
                                        .password(usuario.getContrasena())
                                        .roles("USER")
                                        .build();
                };
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
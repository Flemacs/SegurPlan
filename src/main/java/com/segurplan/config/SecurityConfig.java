package com.segurplan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler) {

        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // =========================================
                // CSRF
                // =========================================
                .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                        "/api/usuarios/login",
                        "/api/usuarios/registro"
                )
                )
                // =========================================
                // SESIONES
                // =========================================
                .sessionManagement(session -> session
                .sessionCreationPolicy(
                        SessionCreationPolicy.IF_REQUIRED
                )
                )
                // =========================================
                // AUTORIZACIÓN
                // =========================================
                .authorizeHttpRequests(auth -> auth
                // -------------------------------------
                // Recursos públicos
                // -------------------------------------
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/pages/s00-registro.html",
                        "/pages/s01-login.html"
                ).permitAll()
                // -------------------------------------
                // Registro y login
                // -------------------------------------
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/usuarios/registro",
                        "/api/usuarios/login"
                ).permitAll()
                // -------------------------------------
                // Token CSRF
                // -------------------------------------
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/csrf"
                ).permitAll()
                // -------------------------------------
                // CLIENTE
                // -------------------------------------
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/polizas/mis-polizas"
                ).hasRole("Cliente")
                // -------------------------------------
                // ADMINISTRADOR
                // -------------------------------------
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/polizas"
                ).hasRole("Administrador")
                // -------------------------------------
                // Cualquier otra petición
                // necesita autenticación
                // -------------------------------------
                .requestMatchers(
                        "/api/previsiones/asesor/**"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )
                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/previsiones/*/respuesta"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )
                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/previsiones/*/solicitar-orientacion"
                ).hasRole(
                        "Cliente"
                )
                .requestMatchers(
                        "/api/reportes/**"
                ).hasAnyRole(
                        "Administrador",
                        "Gerente"
                )
                .anyRequest().authenticated()
                )
                // =========================================
                // LOGOUT
                // =========================================
                .logout(logout -> logout
                // Endpoint que llamará app.js
                .logoutUrl("/api/usuarios/logout")
                // Invalidar sesión del servidor
                .invalidateHttpSession(true)
                // Limpiar autenticación de Spring Security
                .clearAuthentication(true)
                // Eliminar cookie de sesión
                .deleteCookies("JSESSIONID")
                // Respuesta exitosa sin redirección automática
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    response.getWriter().write(
                            "{\"mensaje\":\"Sesión cerrada correctamente\"}"
                    );
                })
                )
                // =========================================
                // ERRORES DE SEGURIDAD
                // =========================================
                .exceptionHandling(errors -> errors
                .authenticationEntryPoint(
                        authenticationEntryPoint
                )
                .accessDeniedHandler(
                        accessDeniedHandler
                )
                );

        return http.build();
    }
}

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

    // =====================================================
    // PASSWORD ENCODER
    // =====================================================

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    // =====================================================
    // SECURITY FILTER CHAIN
    // =====================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

                // =================================================
                // CSRF
                // =================================================
                .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                        "/api/usuarios/login",
                        "/api/usuarios/registro"
                )
                )

                // =================================================
                // SESIONES
                // =================================================
                .sessionManagement(session -> session
                .sessionCreationPolicy(
                        SessionCreationPolicy.IF_REQUIRED
                )
                )

                // =================================================
                // AUTORIZACIÓN
                // =================================================
                .authorizeHttpRequests(auth -> auth

                // -------------------------------------------------
                // RECURSOS PÚBLICOS
                // -------------------------------------------------
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/pages/s00-registro.html",
                        "/pages/s01-login.html"
                ).permitAll()

                // -------------------------------------------------
                // LOGIN Y REGISTRO
                // -------------------------------------------------
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/usuarios/registro",
                        "/api/usuarios/login"
                ).permitAll()

                // -------------------------------------------------
                // TOKEN CSRF
                // -------------------------------------------------
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/csrf"
                ).permitAll()

                // =================================================
                // ADMINISTRADOR
                // =================================================

                // Todas las operaciones administrativas
                .requestMatchers(
                        "/api/admin/**"
                ).hasRole(
                        "Administrador"
                )

                // Generación de pólizas
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/polizas"
                ).hasRole(
                        "Administrador"
                )

                // Reportes
                .requestMatchers(
                        "/api/reportes/**"
                ).hasRole(
                        "Administrador"
                )

                // =================================================
                // ASESOR Y ADMINISTRADOR
                // =================================================

                // Gestión AFP desde panel asesor
                .requestMatchers(
                        "/api/previsiones/asesor/**"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )

                // Responder solicitud AFP
                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/previsiones/*/respuesta"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )

                // Consultar siniestros desde panel asesor
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/siniestros/asesor/**"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )

                // Derivar siniestros
                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/siniestros/*/derivar"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )

                // Consultar documentos para revisión
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/documentos/asesor/**"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )

                // =================================================
                // CLIENTE
                // =================================================

                // Consultar pólizas propias
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/polizas/mis-polizas"
                ).hasRole(
                        "Cliente"
                )

                // Solicitar orientación AFP
                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/previsiones/*/solicitar-orientacion"
                ).hasRole(
                        "Cliente"
                )

                // =================================================
                // RESTO DE ENDPOINTS
                // =================================================
                // Por ahora conservamos el comportamiento actual:
                // cualquier usuario autenticado puede acceder.
                .anyRequest()
                .authenticated()
                )

                // =================================================
                // LOGOUT
                // =================================================
                .logout(logout -> logout

                .logoutUrl(
                        "/api/usuarios/logout"
                )

                .invalidateHttpSession(true)

                .clearAuthentication(true)

                .deleteCookies(
                        "JSESSIONID"
                )

                .logoutSuccessHandler(
                        (request,
                         response,
                         authentication) -> {

                            response.setStatus(200);

                            response.setContentType(
                                    "application/json"
                            );

                            response.setCharacterEncoding(
                                    "UTF-8"
                            );

                            response.getWriter().write(
                                    "{\"mensaje\":"
                                    + "\"Sesión cerrada correctamente\"}"
                            );
                        }
                )
                )

                // =================================================
                // ERRORES
                // =================================================
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
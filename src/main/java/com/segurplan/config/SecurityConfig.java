package com.segurplan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
public class SecurityConfig {

    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    /** Swagger UI y /v3/api-docs solo se exponen si SWAGGER_ENABLED=true. */
    private final boolean swaggerHabilitado;

    public SecurityConfig(
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler,
            @Value("${springdoc.api-docs.enabled:true}")
            boolean swaggerHabilitado) {

        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.swaggerHabilitado = swaggerHabilitado;
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
                // CABECERAS DE SEGURIDAD (RNF04)
                // =================================================
                .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; "
                        + "img-src 'self' data: blob:; "
                        + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                        + "font-src 'self' data: https://fonts.gstatic.com; "
                        + "script-src 'self' 'unsafe-inline'; "
                        + "connect-src 'self'; "
                        + "frame-ancestors 'none'; "
                        + "form-action 'self'; "
                        + "base-uri 'self'; "
                        // blob: permite ver evidencias (PDF, imagen, video) descargadas con sesión
                        + "object-src 'self' blob:; "
                        + "media-src 'self' blob:"))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(ref -> ref.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy
                                .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000))
                .addHeaderWriter(new StaticHeadersWriter(
                        "Permissions-Policy",
                        "camera=(), microphone=(), geolocation=()"))
                )

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
                        "/pages/s01-login.html",
                        "/privacidad.html",
                        "/pages/privacidad.html"
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
                ).hasAnyRole(
                        "Administrador",
                        "Asesor"
                )

                // Reportes
                .requestMatchers(
                        "/api/reportes/**"
                ).hasAnyRole(
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

                // Pago simulado de la contratación (tarjeta / Yape / Plin)
                .requestMatchers(
                        "/api/pagos",
                        "/api/pagos/**"
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
                // v5 - CONTROL DE ACCESO POR ROL
                // =================================================
                // Gestión de usuarios duplicada: solo administrador
                .requestMatchers(
                        "/api/usuarios/admin/**"
                ).hasRole(
                        "Administrador"
                )
                // Listado general de cotizaciones: solo personal
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/cotizaciones"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )
                // Responder cotizaciones y comunicarse con la aseguradora
                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/cotizaciones/*/respuesta"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )
                .requestMatchers(
                        "/api/cotizaciones/*/comunicaciones"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )
                // Contrataciones (datos de todos los clientes): solo personal
                .requestMatchers(
                        "/api/contrataciones/**"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )
                // Flujo del asesor sobre siniestros
                .requestMatchers(
                        "/api/siniestros/gestion/**"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )
                // Validación documental
                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/documentos/*/validacion"
                ).hasAnyRole(
                        "Asesor",
                        "Administrador"
                )
                // Historial unificado del cliente (cotizaciones, siniestros, AFP)
                .requestMatchers(
                        "/api/mis-procesos",
                        "/api/mis-procesos/**"
                ).hasRole(
                        "Cliente"
                )
                // Documentación Swagger / OpenAPI (solo si está habilitada)
                .requestMatchers(
                        swaggerHabilitado
                                ? new String[]{
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs",
                                        "/v3/api-docs/**",
                                        "/v3/api-docs.yaml"}
                                : new String[]{"/__swagger_deshabilitado__"}
                ).permitAll()
                // =================================================
                // RESTO DE ENDPOINTS
                // =================================================
                // Cualquier otra ruta exige sesión. Los controladores
                // vuelven a comprobar que el dato pertenezca al usuario
                // (AccesoService) cuando el recurso tiene dueño.
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

package com.segurplan.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ApiAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/")) {
            // API: 401 para que el frontend sepa que debe iniciar sesión
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"mensaje\":\"Debe iniciar sesión para continuar.\"}");
        } else {
            // Páginas: se envía al login
            response.sendRedirect("/pages/s01-login.html");
        }
    }
}
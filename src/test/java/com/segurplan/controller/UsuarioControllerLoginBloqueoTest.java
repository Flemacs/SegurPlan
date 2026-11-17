package com.segurplan.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.segurplan.dto.LoginRequest;
import com.segurplan.service.LoginAttemptService;
import com.segurplan.service.NotificacionService;
import com.segurplan.service.SegurplanUserDetailsService;
import com.segurplan.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * El inicio de sesión se bloquea tras varios intentos fallidos (fuerza bruta).
 */
class UsuarioControllerLoginBloqueoTest {

    private UsuarioService service;
    private UsuarioController controller;
    private HttpServletRequest http;
    private LoginRequest request;

    @BeforeEach
    void setUp() {
        service = mock(UsuarioService.class);
        controller = new UsuarioController(
                service,
                mock(SegurplanUserDetailsService.class),
                mock(NotificacionService.class),
                new LoginAttemptService());

        http = mock(HttpServletRequest.class);
        when(http.getRemoteAddr()).thenReturn("203.0.113.7");

        request = new LoginRequest();
        request.setCorreo("Ana@Correo.com");
        request.setPassword("incorrecta1");

        when(service.login(any()))
                .thenThrow(new RuntimeException("Correo o contraseña incorrectos"));
    }

    @Test
    void tras5FallosElSextoIntentoDevuelve429SinConsultarElServicio() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS_USUARIO; i++) {
            assertEquals(400, controller.login(request, http)
                    .getStatusCode().value());
        }

        ResponseEntity<?> bloqueada = controller.login(request, http);

        assertEquals(429, bloqueada.getStatusCode().value());
        assertNotNull(bloqueada.getHeaders().getFirst("Retry-After"));
        assertTrue(((Map<?, ?>) bloqueada.getBody()).get("mensaje")
                .toString().contains("Demasiados intentos"));
        verify(service, times(LoginAttemptService.MAX_INTENTOS_USUARIO))
                .login(any());
    }

    @Test
    void elBloqueoNoDependeDeMayusculasEnElCorreo() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS_USUARIO; i++) {
            controller.login(request, http);
        }

        request.setCorreo("ana@correo.com");

        assertEquals(429, controller.login(request, http)
                .getStatusCode().value());
    }

    @Test
    void otroCorreoDesdeOtraIpNoSeVeAfectado() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS_USUARIO; i++) {
            controller.login(request, http);
        }

        LoginRequest otro = new LoginRequest();
        otro.setCorreo("luis@correo.com");
        otro.setPassword("incorrecta1");
        HttpServletRequest otraIp = mock(HttpServletRequest.class);
        when(otraIp.getRemoteAddr()).thenReturn("198.51.100.9");

        assertEquals(400, controller.login(otro, otraIp)
                .getStatusCode().value());
    }
}

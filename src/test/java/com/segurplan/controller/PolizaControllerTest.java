
package com.segurplan.controller;

import com.segurplan.dto.PolizaRequest;
import com.segurplan.dto.PolizaResumenDTO;
import com.segurplan.model.Poliza;
import com.segurplan.model.Usuario;
import com.segurplan.repository.UsuarioRepository;
import com.segurplan.service.AccesoService;
import com.segurplan.service.NotificacionService;
import com.segurplan.service.PolizaService;
import org.springframework.security.core.Authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PolizaControllerTest {

    private PolizaService polizaService;
    private UsuarioRepository usuarioRepository;
    private PolizaController controller;
    private AccesoService acceso;
    private NotificacionService notificaciones;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        polizaService = mock(PolizaService.class);
        usuarioRepository = mock(UsuarioRepository.class);

        acceso = mock(AccesoService.class);
        notificaciones = mock(NotificacionService.class);
        auth = mock(Authentication.class);
        when(acceso.puedeVer(auth, 1L)).thenReturn(true);

        controller = new PolizaController(
                polizaService, usuarioRepository, acceso, notificaciones
        );
    }

    @Test
    void generarPolizaCorrectamente() {
        PolizaRequest request = mock(PolizaRequest.class);
        Poliza poliza = mock(Poliza.class);

        when(polizaService.generar(request)).thenReturn(poliza);
        when(poliza.getIdPoliza()).thenReturn(1L);
        when(poliza.getNumeroPoliza()).thenReturn("POL-001");
        when(poliza.getEstado()).thenReturn("Activa");

        ResponseEntity<?> respuesta = controller.generar(request);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals(1L, body.get("idPoliza"));
        assertEquals("POL-001", body.get("numeroPoliza"));
        assertEquals("Activa", body.get("estado"));
    }

    @Test
    void generarPolizaConError() {
        PolizaRequest request = mock(PolizaRequest.class);

        when(polizaService.generar(request))
                .thenThrow(new IllegalArgumentException("Datos inválidos"));

        ResponseEntity<?> respuesta = controller.generar(request);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals("Datos inválidos", body.get("mensaje"));
    }

    @Test
    void listarPolizasPorUsuarioCorrectamente() {
        PolizaResumenDTO dto = mock(PolizaResumenDTO.class);

        when(polizaService.listarPorUsuario(1L))
                .thenReturn(List.of(dto));

        ResponseEntity<?> respuesta = controller.listarPorUsuario(1L, auth);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(1, ((List<?>) respuesta.getBody()).size());
    }

    @Test
    void listarPolizasPorUsuarioConError() {
        when(polizaService.listarPorUsuario(1L))
                .thenThrow(new IllegalArgumentException("Usuario inválido"));

        ResponseEntity<?> respuesta = controller.listarPorUsuario(1L, auth);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals("Usuario inválido", body.get("mensaje"));
    }

    @Test
    void misPolizasSinPrincipal() {
        ResponseEntity<?> respuesta = controller.misPolizas(null);

        assertEquals(401, respuesta.getStatusCode().value());
    }

    @Test
    void misPolizasCorrectamente() {
        Principal principal = mock(Principal.class);
        Usuario usuario = mock(Usuario.class);
        PolizaResumenDTO dto = mock(PolizaResumenDTO.class);

        when(principal.getName()).thenReturn("cliente@test.com");
        when(usuarioRepository.findByCorreo("cliente@test.com"))
                .thenReturn(Optional.of(usuario));
        when(usuario.getIdUsuario()).thenReturn(1L);
        when(polizaService.listarPorUsuario(1L))
                .thenReturn(List.of(dto));

        ResponseEntity<?> respuesta = controller.misPolizas(principal);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(1, ((List<?>) respuesta.getBody()).size());
    }

    @Test
    void misPolizasCuandoUsuarioNoExiste() {
        Principal principal = mock(Principal.class);

        when(principal.getName()).thenReturn("noexiste@test.com");
        when(usuarioRepository.findByCorreo("noexiste@test.com"))
                .thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> controller.misPolizas(principal)
        );

        assertEquals("Usuario no encontrado.", error.getMessage());
    }

    @Test
    void listarPolizasDeOtroUsuarioDevuelve403() {
        when(acceso.puedeVer(auth, 2L)).thenReturn(false);

        ResponseEntity<?> respuesta = controller.listarPorUsuario(2L, auth);

        assertEquals(403, respuesta.getStatusCode().value());
        verifyNoInteractions(polizaService);
    }
}

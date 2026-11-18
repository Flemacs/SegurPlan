
package com.segurplan.controller;

import com.segurplan.dto.PrevisionRequest;
import com.segurplan.model.Prevision;
import com.segurplan.model.Usuario;
import com.segurplan.service.PrevisionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrevisionControllerTest {

    private PrevisionService service;
    private PrevisionController controller;
    private Principal principal;

    @BeforeEach
    void setUp() {
        service = mock(PrevisionService.class);
        controller = new PrevisionController(
                service,
                mock(com.segurplan.service.NotificacionService.class));

        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("cliente@test.com");
    }

    private Prevision crearPrevision() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setNombres("Juan");
        usuario.setApellidos("Perez");

        Prevision prevision = new Prevision();
        prevision.setIdPrevision(1L);
        prevision.setUsuario(usuario);
        prevision.setTipoSistema("AFP");
        prevision.setEdad(30);
        prevision.setIngresoMensual(new BigDecimal("2500.00"));
        prevision.setAniosAporte(10);
        prevision.setFechaRegistro(LocalDateTime.now());
        prevision.setMontoPensionEstimado(new BigDecimal("1200.00"));
        prevision.setResultadoSimulacion("Resultado");
        prevision.setConsulta("Consulta");
        prevision.setRespuesta("Respuesta");
        prevision.setEstado("Pendiente");

        return prevision;
    }

    @Test
    void simularCorrectamente() {
        PrevisionRequest request = mock(PrevisionRequest.class);

        when(service.simular("cliente@test.com", request))
                .thenReturn(crearPrevision());

        ResponseEntity<?> respuesta =
                controller.simular(request, principal);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals(1L, body.get("idPrevision"));
        assertEquals("Juan Perez", body.get("cliente"));
        assertEquals("AFP", body.get("tipoSistema"));
    }

    @Test
    void simularConError() {
        PrevisionRequest request = mock(PrevisionRequest.class);

        when(service.simular("cliente@test.com", request))
                .thenThrow(new IllegalArgumentException("Datos inválidos"));

        ResponseEntity<?> respuesta =
                controller.simular(request, principal);

        assertEquals(400, respuesta.getStatusCode().value());
        assertTrue(respuesta.getBody().toString()
                .contains("Datos inválidos"));
    }

    @Test
    void listarMisSimulaciones() {
        when(service.listarPorUsuario("cliente@test.com"))
                .thenReturn(List.of(crearPrevision()));

        ResponseEntity<?> respuesta =
                controller.misSimulaciones(principal);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(1, ((List<?>) respuesta.getBody()).size());
    }

    @Test
    void obtenerSimulacionCorrectamente() {
        when(service.obtenerDelUsuario(1L, "cliente@test.com"))
                .thenReturn(crearPrevision());

        ResponseEntity<?> respuesta =
                controller.obtener(1L, principal);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals(1L, body.get("idPrevision"));
    }

    @Test
    void obtenerSimulacionConError() {
        when(service.obtenerDelUsuario(1L, "cliente@test.com"))
                .thenThrow(new IllegalArgumentException("No encontrada"));

        ResponseEntity<?> respuesta =
                controller.obtener(1L, principal);

        assertEquals(400, respuesta.getStatusCode().value());
    }

    @Test
    void solicitarOrientacionCorrectamente() {
        when(service.solicitarOrientacion(1L, "cliente@test.com"))
                .thenReturn(crearPrevision());

        ResponseEntity<?> respuesta =
                controller.solicitarOrientacion(1L, principal);

        assertEquals(200, respuesta.getStatusCode().value());
    }

    @Test
    void solicitarOrientacionConError() {
        when(service.solicitarOrientacion(1L, "cliente@test.com"))
                .thenThrow(new IllegalArgumentException("Solicitud inválida"));

        ResponseEntity<?> respuesta =
                controller.solicitarOrientacion(1L, principal);

        assertEquals(400, respuesta.getStatusCode().value());
    }

    @Test
    void listarSolicitudesPendientesDelAsesor() {
        when(service.listarSolicitudesAsesor())
                .thenReturn(List.of(crearPrevision()));

        ResponseEntity<?> respuesta =
                controller.solicitudesAsesor();

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(1, ((List<?>) respuesta.getBody()).size());
    }

    @Test
    void responderOrientacionCorrectamente() {
        when(service.responderOrientacion(1L, "Debe continuar aportando"))
                .thenReturn(crearPrevision());

        ResponseEntity<?> respuesta = controller.responder(
                1L,
                Map.of("respuesta", "Debe continuar aportando")
        );

        assertEquals(200, respuesta.getStatusCode().value());
    }

    @Test
    void responderOrientacionConError() {
        when(service.responderOrientacion(1L, "Respuesta"))
                .thenThrow(new IllegalArgumentException("No se pudo responder"));

        ResponseEntity<?> respuesta = controller.responder(
                1L,
                Map.of("respuesta", "Respuesta")
        );

        assertEquals(400, respuesta.getStatusCode().value());
    }
}

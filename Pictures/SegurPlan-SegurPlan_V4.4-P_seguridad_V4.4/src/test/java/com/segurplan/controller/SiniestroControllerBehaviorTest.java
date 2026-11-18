
package com.segurplan.controller;

import com.segurplan.dto.SiniestroRequest;
import com.segurplan.model.Aseguradora;
import com.segurplan.model.Siniestro;
import com.segurplan.model.Usuario;
import com.segurplan.service.SiniestroService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SiniestroControllerBehaviorTest {

    private SiniestroService service;
    private SiniestroController controller;
    private com.segurplan.service.SiniestroGestionService gestion;
    private Principal principal;

    @BeforeEach
    void setUp() {
        service = mock(SiniestroService.class);
        gestion = mock(com.segurplan.service.SiniestroGestionService.class);
        controller = new SiniestroController(service, gestion);
        principal = () -> "cliente@test.com";
    }

    private Siniestro crearSiniestro() {
        Siniestro s = new Siniestro();

        s.setIdSiniestro(1L);
        s.setNumeroSiniestro("SIN-ABC12345");
        s.setFechaRegistro(LocalDateTime.of(2026, 11, 10, 10, 0));
        s.setFechaAccidente(LocalDateTime.of(2026, 11, 9, 15, 0));
        s.setPlaca("ABC-123");
        s.setMarca("Toyota");
        s.setModelo("Corolla");
        s.setAnio(2022);
        s.setLugar("Arequipa");
        s.setDescripcion("Accidente vehicular");
        s.setResultadoEvaluacion("Pendiente");
        s.setObservaciones("Sin observaciones");
        s.setEstado("Registrado");

        return s;
    }

    @Test
    void registrarCorrectamente() {
        SiniestroRequest request = new SiniestroRequest();
        Siniestro siniestro = crearSiniestro();

        when(service.registrar("cliente@test.com", request))
                .thenReturn(siniestro);

        ResponseEntity<?> respuesta =
                controller.registrar(request, principal);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(1L, body.get("idSiniestro"));
        assertEquals("SIN-ABC12345", body.get("numeroSiniestro"));
        assertEquals("Registrado", body.get("estado"));
        assertEquals("Siniestro registrado correctamente.",
                body.get("mensaje"));

        verify(service).registrar("cliente@test.com", request);
    }

    @Test
    void registrarConDatosInvalidos() {
        SiniestroRequest request = new SiniestroRequest();

        when(service.registrar("cliente@test.com", request))
                .thenThrow(new IllegalArgumentException(
                        "La placa es obligatoria."));

        ResponseEntity<?> respuesta =
                controller.registrar(request, principal);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals("La placa es obligatoria.", body.get("mensaje"));
    }

    @Test
    void listarMisSiniestrosCorrectamente() {
        Siniestro siniestro = crearSiniestro();

        when(service.listarDelUsuario("cliente@test.com"))
                .thenReturn(List.of(siniestro));

        ResponseEntity<?> respuesta =
                controller.misSiniestros(principal);

        assertEquals(200, respuesta.getStatusCode().value());

        List<?> body = (List<?>) respuesta.getBody();
        assertEquals(1, body.size());

        Map<?, ?> item = (Map<?, ?>) body.get(0);
        assertEquals(1L, item.get("idSiniestro"));
        assertEquals("Toyota", item.get("marca"));
        assertEquals("ABC-123", item.get("placa"));
        assertEquals("Registrado", item.get("estado"));
    }

    @Test
    void listarMisSiniestrosSinRegistros() {
        when(service.listarDelUsuario("cliente@test.com"))
                .thenReturn(List.of());

        ResponseEntity<?> respuesta =
                controller.misSiniestros(principal);

        assertEquals(200, respuesta.getStatusCode().value());
        assertNotNull(respuesta.getBody());
        assertTrue(((List<?>) respuesta.getBody()).isEmpty());
    }

    @Test
    void listarMisSiniestrosConError() {
        when(service.listarDelUsuario("cliente@test.com"))
                .thenThrow(new IllegalArgumentException(
                        "No se encontró el usuario autenticado."));

        ResponseEntity<?> respuesta =
                controller.misSiniestros(principal);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals("No se encontró el usuario autenticado.",
                body.get("mensaje"));
    }

    @Test
    void obtenerSiniestroCorrectamente() {
        Siniestro siniestro = crearSiniestro();

        when(service.obtenerDelUsuario(1L, "cliente@test.com"))
                .thenReturn(siniestro);

        ResponseEntity<?> respuesta =
                controller.obtener(1L, principal);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(1L, body.get("idSiniestro"));
        assertEquals("SIN-ABC12345", body.get("numeroSiniestro"));
        assertEquals("Toyota", body.get("marca"));
        assertEquals("Corolla", body.get("modelo"));
    }

    @Test
    void obtenerSiniestroNoEncontrado() {
        when(service.obtenerDelUsuario(99L, "cliente@test.com"))
                .thenThrow(new IllegalArgumentException(
                        "El siniestro no existe."));

        ResponseEntity<?> respuesta =
                controller.obtener(99L, principal);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals("El siniestro no existe.", body.get("mensaje"));
    }

    @Test
    void derivarSinPrincipal() {
        ResponseEntity<?> respuesta =
                controller.derivarAseguradora(1L, null);

        assertEquals(401, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals("Debe iniciar sesión.", body.get("mensaje"));

        verifyNoInteractions(gestion);
    }

    @Test
    void derivarSiniestroCorrectamente() {
        Siniestro siniestro = crearSiniestro();
        siniestro.setEstado("Derivado");

        when(gestion.derivar("cliente@test.com", 1L))
                .thenReturn(siniestro);

        ResponseEntity<?> respuesta =
                controller.derivarAseguradora(1L, principal);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(1L, body.get("idSiniestro"));
        assertEquals("SIN-ABC12345", body.get("numeroSiniestro"));
        assertEquals("Derivado", body.get("estado"));
        assertEquals(
                "Siniestro derivado correctamente a la aseguradora.",
                body.get("mensaje")
        );
    }

    @Test
    void derivarSiniestroConErrorDeValidacion() {
        when(gestion.derivar("cliente@test.com", 1L))
                .thenThrow(new IllegalArgumentException(
                        "El siniestro no existe."));

        ResponseEntity<?> respuesta =
                controller.derivarAseguradora(1L, principal);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals("El siniestro no existe.", body.get("mensaje"));
    }

    @Test
    void derivarSiniestroConErrorInterno() {
        when(gestion.derivar("cliente@test.com", 1L))
                .thenThrow(new RuntimeException("Error interno"));

        ResponseEntity<?> respuesta =
                controller.derivarAseguradora(1L, principal);

        assertEquals(500, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals(
                "No se pudo derivar el siniestro.",
                body.get("mensaje")
        );
    }

    @Test
    void listarPendientesSinPrincipal() {
        ResponseEntity<?> respuesta =
                controller.listarPendientesAsesor(null);

        assertEquals(401, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals("Debe iniciar sesión.", body.get("mensaje"));

        verifyNoInteractions(service);
    }

    @Test
    void listarPendientesDelAsesorCorrectamente() {
        Siniestro siniestro = crearSiniestro();

        when(service.listarPendientesAsesor())
                .thenReturn(List.of(siniestro));

        ResponseEntity<?> respuesta =
                controller.listarPendientesAsesor(principal);

        assertEquals(200, respuesta.getStatusCode().value());

        List<?> body = (List<?>) respuesta.getBody();
        assertEquals(1, body.size());

        Map<?, ?> item = (Map<?, ?>) body.get(0);
        assertEquals(1L, item.get("idSiniestro"));
        assertEquals("Toyota", item.get("marca"));
        assertEquals("Arequipa", item.get("lugar"));
    }

    @Test
    void listarPendientesConErrorInterno() {
        when(service.listarPendientesAsesor())
                .thenThrow(new RuntimeException("Error interno"));

        ResponseEntity<?> respuesta =
                controller.listarPendientesAsesor(principal);

        assertEquals(500, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals(
                "No se pudieron consultar los siniestros pendientes.",
                body.get("mensaje")
        );
    }

    @Test
    void obtenerParaRevisionSinPrincipal() {
        ResponseEntity<?> respuesta =
                controller.obtenerParaRevision(1L, null);

        assertEquals(401, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals("Debe iniciar sesión.", body.get("mensaje"));

        verifyNoInteractions(service);
    }

    @Test
    void obtenerParaRevisionCorrectamente() {
        Siniestro siniestro = crearSiniestro();

        Usuario usuario = mock(Usuario.class);
        when(usuario.getIdUsuario()).thenReturn(10L);
        when(usuario.getNombres()).thenReturn("Juan");
        when(usuario.getApellidos()).thenReturn("Perez");
        when(usuario.getCorreo()).thenReturn("juan@test.com");

        Aseguradora aseguradora = mock(Aseguradora.class);
        when(aseguradora.getIdAseguradora()).thenReturn(2);
        when(aseguradora.getNombre()).thenReturn("Aseguradora Test");

        siniestro.setUsuario(usuario);
        siniestro.setAseguradora(aseguradora);

        when(service.obtenerParaRevision(1L))
                .thenReturn(siniestro);

        ResponseEntity<?> respuesta =
                controller.obtenerParaRevision(1L, principal);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(1L, body.get("idSiniestro"));
        assertEquals("Juan Perez", body.get("nombreUsuario"));
        assertEquals("juan@test.com", body.get("correoUsuario"));
        assertEquals(2, body.get("idAseguradora"));
        assertEquals("Aseguradora Test", body.get("nombreAseguradora"));
    }

    @Test
    void obtenerParaRevisionConErrorDeValidacion() {
        when(service.obtenerParaRevision(99L))
                .thenThrow(new IllegalArgumentException(
                        "El siniestro indicado no existe."));

        ResponseEntity<?> respuesta =
                controller.obtenerParaRevision(99L, principal);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals(
                "El siniestro indicado no existe.",
                body.get("mensaje")
        );
    }

    @Test
    void obtenerParaRevisionConErrorInterno() {
        when(service.obtenerParaRevision(1L))
                .thenThrow(new RuntimeException("Error interno"));

        ResponseEntity<?> respuesta =
                controller.obtenerParaRevision(1L, principal);

        assertEquals(500, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals(
                "No se pudo consultar el siniestro.",
                body.get("mensaje")
        );
    }
}

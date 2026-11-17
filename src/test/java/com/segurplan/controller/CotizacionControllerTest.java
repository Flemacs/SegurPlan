
package com.segurplan.controller;

import com.segurplan.dto.CotizacionRequest;
import com.segurplan.dto.RespuestaCotizacionRequest;
import com.segurplan.model.*;
import com.segurplan.service.AccesoService;
import com.segurplan.service.CotizacionService;
import com.segurplan.service.NotificacionService;
import org.springframework.security.core.Authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CotizacionControllerTest {

    private CotizacionService cotizacionService;
    private CotizacionController controller;
    private AccesoService acceso;
    private NotificacionService notificaciones;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        cotizacionService = mock(CotizacionService.class);
        acceso = mock(AccesoService.class);
        notificaciones = mock(NotificacionService.class);
        auth = mock(Authentication.class);
        // por defecto el usuario en sesión es el dueño (id 1)
        when(acceso.puedeVer(auth, 1L)).thenReturn(true);
        controller = new CotizacionController(
                cotizacionService, acceso, notificaciones);
    }

    private Cotizacion crearCotizacion() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setNombres("Juan");
        usuario.setApellidos("Perez");

        TipoSeguro tipo = new TipoSeguro();
        tipo.setIdTipoSeguro(2);
        tipo.setNombre("Vehicular");

        Aseguradora aseguradora = new Aseguradora();
        aseguradora.setIdAseguradora(3);
        aseguradora.setNombre("Rimac");

        Cotizacion c = new Cotizacion();
        c.setIdCotizacion(10L);
        c.setUsuario(usuario);
        c.setTipoSeguro(tipo);
        c.setAseguradora(aseguradora);
        c.setFechaSolicitud(LocalDateTime.of(2026, 10, 10, 10, 0));
        c.setDescripcionSolicitud("Seguro para automóvil");
        c.setEstado("Disponible");
        c.setPrecio(new BigDecimal("500.00"));
        c.setDeducible(new BigDecimal("100.00"));
        c.setVigencia(LocalDate.of(2027, 10, 10));
        c.setCobertura("Cobertura completa");

        return c;
    }

    @Test
    void crearCotizacionExitosamente() {
        CotizacionRequest request = mock(CotizacionRequest.class);
        Cotizacion cotizacion = crearCotizacion();

        when(cotizacionService.crear(request)).thenReturn(cotizacion);

        ResponseEntity<?> response = controller.crear(request);

        assertEquals(200, response.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Cotización solicitada correctamente", body.get("mensaje"));
        assertEquals(10L, body.get("idCotizacion"));
        assertEquals("Disponible", body.get("estado"));

        verify(cotizacionService).crear(request);
    }

    @Test
    void crearCotizacionConError() {
        CotizacionRequest request = mock(CotizacionRequest.class);

        when(cotizacionService.crear(request))
                .thenThrow(new RuntimeException("Solicitud inválida"));

        ResponseEntity<?> response = controller.crear(request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Solicitud inválida",
                ((Map<?, ?>) response.getBody()).get("mensaje"));
    }

    @Test
    void buscarCotizacionPorIdExitosamente() {
        Cotizacion cotizacion = crearCotizacion();

        when(cotizacionService.buscarPorId(10L)).thenReturn(cotizacion);

        ResponseEntity<?> response = controller.buscarPorId(10L, auth);

        assertEquals(200, response.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertEquals(10L, body.get("idCotizacion"));
        assertEquals("Juan Perez", body.get("nombreSolicitante"));
        assertEquals("Vehicular", body.get("tipoSeguro"));
        assertEquals("Rimac", body.get("aseguradora"));
        assertEquals("Disponible", body.get("estado"));
        assertEquals(new BigDecimal("500.00"), body.get("precio"));

        verify(cotizacionService).buscarPorId(10L);
    }

    @Test
    void buscarCotizacionPorIdConError() {
        when(cotizacionService.buscarPorId(99L))
                .thenThrow(new RuntimeException("Cotización no encontrada"));

        ResponseEntity<?> response = controller.buscarPorId(99L, auth);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Cotización no encontrada",
                ((Map<?, ?>) response.getBody()).get("mensaje"));
    }

    @Test
    void listarCotizacionesPorUsuarioExitosamente() {
        List<Cotizacion> cotizaciones = List.of(crearCotizacion());

        when(cotizacionService.listarPorUsuario(1L))
                .thenReturn(cotizaciones);

        ResponseEntity<?> response =
                controller.listarPorUsuario(1L, auth);

        assertEquals(200, response.getStatusCode().value());
        List<?> lista = (List<?>) response.getBody();
        assertEquals(1, lista.size());
        assertEquals(10L, ((Cotizacion) lista.get(0)).getIdCotizacion());

        verify(cotizacionService).listarPorUsuario(1L);
    }

    @Test
    void listarTodasLasCotizacionesExitosamente() {
        when(cotizacionService.listarTodas())
                .thenReturn(List.of(crearCotizacion()));

        ResponseEntity<?> response = controller.listarTodas();

        assertEquals(200, response.getStatusCode().value());

        List<?> lista = (List<?>) response.getBody();
        assertEquals(1, lista.size());

        Map<?, ?> item = (Map<?, ?>) lista.get(0);

        assertEquals(10L, item.get("idCotizacion"));
        assertEquals("Juan Perez", item.get("cliente"));
        assertEquals("Vehicular", item.get("tipoSeguro"));
        assertEquals("Rimac", item.get("aseguradora"));
    }

    @Test
    void responderCotizacionExitosamente() {
        RespuestaCotizacionRequest request =
                mock(RespuestaCotizacionRequest.class);

        Cotizacion cotizacion = crearCotizacion();

        when(cotizacionService.responderCotizacion(10L, request))
                .thenReturn(cotizacion);

        ResponseEntity<?> response =
                controller.responderCotizacion(10L, request);

        assertEquals(200, response.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertEquals("Cotización actualizada correctamente",
                body.get("mensaje"));
        assertEquals(10L, body.get("idCotizacion"));
        assertEquals("Disponible", body.get("estado"));
        assertEquals("Cobertura completa", body.get("cobertura"));

        verify(cotizacionService).responderCotizacion(10L, request);
    }

    @Test
    void responderCotizacionConError() {
        RespuestaCotizacionRequest request =
                mock(RespuestaCotizacionRequest.class);

        when(cotizacionService.responderCotizacion(10L, request))
                .thenThrow(new RuntimeException("No se pudo responder"));

        ResponseEntity<?> response =
                controller.responderCotizacion(10L, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("No se pudo responder",
                ((Map<?, ?>) response.getBody()).get("mensaje"));
    }

    @Test
    void buscarCotizacionDeOtroUsuarioDevuelve403() {
        when(cotizacionService.buscarPorId(10L))
                .thenReturn(crearCotizacion());
        when(acceso.puedeVer(auth, 1L)).thenReturn(false);

        ResponseEntity<?> response = controller.buscarPorId(10L, auth);

        assertEquals(403, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertFalse(body.containsKey("idCotizacion"));
    }

    @Test
    void listarCotizacionesDeOtroUsuarioDevuelve403() {
        when(acceso.puedeVer(auth, 2L)).thenReturn(false);

        ResponseEntity<?> response = controller.listarPorUsuario(2L, auth);

        assertEquals(403, response.getStatusCode().value());
        verifyNoInteractions(cotizacionService);
    }
}

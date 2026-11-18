
package com.segurplan.controller;

import com.segurplan.model.Cotizacion;
import com.segurplan.model.Prevision;
import com.segurplan.model.Siniestro;
import com.segurplan.model.Usuario;
import com.segurplan.repository.ContratacionRepository;
import com.segurplan.repository.CotizacionRepository;
import com.segurplan.repository.PolizaRepository;
import com.segurplan.repository.UsuarioRepository;
import com.segurplan.service.PrevisionService;
import com.segurplan.service.SiniestroService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HistorialClienteControllerTest {

    private UsuarioRepository usuarioRepository;
    private CotizacionRepository cotizacionRepository;
    private ContratacionRepository contratacionRepository;
    private PolizaRepository polizaRepository;
    private SiniestroService siniestroService;
    private PrevisionService previsionService;

    private HistorialClienteController controller;
    private Principal principal;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        cotizacionRepository = mock(CotizacionRepository.class);
        contratacionRepository = mock(ContratacionRepository.class);
        polizaRepository = mock(PolizaRepository.class);
        siniestroService = mock(SiniestroService.class);
        previsionService = mock(PrevisionService.class);

        controller = new HistorialClienteController(
                usuarioRepository,
                cotizacionRepository,
                contratacionRepository,
                polizaRepository,
                siniestroService,
                previsionService
        );

        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("cliente@test.com");
    }

    @Test
    void listarUsuarioNoEncontradoRetorna400() {
        when(usuarioRepository.findByCorreo("cliente@test.com"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.listar(principal);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        verify(cotizacionRepository, never())
                .findByUsuario_IdUsuario(anyLong());
    }

    @Test
    void listarSinProcesosRetornaListaVacia() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);

        when(usuarioRepository.findByCorreo("cliente@test.com"))
                .thenReturn(Optional.of(usuario));
        when(cotizacionRepository.findByUsuario_IdUsuario(1L))
                .thenReturn(List.of());
        when(siniestroService.listarDelUsuario("cliente@test.com"))
                .thenReturn(List.of());
        when(previsionService.listarPorUsuario("cliente@test.com"))
                .thenReturn(List.of());

        ResponseEntity<?> response = controller.listar(principal);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List<?>) response.getBody()).isEmpty());
    }

    @Test
    void listarCotizacionDisponibleMuestraAccionDeCompra() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setIdCotizacion(12L);
        cotizacion.setEstado("Disponible");
        cotizacion.setFechaSolicitud(LocalDateTime.of(2026, 10, 10, 10, 0));

        when(usuarioRepository.findByCorreo("cliente@test.com"))
                .thenReturn(Optional.of(usuario));
        when(cotizacionRepository.findByUsuario_IdUsuario(1L))
                .thenReturn(List.of(cotizacion));
        when(contratacionRepository.findByCotizacion_IdCotizacion(12L))
                .thenReturn(Optional.empty());
        when(siniestroService.listarDelUsuario("cliente@test.com"))
                .thenReturn(List.of());
        when(previsionService.listarPorUsuario("cliente@test.com"))
                .thenReturn(List.of());

        ResponseEntity<?> response = controller.listar(principal);

        assertEquals(200, response.getStatusCode().value());

        List<?> filas = (List<?>) response.getBody();
        assertEquals(1, filas.size());

        Map<?, ?> fila = (Map<?, ?>) filas.get(0);
        assertEquals("COTIZACION", fila.get("tipo"));
        assertEquals(12L, fila.get("id"));
        assertEquals("Continuar con la compra", fila.get("accionTexto"));
        assertEquals("s05-poliza.html", fila.get("accionUrl"));
        assertEquals(true, fila.get("accionDestacada"));
    }

    @Test
    void listarSiniestroYPrevisionOrdenaPorFechaDescendente() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);

        Siniestro siniestro = new Siniestro();
        siniestro.setIdSiniestro(20L);
        siniestro.setNumeroSiniestro("SIN-020");
        siniestro.setEstado("Registrado");
        siniestro.setFechaRegistro(
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        Prevision prevision = new Prevision();
        prevision.setIdPrevision(30L);
        prevision.setTipoSistema("AFP");
        prevision.setEstado("Registrada");
        prevision.setFechaRegistro(
                LocalDateTime.of(2026, 10, 12, 10, 0)
        );

        when(usuarioRepository.findByCorreo("cliente@test.com"))
                .thenReturn(Optional.of(usuario));
        when(cotizacionRepository.findByUsuario_IdUsuario(1L))
                .thenReturn(List.of());
        when(siniestroService.listarDelUsuario("cliente@test.com"))
                .thenReturn(List.of(siniestro));
        when(previsionService.listarPorUsuario("cliente@test.com"))
                .thenReturn(List.of(prevision));

        ResponseEntity<?> response = controller.listar(principal);

        assertEquals(200, response.getStatusCode().value());

        List<?> filas = (List<?>) response.getBody();
        assertEquals(2, filas.size());

        Map<?, ?> primera = (Map<?, ?>) filas.get(0);
        Map<?, ?> segunda = (Map<?, ?>) filas.get(1);

        assertEquals("AFP", primera.get("tipo"));
        assertEquals("SINIESTRO", segunda.get("tipo"));

        assertEquals("Continuar: subir evidencias",
                segunda.get("accionTexto"));
        assertEquals("Continuar: solicitar orientación",
                primera.get("accionTexto"));
    }
}

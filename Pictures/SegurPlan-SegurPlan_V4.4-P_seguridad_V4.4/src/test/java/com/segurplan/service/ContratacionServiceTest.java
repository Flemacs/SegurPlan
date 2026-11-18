
package com.segurplan.service;

import com.segurplan.dto.ContratacionRequest;
import com.segurplan.model.Aseguradora;
import com.segurplan.model.Contratacion;
import com.segurplan.model.Cotizacion;
import com.segurplan.model.TipoSeguro;
import com.segurplan.model.Usuario;
import com.segurplan.repository.ContratacionRepository;
import com.segurplan.repository.CotizacionRepository;
import com.segurplan.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContratacionServiceTest {

    @Mock
    private ContratacionRepository contratacionRepository;

    @Mock
    private CotizacionRepository cotizacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ContratacionService contratacionService;

    private Usuario crearUsuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombres("Juan");
        usuario.setApellidos("Perez");
        return usuario;
    }

    private Cotizacion crearCotizacion(
            Long id,
            Usuario usuario,
            String estado) {

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setIdCotizacion(id);
        cotizacion.setUsuario(usuario);
        cotizacion.setEstado(estado);

        return cotizacion;
    }

    private ContratacionRequest crearRequest(
            Long idCotizacion,
            Long idUsuario,
            Boolean acepta) {

        ContratacionRequest request = new ContratacionRequest();
        request.setIdCotizacion(idCotizacion);
        request.setIdUsuario(idUsuario);
        request.setAceptaCondiciones(acepta);

        return request;
    }

    // =====================================================
    // CREAR CONTRATACIÓN
    // =====================================================

    @Test
    void crear_datosValidos_creaContratacion() {

        Usuario usuario = crearUsuario(1L);
        Cotizacion cotizacion =
                crearCotizacion(10L, usuario, "Disponible");

        ContratacionRequest request =
                crearRequest(10L, 1L, true);

        when(cotizacionRepository.findById(10L))
                .thenReturn(Optional.of(cotizacion));

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        when(contratacionRepository
                .findByCotizacion_IdCotizacion(10L))
                .thenReturn(Optional.empty());

        when(contratacionRepository.save(any(Contratacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime antes = LocalDateTime.now();

        Contratacion resultado =
                contratacionService.crear(request);

        LocalDateTime despues = LocalDateTime.now();

        assertNotNull(resultado);
        assertEquals(cotizacion, resultado.getCotizacion());
        assertEquals(usuario, resultado.getUsuario());
        assertEquals("1.0", resultado.getVersionCondiciones());
        assertTrue(resultado.getCondicionesAceptadas());
        assertEquals("Iniciada", resultado.getEstado());

        assertNotNull(resultado.getFechaAceptacion());
        assertFalse(resultado.getFechaAceptacion().isBefore(antes));
        assertFalse(resultado.getFechaAceptacion().isAfter(despues));

        assertEquals("Aceptada", cotizacion.getEstado());

        verify(contratacionRepository).save(any(Contratacion.class));
        verify(cotizacionRepository).save(cotizacion);
    }

    @Test
    void crear_cotizacionNoExiste_lanzaExcepcion() {

        ContratacionRequest request =
                crearRequest(10L, 1L, true);

        when(cotizacionRepository.findById(10L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> contratacionService.crear(request)
        );

        assertEquals("Cotización no encontrada", exception.getMessage());

        verify(usuarioRepository, never()).findById(anyLong());
        verify(contratacionRepository, never()).save(any());
    }

    @Test
    void crear_usuarioNoExiste_lanzaExcepcion() {

        Usuario usuario = crearUsuario(1L);

        Cotizacion cotizacion =
                crearCotizacion(10L, usuario, "Disponible");

        ContratacionRequest request =
                crearRequest(10L, 1L, true);

        when(cotizacionRepository.findById(10L))
                .thenReturn(Optional.of(cotizacion));

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> contratacionService.crear(request)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());

        verify(contratacionRepository, never()).save(any());
    }

    @Test
    void crear_cotizacionPerteneceAOtroUsuario_lanzaExcepcion() {

        Usuario propietario = crearUsuario(1L);
        Usuario otroUsuario = crearUsuario(2L);

        Cotizacion cotizacion =
                crearCotizacion(10L, propietario, "Disponible");

        ContratacionRequest request =
                crearRequest(10L, 2L, true);

        when(cotizacionRepository.findById(10L))
                .thenReturn(Optional.of(cotizacion));

        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(otroUsuario));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> contratacionService.crear(request)
        );

        assertEquals(
                "La cotización no pertenece al usuario",
                exception.getMessage()
        );

        verify(contratacionRepository, never()).save(any());
    }

    @Test
    void crear_cotizacionNoDisponible_lanzaExcepcion() {

        Usuario usuario = crearUsuario(1L);

        Cotizacion cotizacion =
                crearCotizacion(10L, usuario, "Aceptada");

        ContratacionRequest request =
                crearRequest(10L, 1L, true);

        when(cotizacionRepository.findById(10L))
                .thenReturn(Optional.of(cotizacion));

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> contratacionService.crear(request)
        );

        assertEquals(
                "La cotización no está disponible para contratación",
                exception.getMessage()
        );

        verify(contratacionRepository, never()).save(any());
    }

    @Test
    void crear_noAceptaCondiciones_lanzaExcepcion() {

        Usuario usuario = crearUsuario(1L);

        Cotizacion cotizacion =
                crearCotizacion(10L, usuario, "Disponible");

        ContratacionRequest request =
                crearRequest(10L, 1L, false);

        when(cotizacionRepository.findById(10L))
                .thenReturn(Optional.of(cotizacion));

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> contratacionService.crear(request)
        );

        assertEquals(
                "Debe aceptar las condiciones",
                exception.getMessage()
        );

        verify(contratacionRepository, never()).save(any());
    }

    @Test
    void crear_aceptacionNula_lanzaExcepcion() {

        Usuario usuario = crearUsuario(1L);

        Cotizacion cotizacion =
                crearCotizacion(10L, usuario, "Disponible");

        ContratacionRequest request =
                crearRequest(10L, 1L, null);

        when(cotizacionRepository.findById(10L))
                .thenReturn(Optional.of(cotizacion));

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> contratacionService.crear(request)
        );

        assertEquals(
                "Debe aceptar las condiciones",
                exception.getMessage()
        );

        verify(contratacionRepository, never()).save(any());
    }

    @Test
    void crear_cotizacionYaTieneContratacion_lanzaExcepcion() {

        Usuario usuario = crearUsuario(1L);

        Cotizacion cotizacion =
                crearCotizacion(10L, usuario, "Disponible");

        Contratacion existente = new Contratacion();

        ContratacionRequest request =
                crearRequest(10L, 1L, true);

        when(cotizacionRepository.findById(10L))
                .thenReturn(Optional.of(cotizacion));

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        when(contratacionRepository
                .findByCotizacion_IdCotizacion(10L))
                .thenReturn(Optional.of(existente));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> contratacionService.crear(request)
        );

        assertEquals(
                "Esta cotización ya tiene una contratación",
                exception.getMessage()
        );

        verify(contratacionRepository, never()).save(any());
        verify(cotizacionRepository, never()).save(any());
    }

    // =====================================================
    // LISTAR CONTRATACIONES PENDIENTES
    // =====================================================

    @Test
    void listarPendientes_conContrataciones_retornaDatosCorrectos() {

        Usuario usuario = crearUsuario(1L);

        Cotizacion cotizacion =
                crearCotizacion(10L, usuario, "Aceptada");

        TipoSeguro tipoSeguro = new TipoSeguro();
        tipoSeguro.setNombre("Seguro vehicular");

        Aseguradora aseguradora = new Aseguradora();
        aseguradora.setNombre("Aseguradora Nacional");

        cotizacion.setTipoSeguro(tipoSeguro);
        cotizacion.setAseguradora(aseguradora);

        LocalDateTime fecha = LocalDateTime.of(
                2026, 11, 10, 10, 30
        );

        Contratacion contratacion = new Contratacion();
        contratacion.setIdContratacion(100L);
        contratacion.setCotizacion(cotizacion);
        contratacion.setUsuario(usuario);
        contratacion.setFechaContratacion(fecha);

        when(contratacionRepository.findPendientesDePoliza())
                .thenReturn(List.of(contratacion));

        List<Map<String, Object>> resultado =
                contratacionService.listarPendientes();

        assertEquals(1, resultado.size());

        Map<String, Object> item = resultado.get(0);

        assertEquals(100L, item.get("idContratacion"));
        assertEquals(10L, item.get("idCotizacion"));
        assertEquals("Juan Perez", item.get("cliente"));
        assertEquals("Seguro vehicular", item.get("tipoSeguro"));
        assertEquals("Aseguradora Nacional", item.get("aseguradora"));
        assertEquals(fecha, item.get("fechaContratacion"));

        verify(contratacionRepository).findPendientesDePoliza();
    }

    @Test
    void listarPendientes_sinContrataciones_retornaListaVacia() {

        when(contratacionRepository.findPendientesDePoliza())
                .thenReturn(List.of());

        List<Map<String, Object>> resultado =
                contratacionService.listarPendientes();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        verify(contratacionRepository).findPendientesDePoliza();
    }
}

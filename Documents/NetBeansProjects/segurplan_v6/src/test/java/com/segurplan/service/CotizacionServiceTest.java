package com.segurplan.service;

import com.segurplan.dto.CotizacionRequest;
import com.segurplan.dto.RespuestaCotizacionRequest;
import com.segurplan.model.Aseguradora;
import com.segurplan.model.Cotizacion;
import com.segurplan.model.TipoSeguro;
import com.segurplan.model.Usuario;
import com.segurplan.repository.AseguradoraRepository;
import com.segurplan.repository.CotizacionRepository;
import com.segurplan.repository.TipoSeguroRepository;
import com.segurplan.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de las reglas de negocio de CotizacionService.
 * Cubre el caso de prueba CP002 (Requisitos funcionales RF005 y RF006).
 *
 * Utiliza Mockito para simular los repositorios y el servicio de auditoría,
 * garantizando la ejecución aislada en memoria sin base de datos real.
 */
@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    @Mock
    private CotizacionRepository cotizacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TipoSeguroRepository tipoSeguroRepository;

    @Mock
    private AseguradoraRepository aseguradoraRepository;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private CotizacionService cotizacionService;

    // =====================================================
    // 1. SOLICITUD DE COTIZACIÓN (RF005 / CP002)
    // =====================================================

    /**
     * Prueba 1: Validación de usuario inexistente al solicitar cotización.
     * Si el usuario no existe en el sistema, la solicitud debe abortarse inmediatamente.
     */
    @Test
    void crear_usuarioInexistente_lanzaExcepcion() {
        // Arrange (Preparar datos)
        CotizacionRequest request = new CotizacionRequest();
        request.setIdUsuario(999L);
        request.setIdTipoSeguro(1);
        request.setIdAseguradora(1);
        request.setDescripcionSolicitud("Solicitud de prueba");

        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert (Ejecutar y comprobar)
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> cotizacionService.crear(request));

        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(cotizacionRepository, never()).save(any(Cotizacion.class));
        verifyNoInteractions(historialService);
    }

    /**
     * Prueba 2: Procesamiento correcto de una solicitud de cotización válida.
     * Comprueba que se asocien correctamente usuario, tipo de seguro y aseguradora,
     * se guarde la entidad y se registre la trazabilidad en auditoría.
     */
    @Test
    void crear_datosValidos_guardaCotizacionYRegistraHistorial() {
        // Arrange
        Long idUsuario = 1L;
        Integer idTipoSeguro = 2;
        Integer idAseguradora = 3;

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);

        TipoSeguro tipoSeguro = new TipoSeguro();
        tipoSeguro.setIdTipoSeguro(idTipoSeguro);
        tipoSeguro.setNombre("Vehicular Todo Riesgo");

        Aseguradora aseguradora = new Aseguradora();
        aseguradora.setIdAseguradora(idAseguradora);
        aseguradora.setNombre("Rímac Seguros");

        CotizacionRequest request = new CotizacionRequest();
        request.setIdUsuario(idUsuario);
        request.setIdTipoSeguro(idTipoSeguro);
        request.setIdAseguradora(idAseguradora);
        request.setDescripcionSolicitud("Toyota Corolla 2023 - Cobertura completa");

        when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(tipoSeguroRepository.findById(idTipoSeguro)).thenReturn(Optional.of(tipoSeguro));
        when(aseguradoraRepository.findById(idAseguradora)).thenReturn(Optional.of(aseguradora));

        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(inv -> {
            Cotizacion c = inv.getArgument(0);
            c.setIdCotizacion(25L);
            return c;
        });

        // Act
        Cotizacion resultado = cotizacionService.crear(request);

        // Assert
        assertNotNull(resultado);
        assertEquals(25L, resultado.getIdCotizacion());
        assertEquals(usuario, resultado.getUsuario());
        assertEquals(tipoSeguro, resultado.getTipoSeguro());
        assertEquals(aseguradora, resultado.getAseguradora());
        assertEquals("Toyota Corolla 2023 - Cobertura completa", resultado.getDescripcionSolicitud());

        verify(cotizacionRepository).save(any(Cotizacion.class));
        verify(historialService).registrar(
                eq(idUsuario),
                eq("COTIZACION"),
                eq(25L),
                eq("CREAR"),
                isNull(),
                eq("Cotización registrada"));
    }

    // =====================================================
    // 2. EVALUACIÓN Y RESPUESTA DEL ASESOR (RF006 / CP002)
    // =====================================================

    /**
     * Prueba 3: Validación de estados permitidos al responder una cotización.
     * El asesor únicamente puede cambiar el estado a 'En revisión' o 'Disponible'.
     * Cualquier otro estado debe ser rechazado.
     */
    @Test
    void responder_estadoInvalido_lanzaExcepcion() {
        // Arrange
        Long idCotizacion = 10L;
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setIdCotizacion(idCotizacion);
        cotizacion.setEstado("Solicitada");

        when(cotizacionRepository.findById(idCotizacion)).thenReturn(Optional.of(cotizacion));

        RespuestaCotizacionRequest request = new RespuestaCotizacionRequest();
        request.setEstado("Cancelada"); // Estado no permitido para la respuesta del asesor

        // Act & Assert
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> cotizacionService.responderCotizacion(idCotizacion, request));

        assertEquals("Estado de cotización no válido", ex.getMessage());
        verify(cotizacionRepository, never()).save(any(Cotizacion.class));
    }

    /**
     * Prueba 4: Rechazo de deducibles con valor negativo.
     * Comprueba que las condiciones comerciales del asesor no admitan deducibles inferiores a cero.
     */
    @Test
    void responder_deducibleNegativo_lanzaExcepcion() {
        // Arrange
        Long idCotizacion = 10L;
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setIdCotizacion(idCotizacion);
        cotizacion.setEstado("Solicitada");

        when(cotizacionRepository.findById(idCotizacion)).thenReturn(Optional.of(cotizacion));

        RespuestaCotizacionRequest request = new RespuestaCotizacionRequest();
        request.setEstado("En revisión");
        request.setDeducible(new BigDecimal("-100.00")); // Deducible negativo inválido

        // Act & Assert
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> cotizacionService.responderCotizacion(idCotizacion, request));

        assertEquals("El deducible no puede ser negativo", ex.getMessage());
        verify(cotizacionRepository, never()).save(any(Cotizacion.class));
    }

    /**
     * Prueba 5: Rechazo de publicación sin precio válido (menor o igual a cero).
     * Para publicar una cotización como 'Disponible' es obligatorio fijar una prima comercial mayor a cero.
     */
    @Test
    void responder_publicarSinPrecioValido_lanzaExcepcion() {
        // Arrange
        Long idCotizacion = 10L;
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setIdCotizacion(idCotizacion);
        cotizacion.setEstado("Solicitada");
        cotizacion.setPrecio(null);

        when(cotizacionRepository.findById(idCotizacion)).thenReturn(Optional.of(cotizacion));

        RespuestaCotizacionRequest request = new RespuestaCotizacionRequest();
        request.setEstado("Disponible");
        request.setPrecio(BigDecimal.ZERO); // Precio cero: no se puede publicar

        // Act & Assert
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> cotizacionService.responderCotizacion(idCotizacion, request));

        assertEquals("Ingrese un precio mayor a 0 antes de publicar", ex.getMessage());
        verify(cotizacionRepository, never()).save(any(Cotizacion.class));
    }

    /**
     * Prueba 6: Procesamiento correcto de respuesta y publicación comercial.
     * Comprueba que al ingresar datos válidos la cotización pase a 'Disponible',
     * se actualicen precio, deducible, cobertura, vigencia, fecha de respuesta y la auditoría.
     */
    @Test
    void responder_datosValidos_publicaCotizacionDisponibleYRegistraHistorial() {
        // Arrange
        Long idCotizacion = 10L;
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(3L);

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setIdCotizacion(idCotizacion);
        cotizacion.setUsuario(usuario);
        cotizacion.setEstado("Solicitada");

        when(cotizacionRepository.findById(idCotizacion)).thenReturn(Optional.of(cotizacion));
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));

        RespuestaCotizacionRequest request = new RespuestaCotizacionRequest();
        request.setEstado("Disponible");
        request.setPrecio(new BigDecimal("450.00"));
        request.setDeducible(new BigDecimal("100.00"));
        request.setCobertura("Cobertura amplia contra accidentes y robos");
        request.setVigencia(LocalDate.now().plusYears(1));

        // Act
        Cotizacion resultado = cotizacionService.responderCotizacion(idCotizacion, request);

        // Assert
        assertNotNull(resultado);
        assertEquals("Disponible", resultado.getEstado());
        assertEquals(new BigDecimal("450.00"), resultado.getPrecio());
        assertEquals(new BigDecimal("100.00"), resultado.getDeducible());
        assertEquals("Cobertura amplia contra accidentes y robos", resultado.getCobertura());
        assertNotNull(resultado.getFechaRespuesta());

        verify(cotizacionRepository).save(cotizacion);
        verify(historialService).registrar(
                eq(3L),
                eq("COTIZACION"),
                eq(idCotizacion),
                eq("ACTUALIZAR"),
                eq("Estado: Solicitada"),
                eq("Estado: Disponible"));
    }
}

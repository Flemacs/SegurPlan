package com.segurplan.service;

import com.segurplan.dto.PrevisionRequest;
import com.segurplan.model.Prevision;
import com.segurplan.model.Usuario;
import com.segurplan.repository.PrevisionRepository;
import com.segurplan.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de las reglas de negocio de PrevisionService.
 * Cubre el caso de prueba CP006 y los requisitos funcionales RF013, RF014, RF015, RF016 y RF017.
 *
 * Utiliza Mockito para simular los repositorios en memoria sin base de datos real.
 */
@ExtendWith(MockitoExtension.class)
class PrevisionServiceTest {

    private static final String CORREO_USUARIO = "afiliado@segurplan.pe";
    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PREVISION = 10L;

    @Mock
    private PrevisionRepository previsionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PrevisionService previsionService;

    // =====================================================
    // DATOS DE PRUEBA AUXILIARES
    // =====================================================

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(ID_USUARIO);
        usuario.setCorreo(CORREO_USUARIO);
        return usuario;
    }

    private PrevisionRequest crearRequestBase() {
        PrevisionRequest request = new PrevisionRequest();
        request.setEdad(45);
        request.setIngresoMensual(new BigDecimal("3000.00"));
        request.setAniosAporte(20);
        request.setConsulta("¿Cuánto recibiría de pensión con 20 años de aportes?");
        return request;
    }

    // =====================================================
    // 1. VALIDACIONES DE ENTRADA (RF013 / CP006)
    // =====================================================

    /**
     * Prueba 1: Rechazo si la edad ingresada es menor a 18 años o mayor a 70 años.
     * Comprueba que se lance IllegalArgumentException ante edades fuera de rango laboral.
     */
    @Test
    void simular_edadMenorA18_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByCorreo(CORREO_USUARIO))
                .thenReturn(Optional.of(crearUsuario()));

        PrevisionRequest request = crearRequestBase();
        request.setEdad(16); // Edad inválida

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> previsionService.simular(CORREO_USUARIO, request));

        assertEquals("La edad debe estar entre 18 y 70 años.", ex.getMessage());
        verify(previsionRepository, never()).save(any(Prevision.class));
    }

    /**
     * Prueba 2: Rechazo si los años de aporte son incoherentes con la edad.
     * Los años aportados no pueden exceder (edad - 18).
     */
    @Test
    void simular_aniosAporteIncoherenteConEdad_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByCorreo(CORREO_USUARIO))
                .thenReturn(Optional.of(crearUsuario()));

        PrevisionRequest request = crearRequestBase();
        request.setEdad(25);
        request.setAniosAporte(10); // 25 - 18 = 7 años máx. Posibles: 10 > 7

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> previsionService.simular(CORREO_USUARIO, request));

        assertEquals("Los años de aporte no son coherentes con la edad ingresada.", ex.getMessage());
        verify(previsionRepository, never()).save(any(Prevision.class));
    }

    /**
     * Prueba 3: Rechazo si el ingreso mensual es menor o igual a cero.
     * No se puede calcular una simulación previsional sin un ingreso positivo.
     */
    @Test
    void simular_ingresoMenorOIgualACero_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByCorreo(CORREO_USUARIO))
                .thenReturn(Optional.of(crearUsuario()));

        PrevisionRequest request = crearRequestBase();
        request.setIngresoMensual(BigDecimal.ZERO); // Ingreso inválido

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> previsionService.simular(CORREO_USUARIO, request));

        assertEquals("El ingreso mensual debe ser mayor que cero.", ex.getMessage());
        verify(previsionRepository, never()).save(any(Prevision.class));
    }

    // =====================================================
    // 2. SIMULACIÓN Y ESTIMACIÓN MATEMÁTICA (RF014, RF015 / CP006)
    // =====================================================

    /**
     * Prueba 4: Verificación del cálculo matemático de la pensión y persistencia de simulación.
     * Fórmula:
     *   - mesesAportados = 20 * 12 = 240
     *   - fondoEstimado = 3000 * 0.10 * 240 = S/ 72,000.00
     *   - pension = 72000.00 / 240 = S/ 300.00
     * Estado inicial resultante: 'Simulada' y tipo de sistema 'AFP'.
     */
    @Test
    void simular_datosValidos_calculaPensionYGuardaSimulacion() {
        // Arrange
        Usuario usuario = crearUsuario();
        when(usuarioRepository.findByCorreo(CORREO_USUARIO))
                .thenReturn(Optional.of(usuario));

        when(previsionRepository.save(any(Prevision.class)))
                .thenAnswer(inv -> {
                    Prevision p = inv.getArgument(0);
                    p.setIdPrevision(ID_PREVISION);
                    return p;
                });

        PrevisionRequest request = crearRequestBase();

        // Act
        Prevision resultado = previsionService.simular(CORREO_USUARIO, request);

        // Assert
        assertNotNull(resultado);
        assertEquals(ID_PREVISION, resultado.getIdPrevision());
        assertEquals("AFP", resultado.getTipoSistema());
        assertEquals("Simulada", resultado.getEstado());
        assertEquals(new BigDecimal("300.00"), resultado.getMontoPensionEstimado());
        assertTrue(resultado.getResultadoSimulacion().contains("Pensión mensual estimada: S/ 300.00"));
        assertTrue(resultado.getResultadoSimulacion().contains("Edad: 45 años"));

        verify(previsionRepository).save(any(Prevision.class));
    }

    // =====================================================
    // 3. SOLICITUD Y GESTIÓN DE ORIENTACIÓN (RF016, RF017 / CP006)
    // =====================================================

    /**
     * Prueba 5: Solicitud de orientación previsional por parte del cliente.
     * Comprueba que una simulación en estado 'Simulada' pase a estado 'En revisión'.
     */
    @Test
    void solicitarOrientacion_estadoSimulada_cambiaAEnRevision() {
        // Arrange
        Usuario usuario = crearUsuario();
        Prevision prevision = new Prevision();
        prevision.setIdPrevision(ID_PREVISION);
        prevision.setUsuario(usuario);
        prevision.setEstado("Simulada");

        when(previsionRepository.findById(ID_PREVISION))
                .thenReturn(Optional.of(prevision));
        when(previsionRepository.save(any(Prevision.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        Prevision resultado = previsionService.solicitarOrientacion(ID_PREVISION, CORREO_USUARIO);

        // Assert
        assertNotNull(resultado);
        assertEquals("En revisión", resultado.getEstado());
        verify(previsionRepository).save(prevision);
    }

    /**
     * Prueba 6: Respuesta del asesor previsional a la solicitud de orientación.
     * Comprueba que la consulta pase a estado 'Respondida' y se registre el texto de asesoría.
     */
    @Test
    void responderOrientacion_datosValidos_guardaRespuestaYCambiaARespondida() {
        // Arrange
        Prevision prevision = new Prevision();
        prevision.setIdPrevision(ID_PREVISION);
        prevision.setEstado("En revisión");

        when(previsionRepository.findById(ID_PREVISION))
                .thenReturn(Optional.of(prevision));
        when(previsionRepository.save(any(Prevision.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String respuestaAsesor = "Estimado afiliado, le recomendamos continuar aportando para alcanzar una mayor tasa de reemplazo.";

        // Act
        Prevision resultado = previsionService.responderOrientacion(ID_PREVISION, respuestaAsesor);

        // Assert
        assertNotNull(resultado);
        assertEquals("Respondida", resultado.getEstado());
        assertEquals(respuestaAsesor, resultado.getRespuesta());
        verify(previsionRepository).save(prevision);
    }
}

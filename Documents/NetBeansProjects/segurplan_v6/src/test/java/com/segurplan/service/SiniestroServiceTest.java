package com.segurplan.service;

import com.segurplan.dto.SiniestroRequest;
import com.segurplan.model.Aseguradora;
import com.segurplan.model.Siniestro;
import com.segurplan.model.Usuario;
import com.segurplan.repository.AseguradoraRepository;
import com.segurplan.repository.SiniestroRepository;
import com.segurplan.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de las reglas de negocio de SiniestroService.
 * Cubre el caso de prueba CP005 y los requisitos funcionales RF018, RF019, RF020, RF021, RF024, RF026 y RF033.
 *
 * Utiliza Mockito para simular las consultas y persistencia en memoria sin conexión a base de datos real.
 */
@ExtendWith(MockitoExtension.class)
class SiniestroServiceTest {

    private static final String CORREO_USUARIO = "conductor@segurplan.pe";
    private static final Long ID_USUARIO = 2L;
    private static final Long ID_SINIESTRO = 50L;
    private static final Integer ID_ASEGURADORA = 1;

    @Mock
    private SiniestroRepository siniestroRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AseguradoraRepository aseguradoraRepository;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private SiniestroService siniestroService;

    // =====================================================
    // DATOS DE PRUEBA AUXILIARES
    // =====================================================

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(ID_USUARIO);
        usuario.setCorreo(CORREO_USUARIO);
        return usuario;
    }

    private Aseguradora crearAseguradora() {
        Aseguradora aseguradora = new Aseguradora();
        aseguradora.setIdAseguradora(ID_ASEGURADORA);
        aseguradora.setNombre("Pacífico Seguros");
        return aseguradora;
    }

    private SiniestroRequest crearRequestBase() {
        SiniestroRequest request = new SiniestroRequest();
        request.setFechaAccidente(LocalDate.now().minusDays(2)); // Hace 2 días
        request.setIdAseguradora(ID_ASEGURADORA);
        request.setPlaca("abc-123");
        request.setMarca("Toyota");
        request.setModelo("Yaris");
        request.setAnio(2021);
        request.setLugar("Av. Javier Prado Este 2500, San Borja");
        request.setDescripcion("Colisión posterior leve contra vehículo de transporte.");
        request.setTipoSiniestro("Choque");
        request.setObservaciones("Conductor ileso, daños materiales en parachoques.");
        return request;
    }

    // =====================================================
    // 1. REGISTRO Y VALIDACIÓN DE DATOS (RF018, RF019, RF020, RF021, RF033 / CP005)
    // =====================================================

    /**
     * Prueba 1: Registro exitoso de siniestro con datos válidos.
     * Comprueba la generación del código único (SIN-XXXXXXXX), la normalización
     * de la placa en mayúsculas, el estado inicial 'Registrado' y la trazabilidad.
     */
    @Test
    void registrar_datosValidos_creaNumeroSiniestroYRegistraHistorial() {
        // Arrange (Preparar datos)
        when(usuarioRepository.findByCorreo(CORREO_USUARIO))
                .thenReturn(Optional.of(crearUsuario()));
        when(aseguradoraRepository.findById(ID_ASEGURADORA))
                .thenReturn(Optional.of(crearAseguradora()));

        when(siniestroRepository.save(any(Siniestro.class))).thenAnswer(inv -> {
            Siniestro s = inv.getArgument(0);
            s.setIdSiniestro(ID_SINIESTRO);
            return s;
        });

        SiniestroRequest request = crearRequestBase();

        // Act (Ejecutar)
        Siniestro resultado = siniestroService.registrar(CORREO_USUARIO, request);

        // Assert (Comprobar)
        assertNotNull(resultado);
        assertEquals(ID_SINIESTRO, resultado.getIdSiniestro());
        assertEquals("ABC-123", resultado.getPlaca()); // Placa limpia en mayúsculas
        assertEquals("Toyota", resultado.getMarca());
        assertEquals("Registrado", resultado.getEstado());
        assertNotNull(resultado.getNumeroSiniestro());
        assertTrue(resultado.getNumeroSiniestro().startsWith("SIN-"));

        verify(siniestroRepository).save(any(Siniestro.class));
        verify(historialService).registrar(
                eq(ID_USUARIO),
                eq("SINIESTRO"),
                eq(ID_SINIESTRO),
                eq("CREAR"),
                isNull(),
                contains("Pacífico Seguros"));
    }

    /**
     * Prueba 2: Rechazo de fecha de accidente futura.
     * Comprueba que la fecha del siniestro no pueda ser posterior al día de hoy.
     */
    @Test
    void registrar_fechaAccidenteFutura_lanzaExcepcion() {
        // Arrange
        SiniestroRequest request = crearRequestBase();
        request.setFechaAccidente(LocalDate.now().plusDays(1)); // Fecha futura inválida

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> siniestroService.registrar(CORREO_USUARIO, request));

        assertEquals("La fecha del accidente no puede ser futura.", ex.getMessage());
        verify(siniestroRepository, never()).save(any(Siniestro.class));
        verifyNoInteractions(historialService);
    }

    /**
     * Prueba 3: Rechazo de año de fabricación del vehículo fuera de rango.
     * Comprueba que años inferiores a 1900 sean rechazados por incoherencia.
     */
    @Test
    void registrar_anioVehiculoInvalido_lanzaExcepcion() {
        // Arrange
        SiniestroRequest request = crearRequestBase();
        request.setAnio(1850); // Año anterior a 1900

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> siniestroService.registrar(CORREO_USUARIO, request));

        assertEquals("El año del vehículo no es válido.", ex.getMessage());
        verify(siniestroRepository, never()).save(any(Siniestro.class));
        verifyNoInteractions(historialService);
    }

    /**
     * Prueba 4: Rechazo si la aseguradora seleccionada no existe en el catálogo.
     * Valida la relación referencial obligatoria entre el siniestro y la aseguradora.
     */
    @Test
    void registrar_aseguradoraInexistente_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByCorreo(CORREO_USUARIO))
                .thenReturn(Optional.of(crearUsuario()));
        when(aseguradoraRepository.findById(99))
                .thenReturn(Optional.empty());

        SiniestroRequest request = crearRequestBase();
        request.setIdAseguradora(99); // Aseguradora inexistente

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> siniestroService.registrar(CORREO_USUARIO, request));

        assertEquals("La aseguradora seleccionada no existe.", ex.getMessage());
        verify(siniestroRepository, never()).save(any(Siniestro.class));
        verifyNoInteractions(historialService);
    }

    // =====================================================
    // 2. DERIVACIÓN A ASEGURADORA (RF024, RF026 / CP005)
    // =====================================================

    /**
     * Prueba 5: Derivación correcta del siniestro a la aseguradora.
     * Comprueba que un siniestro en estado 'Registrado' pase a estado 'Derivado'.
     */
    @Test
    void derivarAseguradora_estadoRegistrado_cambiaADerivado() {
        // Arrange
        Usuario usuario = crearUsuario();
        when(usuarioRepository.findByCorreo(CORREO_USUARIO))
                .thenReturn(Optional.of(usuario));

        Siniestro siniestro = new Siniestro();
        siniestro.setIdSiniestro(ID_SINIESTRO);
        siniestro.setEstado("Registrado");
        siniestro.setAseguradora(crearAseguradora());

        when(siniestroRepository.findById(ID_SINIESTRO))
                .thenReturn(Optional.of(siniestro));
        when(siniestroRepository.save(any(Siniestro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        Siniestro resultado = siniestroService.derivarAseguradora(CORREO_USUARIO, ID_SINIESTRO);

        // Assert
        assertNotNull(resultado);
        assertEquals("Derivado", resultado.getEstado());
        verify(siniestroRepository).save(siniestro);
    }

    /**
     * Prueba 6: Rechazo de derivación desde un estado no permitido.
     * Comprueba que un siniestro en estado terminal como 'Cerrado' no pueda ser derivado.
     */
    @Test
    void derivarAseguradora_estadoNoPermitido_lanzaExcepcion() {
        // Arrange
        Usuario usuario = crearUsuario();
        when(usuarioRepository.findByCorreo(CORREO_USUARIO))
                .thenReturn(Optional.of(usuario));

        Siniestro siniestro = new Siniestro();
        siniestro.setIdSiniestro(ID_SINIESTRO);
        siniestro.setEstado("Cerrado"); // Estado no derivable
        siniestro.setAseguradora(crearAseguradora());

        when(siniestroRepository.findById(ID_SINIESTRO))
                .thenReturn(Optional.of(siniestro));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> siniestroService.derivarAseguradora(CORREO_USUARIO, ID_SINIESTRO));

        assertTrue(ex.getMessage().contains("El siniestro no puede ser derivado desde el estado actual: Cerrado"));
        verify(siniestroRepository, never()).save(any(Siniestro.class));
    }
}

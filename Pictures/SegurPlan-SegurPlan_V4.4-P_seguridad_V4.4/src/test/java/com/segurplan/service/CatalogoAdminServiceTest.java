
package com.segurplan.service;

import com.segurplan.dto.AseguradoraRequest;
import com.segurplan.dto.TipoSeguroRequest;
import com.segurplan.model.Aseguradora;
import com.segurplan.model.TipoSeguro;
import com.segurplan.model.Usuario;
import com.segurplan.repository.AseguradoraRepository;
import com.segurplan.repository.TipoSeguroRepository;
import com.segurplan.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoAdminServiceTest {

    @Mock
    private AseguradoraRepository aseguradoraRepository;

    @Mock
    private TipoSeguroRepository tipoSeguroRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private CatalogoAdminService service;

    private static final String ADMIN = "admin@segurplan.pe";

    private AseguradoraRequest aseguradoraRequest(
            String nombre, String ruc, String correo) {

        AseguradoraRequest r = new AseguradoraRequest();
        r.setNombre(nombre);
        r.setRuc(ruc);
        r.setTelefono("987654321");
        r.setCorreo(correo);
        return r;
    }

    private TipoSeguroRequest tipoRequest(
            String nombre, String descripcion) {

        TipoSeguroRequest r = new TipoSeguroRequest();
        r.setNombre(nombre);
        r.setDescripcion(descripcion);
        return r;
    }

    private void configurarAdmin() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);

        when(usuarioRepository.findByCorreo(ADMIN))
                .thenReturn(Optional.of(usuario));
    }

    // =====================================================
    // ASEGURADORAS
    // =====================================================

    @Test
    void listarAseguradoras_retornaLista() {
        when(aseguradoraRepository.findAll())
                .thenReturn(List.of(new Aseguradora()));

        List<Aseguradora> resultado = service.listarAseguradoras();

        assertEquals(1, resultado.size());
        verify(aseguradoraRepository).findAll();
    }

    @Test
    void crearAseguradora_datosValidos_guardaCorrectamente() {
        configurarAdmin();

        AseguradoraRequest request =
                aseguradoraRequest("  Rimac  ", "20100041953",
                        " contacto@rimac.pe ");

        when(aseguradoraRepository.existsByRuc("20100041953"))
                .thenReturn(false);

        when(aseguradoraRepository.save(any(Aseguradora.class)))
                .thenAnswer(inv -> {
                    Aseguradora a = inv.getArgument(0);
                    a.setIdAseguradora(10);
                    return a;
                });

        Aseguradora resultado =
                service.crearAseguradora(request, ADMIN);

        assertNotNull(resultado);
        assertEquals(10, resultado.getIdAseguradora());
        assertEquals("Rimac", resultado.getNombre());
        assertEquals("20100041953", resultado.getRuc());
        assertEquals("contacto@rimac.pe", resultado.getCorreo());
        assertEquals("Activo", resultado.getEstado());

        verify(aseguradoraRepository).save(any(Aseguradora.class));
        verify(historialService).registrar(
                1L, "Aseguradora", 10L, "CREAR", null, "Rimac");
    }

    @Test
    void crearAseguradora_nombreVacio_lanzaExcepcion() {
        AseguradoraRequest request =
                aseguradoraRequest("   ", null, null);

        assertThrows(RuntimeException.class,
                () -> service.crearAseguradora(request, ADMIN));

        verify(aseguradoraRepository, never())
                .save(any(Aseguradora.class));
    }

    @Test
    void crearAseguradora_nombreDemasiadoLargo_lanzaExcepcion() {
        AseguradoraRequest request =
                aseguradoraRequest("A".repeat(151), null, null);

        assertThrows(RuntimeException.class,
                () -> service.crearAseguradora(request, ADMIN));
    }

    @Test
    void crearAseguradora_rucInvalido_lanzaExcepcion() {
        AseguradoraRequest request =
                aseguradoraRequest("Rimac", "12345", null);

        assertThrows(RuntimeException.class,
                () -> service.crearAseguradora(request, ADMIN));
    }

    @Test
    void crearAseguradora_correoInvalido_lanzaExcepcion() {
        AseguradoraRequest request =
                aseguradoraRequest("Rimac", null, "correo-invalido");

        assertThrows(RuntimeException.class,
                () -> service.crearAseguradora(request, ADMIN));
    }

    @Test
    void crearAseguradora_rucDuplicado_lanzaExcepcion() {
        AseguradoraRequest request =
                aseguradoraRequest("Rimac", "20100041953", null);

        when(aseguradoraRepository.existsByRuc("20100041953"))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> service.crearAseguradora(request, ADMIN));

        verify(aseguradoraRepository, never())
                .save(any(Aseguradora.class));
    }

    @Test
    void actualizarAseguradora_datosValidos_actualizaCorrectamente() {
        configurarAdmin();

        Aseguradora existente = new Aseguradora();
        existente.setIdAseguradora(5);
        existente.setNombre("Nombre anterior");
        existente.setEstado("Activo");

        when(aseguradoraRepository.findById(5))
                .thenReturn(Optional.of(existente));

        when(aseguradoraRepository.existsByRucAndIdAseguradoraNot(
                "20100041953", 5)).thenReturn(false);

        when(aseguradoraRepository.save(any(Aseguradora.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Aseguradora resultado = service.actualizarAseguradora(
                5,
                aseguradoraRequest("Rimac", "20100041953",
                        "contacto@rimac.pe"),
                ADMIN);

        assertEquals("Rimac", resultado.getNombre());
        assertEquals("20100041953", resultado.getRuc());

        verify(historialService).registrar(
                1L, "Aseguradora", 5L,
                "ACTUALIZAR", "Nombre anterior", "Rimac");
    }

    @Test
    void actualizarAseguradora_noExiste_lanzaExcepcion() {
        when(aseguradoraRepository.findById(5))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.actualizarAseguradora(
                        5, aseguradoraRequest("Rimac", null, null), ADMIN));

        verify(aseguradoraRepository, never())
                .save(any(Aseguradora.class));
    }

    @Test
    void actualizarAseguradora_rucDuplicado_lanzaExcepcion() {
        Aseguradora existente = new Aseguradora();
        existente.setIdAseguradora(5);

        when(aseguradoraRepository.findById(5))
                .thenReturn(Optional.of(existente));

        when(aseguradoraRepository.existsByRucAndIdAseguradoraNot(
                "20100041953", 5)).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> service.actualizarAseguradora(
                        5,
                        aseguradoraRequest("Rimac", "20100041953", null),
                        ADMIN));

        verify(aseguradoraRepository, never())
                .save(any(Aseguradora.class));
    }

    @Test
    void cambiarEstadoAseguradora_estadoValido_actualizaEstado() {
        configurarAdmin();

        Aseguradora existente = new Aseguradora();
        existente.setIdAseguradora(5);
        existente.setEstado("Activo");

        when(aseguradoraRepository.findById(5))
                .thenReturn(Optional.of(existente));

        when(aseguradoraRepository.save(any(Aseguradora.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Aseguradora resultado =
                service.cambiarEstadoAseguradora(5, "Inactivo", ADMIN);

        assertEquals("Inactivo", resultado.getEstado());

        verify(historialService).registrar(
                1L, "Aseguradora", 5L,
                "CAMBIO_ESTADO", "Activo", "Inactivo");
    }

    @Test
    void cambiarEstadoAseguradora_estadoInvalido_lanzaExcepcion() {
        assertThrows(RuntimeException.class,
                () -> service.cambiarEstadoAseguradora(
                        5, "Suspendido", ADMIN));

        verify(aseguradoraRepository, never()).findById(anyInt());
    }

    // =====================================================
    // TIPOS DE SEGURO
    // =====================================================

    @Test
    void listarTiposSeguro_retornaLista() {
        when(tipoSeguroRepository.findAll())
                .thenReturn(List.of(new TipoSeguro()));

        List<TipoSeguro> resultado = service.listarTiposSeguro();

        assertEquals(1, resultado.size());
        verify(tipoSeguroRepository).findAll();
    }

    @Test
    void crearTipoSeguro_datosValidos_guardaCorrectamente() {
        configurarAdmin();

        when(tipoSeguroRepository.existsByNombreIgnoreCase("Vida"))
                .thenReturn(false);

        when(tipoSeguroRepository.save(any(TipoSeguro.class)))
                .thenAnswer(inv -> {
                    TipoSeguro t = inv.getArgument(0);
                    t.setIdTipoSeguro(20);
                    return t;
                });

        TipoSeguro resultado = service.crearTipoSeguro(
                tipoRequest(" Vida ", "Seguro de vida"),
                ADMIN);

        assertNotNull(resultado);
        assertEquals(20, resultado.getIdTipoSeguro());
        assertEquals("Vida", resultado.getNombre());
        assertEquals("Seguro de vida", resultado.getDescripcion());
        assertEquals("Activo", resultado.getEstado());

        verify(historialService).registrar(
                1L, "TipoSeguro", 20L, "CREAR", null, "Vida");
    }

    @Test
    void crearTipoSeguro_nombreVacio_lanzaExcepcion() {
        assertThrows(RuntimeException.class,
                () -> service.crearTipoSeguro(
                        tipoRequest("   ", "Descripción"), ADMIN));

        verify(tipoSeguroRepository, never())
                .save(any(TipoSeguro.class));
    }

    @Test
    void crearTipoSeguro_nombreDemasiadoLargo_lanzaExcepcion() {
        assertThrows(RuntimeException.class,
                () -> service.crearTipoSeguro(
                        tipoRequest("A".repeat(81), null), ADMIN));
    }

    @Test
    void crearTipoSeguro_descripcionDemasiadoLarga_lanzaExcepcion() {
        assertThrows(RuntimeException.class,
                () -> service.crearTipoSeguro(
                        tipoRequest("Vida", "A".repeat(201)), ADMIN));
    }

    @Test
    void crearTipoSeguro_nombreDuplicado_lanzaExcepcion() {
        when(tipoSeguroRepository.existsByNombreIgnoreCase("Vida"))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> service.crearTipoSeguro(
                        tipoRequest("Vida", null), ADMIN));

        verify(tipoSeguroRepository, never())
                .save(any(TipoSeguro.class));
    }

    @Test
    void actualizarTipoSeguro_datosValidos_actualizaCorrectamente() {
        configurarAdmin();

        TipoSeguro existente = new TipoSeguro();
        existente.setIdTipoSeguro(8);
        existente.setNombre("Anterior");

        when(tipoSeguroRepository.findById(8))
                .thenReturn(Optional.of(existente));

        when(tipoSeguroRepository
                .existsByNombreIgnoreCaseAndIdTipoSeguroNot("Vida", 8))
                .thenReturn(false);

        when(tipoSeguroRepository.save(any(TipoSeguro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TipoSeguro resultado = service.actualizarTipoSeguro(
                8, tipoRequest("Vida", "Nueva descripción"), ADMIN);

        assertEquals("Vida", resultado.getNombre());
        assertEquals("Nueva descripción", resultado.getDescripcion());

        verify(historialService).registrar(
                1L, "TipoSeguro", 8L,
                "ACTUALIZAR", "Anterior", "Vida");
    }

    @Test
    void actualizarTipoSeguro_noExiste_lanzaExcepcion() {
        when(tipoSeguroRepository.findById(8))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.actualizarTipoSeguro(
                        8, tipoRequest("Vida", null), ADMIN));

        verify(tipoSeguroRepository, never())
                .save(any(TipoSeguro.class));
    }

    @Test
    void actualizarTipoSeguro_nombreDuplicado_lanzaExcepcion() {
        TipoSeguro existente = new TipoSeguro();
        existente.setIdTipoSeguro(8);

        when(tipoSeguroRepository.findById(8))
                .thenReturn(Optional.of(existente));

        when(tipoSeguroRepository
                .existsByNombreIgnoreCaseAndIdTipoSeguroNot("Vida", 8))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> service.actualizarTipoSeguro(
                        8, tipoRequest("Vida", null), ADMIN));

        verify(tipoSeguroRepository, never())
                .save(any(TipoSeguro.class));
    }

    @Test
    void cambiarEstadoTipoSeguro_estadoValido_actualizaEstado() {
        configurarAdmin();

        TipoSeguro existente = new TipoSeguro();
        existente.setIdTipoSeguro(8);
        existente.setEstado("Activo");

        when(tipoSeguroRepository.findById(8))
                .thenReturn(Optional.of(existente));

        when(tipoSeguroRepository.save(any(TipoSeguro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TipoSeguro resultado =
                service.cambiarEstadoTipoSeguro(8, "Inactivo", ADMIN);

        assertEquals("Inactivo", resultado.getEstado());

        verify(historialService).registrar(
                1L, "TipoSeguro", 8L,
                "CAMBIO_ESTADO", "Activo", "Inactivo");
    }

    @Test
    void cambiarEstadoTipoSeguro_estadoInvalido_lanzaExcepcion() {
        assertThrows(RuntimeException.class,
                () -> service.cambiarEstadoTipoSeguro(
                        8, "Suspendido", ADMIN));

        verify(tipoSeguroRepository, never()).findById(anyInt());
    }
}

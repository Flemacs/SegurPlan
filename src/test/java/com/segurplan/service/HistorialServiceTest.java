
package com.segurplan.service;

import com.segurplan.dto.HistorialDTO;
import com.segurplan.model.Historial;
import com.segurplan.model.Usuario;
import com.segurplan.repository.HistorialRepository;
import com.segurplan.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistorialServiceTest {

    @Mock
    private HistorialRepository historialRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private HistorialService historialService;

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setNombres("Juan");
        usuario.setApellidos("Perez");
        usuario.setCorreo("juan@correo.com");
        return usuario;
    }

    private Historial crearHistorial(Usuario usuario) {
        Historial historial = new Historial();
        historial.setIdHistorial(10L);
        historial.setUsuario(usuario);
        historial.setEntidad("Usuario");
        historial.setIdRegistro(5L);
        historial.setAccion("ACTUALIZAR");
        historial.setValorAnterior("Activo");
        historial.setValorNuevo("Inactivo");
        historial.setFechaHora(
                LocalDateTime.of(2026, 11, 10, 10, 30)
        );
        return historial;
    }

    // =====================================================
    // REGISTRAR
    // =====================================================

    @Test
    void registrar_conUsuario_existenteGuardaHistorial() {

        Usuario usuario = crearUsuario();

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        historialService.registrar(
                1L,
                "Usuario",
                5L,
                "ACTUALIZAR",
                "Activo",
                "Inactivo"
        );

        ArgumentCaptor<Historial> captor =
                ArgumentCaptor.forClass(Historial.class);

        verify(historialRepository).save(captor.capture());

        Historial guardado = captor.getValue();

        assertEquals(usuario, guardado.getUsuario());
        assertEquals("Usuario", guardado.getEntidad());
        assertEquals(5L, guardado.getIdRegistro());
        assertEquals("ACTUALIZAR", guardado.getAccion());
        assertEquals("Activo", guardado.getValorAnterior());
        assertEquals("Inactivo", guardado.getValorNuevo());

        verify(usuarioRepository).findById(1L);
    }

    @Test
    void registrar_usuarioNulo_guardaHistorialSinUsuario() {

        historialService.registrar(
                null,
                "Poliza",
                20L,
                "CREAR",
                null,
                "Nueva póliza"
        );

        ArgumentCaptor<Historial> captor =
                ArgumentCaptor.forClass(Historial.class);

        verify(historialRepository).save(captor.capture());

        Historial guardado = captor.getValue();

        assertNull(guardado.getUsuario());
        assertEquals("Poliza", guardado.getEntidad());
        assertEquals(20L, guardado.getIdRegistro());
        assertEquals("CREAR", guardado.getAccion());
        assertNull(guardado.getValorAnterior());
        assertEquals("Nueva póliza", guardado.getValorNuevo());

        verify(usuarioRepository, never()).findById(anyLong());
    }

    @Test
    void registrar_usuarioNoEncontrado_guardaHistorialSinUsuario() {

        when(usuarioRepository.findById(99L))
                .thenReturn(Optional.empty());

        historialService.registrar(
                99L,
                "Cotizacion",
                30L,
                "ELIMINAR",
                "Disponible",
                null
        );

        ArgumentCaptor<Historial> captor =
                ArgumentCaptor.forClass(Historial.class);

        verify(historialRepository).save(captor.capture());

        Historial guardado = captor.getValue();

        assertNull(guardado.getUsuario());
        assertEquals("Cotizacion", guardado.getEntidad());
        assertEquals(30L, guardado.getIdRegistro());
        assertEquals("ELIMINAR", guardado.getAccion());

        verify(usuarioRepository).findById(99L);
    }

    // =====================================================
    // LISTAR TODOS
    // =====================================================

    @Test
    void listarTodos_conRegistrosConvierteCorrectamenteDTO() {

        Usuario usuario = crearUsuario();
        Historial historial = crearHistorial(usuario);

        when(historialRepository.findAllByOrderByFechaHoraDesc())
                .thenReturn(List.of(historial));

        List<HistorialDTO> resultado =
                historialService.listarTodos();

        assertEquals(1, resultado.size());

        HistorialDTO dto = resultado.get(0);

        assertEquals(10L, dto.getIdHistorial());
        assertEquals(1L, dto.getIdUsuario());
        assertEquals("Juan Perez", dto.getUsuario());
        assertEquals("juan@correo.com", dto.getCorreoUsuario());
        assertEquals("Usuario", dto.getEntidad());
        assertEquals(5L, dto.getIdRegistro());
        assertEquals("ACTUALIZAR", dto.getAccion());
        assertEquals("Activo", dto.getValorAnterior());
        assertEquals("Inactivo", dto.getValorNuevo());
        assertEquals(historial.getFechaHora(), dto.getFechaHora());

        verify(historialRepository).findAllByOrderByFechaHoraDesc();
    }

    @Test
    void listarTodos_sinUsuario_retornaUsuarioSistema() {

        Historial historial = crearHistorial(null);

        when(historialRepository.findAllByOrderByFechaHoraDesc())
                .thenReturn(List.of(historial));

        List<HistorialDTO> resultado =
                historialService.listarTodos();

        assertEquals(1, resultado.size());

        HistorialDTO dto = resultado.get(0);

        assertNull(dto.getIdUsuario());
        assertEquals("Sistema", dto.getUsuario());
        assertNull(dto.getCorreoUsuario());
    }

    // =====================================================
    // LISTAR POR ENTIDAD
    // =====================================================

    @Test
    void listarPorEntidad_filtraYConvierteRegistros() {

        Usuario usuario = crearUsuario();
        Historial historial = crearHistorial(usuario);

        when(historialRepository
                .findByEntidadOrderByFechaHoraDesc("Usuario"))
                .thenReturn(List.of(historial));

        List<HistorialDTO> resultado =
                historialService.listarPorEntidad("Usuario");

        assertEquals(1, resultado.size());
        assertEquals("Usuario", resultado.get(0).getEntidad());
        assertEquals("Juan Perez", resultado.get(0).getUsuario());

        verify(historialRepository)
                .findByEntidadOrderByFechaHoraDesc("Usuario");
    }

    // =====================================================
    // LISTAR POR USUARIO
    // =====================================================

    @Test
    void listarPorUsuario_filtraYConvierteRegistros() {

        Usuario usuario = crearUsuario();
        Historial historial = crearHistorial(usuario);

        when(historialRepository
                .findByUsuario_IdUsuarioOrderByFechaHoraDesc(1L))
                .thenReturn(List.of(historial));

        List<HistorialDTO> resultado =
                historialService.listarPorUsuario(1L);

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getIdUsuario());
        assertEquals("Juan Perez", resultado.get(0).getUsuario());

        verify(historialRepository)
                .findByUsuario_IdUsuarioOrderByFechaHoraDesc(1L);
    }

    // =====================================================
    // RESULTADOS VACÍOS
    // =====================================================

    @Test
    void listarTodos_sinRegistros_retornaListaVacia() {

        when(historialRepository.findAllByOrderByFechaHoraDesc())
                .thenReturn(List.of());

        List<HistorialDTO> resultado =
                historialService.listarTodos();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarPorEntidad_sinRegistros_retornaListaVacia() {

        when(historialRepository
                .findByEntidadOrderByFechaHoraDesc("Pago"))
                .thenReturn(List.of());

        List<HistorialDTO> resultado =
                historialService.listarPorEntidad("Pago");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarPorUsuario_sinRegistros_retornaListaVacia() {

        when(historialRepository
                .findByUsuario_IdUsuarioOrderByFechaHoraDesc(99L))
                .thenReturn(List.of());

        List<HistorialDTO> resultado =
                historialService.listarPorUsuario(99L);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }
}

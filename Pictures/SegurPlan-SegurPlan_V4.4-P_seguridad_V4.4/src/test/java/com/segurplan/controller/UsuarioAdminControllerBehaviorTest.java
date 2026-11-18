
package com.segurplan.controller;

import com.segurplan.dto.ActualizarUsuarioRequest;
import com.segurplan.dto.CambiarRolRequest;
import com.segurplan.dto.UsuarioAdminDTO;
import com.segurplan.service.UsuarioService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioAdminControllerBehaviorTest {

    private UsuarioService service;
    private UsuarioAdminController controller;

    @BeforeEach
    void setUp() {
        service = mock(UsuarioService.class);
        controller = new UsuarioAdminController(service);
    }

    private UsuarioAdminDTO crearUsuario() {
        return new UsuarioAdminDTO(
                1L,
                "Juan",
                "Perez",
                "juan@test.com",
                "999888777",
                "Activo",
                LocalDateTime.of(2026, 11, 10, 10, 0),
                "Cliente"
        );
    }

    @Test
    void listarUsuariosCorrectamente() {
        List<UsuarioAdminDTO> usuarios = List.of(crearUsuario());
        when(service.listarUsuarios()).thenReturn(usuarios);

        ResponseEntity<?> respuesta = controller.listarUsuarios();

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(usuarios, respuesta.getBody());
        assertEquals(1, ((List<?>) respuesta.getBody()).size());

        verify(service).listarUsuarios();
    }

    @Test
    void listarUsuariosCuandoNoHayRegistros() {
        when(service.listarUsuarios()).thenReturn(List.of());

        ResponseEntity<?> respuesta = controller.listarUsuarios();

        assertEquals(200, respuesta.getStatusCode().value());
        assertTrue(((List<?>) respuesta.getBody()).isEmpty());
    }

    @Test
    void listarUsuariosConError() {
        when(service.listarUsuarios())
                .thenThrow(new RuntimeException("Error al listar usuarios"));

        ResponseEntity<?> respuesta = controller.listarUsuarios();

        assertEquals(400, respuesta.getStatusCode().value());
        assertEquals("Error al listar usuarios",
                ((Map<?, ?>) respuesta.getBody()).get("mensaje"));
    }

    @Test
    void obtenerUsuarioCorrectamente() {
        UsuarioAdminDTO usuario = crearUsuario();
        when(service.obtenerUsuario(1L)).thenReturn(usuario);

        ResponseEntity<?> respuesta = controller.obtenerUsuario(1L);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(usuario, respuesta.getBody());
        verify(service).obtenerUsuario(1L);
    }

    @Test
    void obtenerUsuarioConError() {
        when(service.obtenerUsuario(99L))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        ResponseEntity<?> respuesta = controller.obtenerUsuario(99L);

        assertEquals(400, respuesta.getStatusCode().value());
        assertEquals("Usuario no encontrado",
                ((Map<?, ?>) respuesta.getBody()).get("mensaje"));
    }

    @Test
    void actualizarUsuarioCorrectamente() {
        ActualizarUsuarioRequest request = new ActualizarUsuarioRequest();
        request.setNombres("Carlos");
        request.setApellidos("Lopez");
        request.setTelefono("987654321");

        UsuarioAdminDTO usuario = crearUsuario();
        usuario.setNombres("Carlos");
        usuario.setApellidos("Lopez");
        usuario.setTelefono("987654321");

        when(service.actualizarUsuario(1L, request)).thenReturn(usuario);

        ResponseEntity<?> respuesta =
                controller.actualizarUsuario(1L, request);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals("Usuario actualizado correctamente", body.get("mensaje"));
        assertEquals(usuario, body.get("usuario"));

        verify(service).actualizarUsuario(1L, request);
    }

    @Test
    void actualizarUsuarioConError() {
        ActualizarUsuarioRequest request = new ActualizarUsuarioRequest();

        when(service.actualizarUsuario(1L, request))
                .thenThrow(new RuntimeException("Datos inválidos"));

        ResponseEntity<?> respuesta =
                controller.actualizarUsuario(1L, request);

        assertEquals(400, respuesta.getStatusCode().value());
        assertEquals("Datos inválidos",
                ((Map<?, ?>) respuesta.getBody()).get("mensaje"));
    }

    @Test
    void cambiarEstadoCorrectamente() {
        Map<String, String> request = Map.of("estado", "Inactivo");
        UsuarioAdminDTO usuario = crearUsuario();
        usuario.setEstado("Inactivo");

        when(service.cambiarEstado(1L, "Inactivo")).thenReturn(usuario);

        ResponseEntity<?> respuesta =
                controller.cambiarEstado(1L, request);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
        assertEquals(
                "Estado del usuario actualizado correctamente",
                body.get("mensaje")
        );
        assertEquals(usuario, body.get("usuario"));

        verify(service).cambiarEstado(1L, "Inactivo");
    }

    @Test
    void cambiarEstadoConError() {
        Map<String, String> request = Map.of("estado", "Desconocido");

        when(service.cambiarEstado(1L, "Desconocido"))
                .thenThrow(new RuntimeException("Estado inválido"));

        ResponseEntity<?> respuesta =
                controller.cambiarEstado(1L, request);

        assertEquals(400, respuesta.getStatusCode().value());
        assertEquals("Estado inválido",
                ((Map<?, ?>) respuesta.getBody()).get("mensaje"));
    }

    @Test
    void cambiarRolCorrectamente() {
        CambiarRolRequest request = new CambiarRolRequest();
        request.setIdRol(2);

        UsuarioAdminDTO usuario = crearUsuario();
        usuario.setRol("Asesor");

        when(service.cambiarRol(1L, 2)).thenReturn(usuario);

        ResponseEntity<UsuarioAdminDTO> respuesta =
                controller.cambiarRol(1L, request);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(usuario, respuesta.getBody());
        assertEquals("Asesor", respuesta.getBody().getRol());

        verify(service).cambiarRol(1L, 2);
    }

    @Test
    void cambiarRolPropagaErrorDelServicio() {
        CambiarRolRequest request = new CambiarRolRequest();
        request.setIdRol(99);

        when(service.cambiarRol(1L, 99))
                .thenThrow(new RuntimeException("Rol no encontrado"));

        RuntimeException excepcion = assertThrows(
                RuntimeException.class,
                () -> controller.cambiarRol(1L, request)
        );

        assertEquals("Rol no encontrado", excepcion.getMessage());
    }
}

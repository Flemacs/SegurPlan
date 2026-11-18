
package com.segurplan.controller;

import com.segurplan.dto.ActualizarUsuarioRequest;
import com.segurplan.dto.RegistroAsesorRequest;
import com.segurplan.dto.UsuarioAdminDTO;
import com.segurplan.service.UsuarioService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AsesorAdminControllerTest {

    private UsuarioService usuarioService;
    private AsesorAdminController controller;
    private UsuarioAdminDTO asesor;

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        controller = new AsesorAdminController(usuarioService);
        asesor = mock(UsuarioAdminDTO.class);
    }

    @Test
    void listarAsesoresExitosamente() {
        when(usuarioService.listarAsesores()).thenReturn(List.of(asesor));

        ResponseEntity<?> response = controller.listarAsesores();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of(asesor), response.getBody());
        verify(usuarioService).listarAsesores();
    }

    @Test
    void listarAsesoresConError() {
        when(usuarioService.listarAsesores())
                .thenThrow(new RuntimeException("Error al listar"));

        ResponseEntity<?> response = controller.listarAsesores();

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Error al listar",
                ((Map<?, ?>) response.getBody()).get("mensaje"));
    }

    @Test
    void obtenerAsesorExitosamente() {
        when(usuarioService.obtenerAsesor(1L)).thenReturn(asesor);

        ResponseEntity<?> response = controller.obtenerAsesor(1L);

        assertEquals(200, response.getStatusCode().value());
        assertSame(asesor, response.getBody());
        verify(usuarioService).obtenerAsesor(1L);
    }

    @Test
    void obtenerAsesorConError() {
        when(usuarioService.obtenerAsesor(99L))
                .thenThrow(new RuntimeException("Asesor no encontrado"));

        ResponseEntity<?> response = controller.obtenerAsesor(99L);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Asesor no encontrado",
                ((Map<?, ?>) response.getBody()).get("mensaje"));
    }

    @Test
    void registrarAsesorExitosamente() {
        RegistroAsesorRequest request = mock(RegistroAsesorRequest.class);

        when(usuarioService.registrarAsesor(request)).thenReturn(asesor);

        ResponseEntity<?> response = controller.registrarAsesor(request);

        assertEquals(201, response.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertEquals("Asesor registrado correctamente", body.get("mensaje"));
        assertSame(asesor, body.get("asesor"));

        verify(usuarioService).registrarAsesor(request);
    }

    @Test
    void registrarAsesorConError() {
        RegistroAsesorRequest request = mock(RegistroAsesorRequest.class);

        when(usuarioService.registrarAsesor(request))
                .thenThrow(new RuntimeException("Datos inválidos"));

        ResponseEntity<?> response = controller.registrarAsesor(request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Datos inválidos",
                ((Map<?, ?>) response.getBody()).get("mensaje"));
    }

    @Test
    void actualizarAsesorExitosamente() {
        ActualizarUsuarioRequest request =
                mock(ActualizarUsuarioRequest.class);

        when(usuarioService.actualizarAsesor(1L, request))
                .thenReturn(asesor);

        ResponseEntity<?> response =
                controller.actualizarAsesor(1L, request);

        assertEquals(200, response.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertEquals("Asesor actualizado correctamente", body.get("mensaje"));
        assertSame(asesor, body.get("asesor"));

        verify(usuarioService).actualizarAsesor(1L, request);
    }

    @Test
    void actualizarAsesorConError() {
        ActualizarUsuarioRequest request =
                mock(ActualizarUsuarioRequest.class);

        when(usuarioService.actualizarAsesor(1L, request))
                .thenThrow(new RuntimeException("No se pudo actualizar"));

        ResponseEntity<?> response =
                controller.actualizarAsesor(1L, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("No se pudo actualizar",
                ((Map<?, ?>) response.getBody()).get("mensaje"));
    }

    @Test
    void cambiarEstadoAsesorExitosamente() {
        Map<String, String> request = Map.of("estado", "Inactivo");

        when(usuarioService.cambiarEstadoAsesor(1L, "Inactivo"))
                .thenReturn(asesor);

        ResponseEntity<?> response =
                controller.cambiarEstadoAsesor(1L, request);

        assertEquals(200, response.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertEquals(
                "Estado del asesor actualizado correctamente",
                body.get("mensaje")
        );
        assertSame(asesor, body.get("asesor"));

        verify(usuarioService).cambiarEstadoAsesor(1L, "Inactivo");
    }

    @Test
    void cambiarEstadoAsesorConError() {
        Map<String, String> request = Map.of("estado", "Desconocido");

        when(usuarioService.cambiarEstadoAsesor(1L, "Desconocido"))
                .thenThrow(new RuntimeException("Estado inválido"));

        ResponseEntity<?> response =
                controller.cambiarEstadoAsesor(1L, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Estado inválido",
                ((Map<?, ?>) response.getBody()).get("mensaje"));
    }
}

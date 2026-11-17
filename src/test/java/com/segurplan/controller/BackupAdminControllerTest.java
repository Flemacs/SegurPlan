
package com.segurplan.controller;

import com.segurplan.dto.BackupDTO;
import com.segurplan.model.Usuario;
import com.segurplan.repository.UsuarioRepository;
import com.segurplan.service.BackupService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BackupAdminControllerTest {

    private BackupService backupService;
    private UsuarioRepository usuarioRepository;
    private BackupAdminController controller;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        backupService = mock(BackupService.class);
        usuarioRepository = mock(UsuarioRepository.class);
        controller = new BackupAdminController(backupService, usuarioRepository);

        auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@test.com");

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);

        when(usuarioRepository.findByCorreo("admin@test.com"))
                .thenReturn(Optional.of(usuario));
    }

    @Test
    void listarExitosamente() {
        BackupDTO backup = mock(BackupDTO.class);
        when(backupService.listar()).thenReturn(List.of(backup));

        ResponseEntity<?> response = controller.listar();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of(backup), response.getBody());
        verify(backupService).listar();
    }

    @Test
    void listarConError() {
        when(backupService.listar())
                .thenThrow(new RuntimeException("Error al listar respaldos"));

        ResponseEntity<?> response = controller.listar();

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void crearExitosamente() {
        BackupDTO backup = mock(BackupDTO.class);
        when(backupService.crear(1L)).thenReturn(backup);

        ResponseEntity<?> response = controller.crear(auth);

        assertEquals(200, response.getStatusCode().value());
        assertSame(backup, response.getBody());
        verify(backupService).crear(1L);
    }

    @Test
    void crearConError() {
        when(backupService.crear(1L))
                .thenThrow(new RuntimeException("No se pudo crear el respaldo"));

        ResponseEntity<?> response = controller.crear(auth);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void descargarExitosamente() {
        String nombre = "segurplan_20261110_120000.sql";
        Path archivo = Path.of("backups", nombre);

        when(backupService.obtenerArchivo(nombre)).thenReturn(archivo);

        ResponseEntity<?> response = controller.descargar(nombre);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Resource);
        assertEquals(
                "attachment; filename=\"" + nombre + "\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
        assertEquals(
                MediaType.APPLICATION_OCTET_STREAM,
                response.getHeaders().getContentType()
        );
    }

    @Test
    void descargarConError() {
        when(backupService.obtenerArchivo("invalido.sql"))
                .thenThrow(new RuntimeException("Nombre de respaldo inválido."));

        ResponseEntity<?> response = controller.descargar("invalido.sql");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void restaurarExitosamente() {
        String nombre = "segurplan_20261110_120000.sql";

        doNothing().when(backupService).restaurar(nombre, 1L);

        ResponseEntity<?> response = controller.restaurar(nombre, auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "Base de datos restaurada desde " + nombre,
                ((java.util.Map<?, ?>) response.getBody()).get("mensaje")
        );
        verify(backupService).restaurar(nombre, 1L);
    }

    @Test
    void restaurarConError() {
        doThrow(new RuntimeException("Falló la restauración"))
                .when(backupService).restaurar("respaldo.sql", 1L);

        ResponseEntity<?> response = controller.restaurar("respaldo.sql", auth);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void eliminarExitosamente() {
        String nombre = "segurplan_20261110_120000.sql";

        doNothing().when(backupService).eliminar(nombre, 1L);

        ResponseEntity<?> response = controller.eliminar(nombre, auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "Respaldo eliminado.",
                ((java.util.Map<?, ?>) response.getBody()).get("mensaje")
        );
        verify(backupService).eliminar(nombre, 1L);
    }

    @Test
    void eliminarConError() {
        doThrow(new RuntimeException("No se pudo eliminar"))
                .when(backupService).eliminar("respaldo.sql", 1L);

        ResponseEntity<?> response = controller.eliminar("respaldo.sql", auth);

        assertEquals(400, response.getStatusCode().value());
    }
}

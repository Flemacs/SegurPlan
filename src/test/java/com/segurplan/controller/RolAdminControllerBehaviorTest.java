
package com.segurplan.controller;

import com.segurplan.dto.RolAdminDTO;
import com.segurplan.service.RolAdminService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RolAdminControllerBehaviorTest {

    private RolAdminService service;
    private RolAdminController controller;

    private RolAdminDTO crearRol() {
        return new RolAdminDTO(
                1,
                "Administrador",
                "Rol administrativo",
                5L,
                List.of("Gestionar usuarios", "Consultar reportes")
        );
    }

    @BeforeEach
    void setUp() {
        service = mock(RolAdminService.class);
        controller = new RolAdminController(service);
    }

    @Test
    void listarRolesCorrectamente() {
        List<RolAdminDTO> roles = List.of(crearRol());

        when(service.listarRoles()).thenReturn(roles);

        ResponseEntity<List<RolAdminDTO>> respuesta =
                controller.listarRoles();

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(1, respuesta.getBody().size());
        assertEquals("Administrador",
                respuesta.getBody().get(0).getNombre());

        verify(service).listarRoles();
    }

    @Test
    void listarRolesCuandoNoExistenRoles() {
        when(service.listarRoles()).thenReturn(List.of());

        ResponseEntity<List<RolAdminDTO>> respuesta =
                controller.listarRoles();

        assertEquals(200, respuesta.getStatusCode().value());
        assertNotNull(respuesta.getBody());
        assertTrue(respuesta.getBody().isEmpty());
    }

    @Test
    void obtenerRolCorrectamente() {
        RolAdminDTO rol = crearRol();

        when(service.obtenerRol(1)).thenReturn(rol);

        ResponseEntity<RolAdminDTO> respuesta =
                controller.obtenerRol(1);

        assertEquals(200, respuesta.getStatusCode().value());
        assertNotNull(respuesta.getBody());
        assertEquals(1, respuesta.getBody().getIdRol());
        assertEquals("Administrador", respuesta.getBody().getNombre());
        assertEquals(5L, respuesta.getBody().getCantidadUsuarios());
        assertEquals(2, respuesta.getBody().getPermisos().size());

        verify(service).obtenerRol(1);
    }

    @Test
    void obtenerRolCuandoNoExiste() {
        when(service.obtenerRol(99))
                .thenThrow(new RuntimeException("Rol no encontrado."));

        RuntimeException excepcion = assertThrows(
                RuntimeException.class,
                () -> controller.obtenerRol(99)
        );

        assertEquals("Rol no encontrado.", excepcion.getMessage());
    }
}

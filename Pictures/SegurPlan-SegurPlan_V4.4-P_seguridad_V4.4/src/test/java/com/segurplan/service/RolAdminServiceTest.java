
package com.segurplan.service;

import com.segurplan.dto.RolAdminDTO;
import com.segurplan.model.Rol;
import com.segurplan.repository.RolRepository;
import com.segurplan.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolAdminServiceTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private RolAdminService rolAdminService;

    @Test
    void listarRoles_retornaRolesConCantidadYPermisos() {
        Rol cliente = new Rol(1, "cliente", "Rol de cliente");
        Rol asesor = new Rol(2, "asesor", "Rol de asesor");

        when(rolRepository.findAll()).thenReturn(List.of(cliente, asesor));
        when(usuarioRepository.countByRol_IdRol(1)).thenReturn(5L);
        when(usuarioRepository.countByRol_IdRol(2)).thenReturn(3L);

        List<RolAdminDTO> resultado = rolAdminService.listarRoles();

        assertEquals(2, resultado.size());

        assertEquals(1, resultado.get(0).getIdRol());
        assertEquals("cliente", resultado.get(0).getNombre());
        assertEquals("Rol de cliente", resultado.get(0).getDescripcion());
        assertEquals(5L, resultado.get(0).getCantidadUsuarios());
        assertFalse(resultado.get(0).getPermisos().isEmpty());

        assertEquals(2, resultado.get(1).getIdRol());
        assertEquals("asesor", resultado.get(1).getNombre());
        assertEquals(3L, resultado.get(1).getCantidadUsuarios());
        assertFalse(resultado.get(1).getPermisos().isEmpty());

        verify(rolRepository).findAll();
        verify(usuarioRepository).countByRol_IdRol(1);
        verify(usuarioRepository).countByRol_IdRol(2);
    }

    @Test
    void listarRoles_sinRoles_retornaListaVacia() {
        when(rolRepository.findAll()).thenReturn(List.of());

        List<RolAdminDTO> resultado = rolAdminService.listarRoles();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        verify(usuarioRepository, never()).countByRol_IdRol(any());
    }

    @Test
    void obtenerRol_rolExistente_retornaDatosYPermisos() {
        Rol administrador = new Rol(
                3,
                "administrador",
                "Rol administrativo"
        );

        when(rolRepository.findById(3))
                .thenReturn(Optional.of(administrador));

        when(usuarioRepository.countByRol_IdRol(3))
                .thenReturn(2L);

        RolAdminDTO resultado = rolAdminService.obtenerRol(3);

        assertNotNull(resultado);
        assertEquals(3, resultado.getIdRol());
        assertEquals("administrador", resultado.getNombre());
        assertEquals("Rol administrativo", resultado.getDescripcion());
        assertEquals(2L, resultado.getCantidadUsuarios());

        assertTrue(resultado.getPermisos().contains(
                "Gestionar usuarios"
        ));

        assertTrue(resultado.getPermisos().contains(
                "Consultar reportes"
        ));

        verify(rolRepository).findById(3);
        verify(usuarioRepository).countByRol_IdRol(3);
    }

    @Test
    void obtenerRol_rolInexistente_lanzaExcepcion() {
        when(rolRepository.findById(99))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> rolAdminService.obtenerRol(99)
        );

        assertEquals("Rol no encontrado.", exception.getMessage());

        verify(usuarioRepository, never()).countByRol_IdRol(any());
    }

    @Test
    void obtenerRol_nombreConMayusculasYEspacios_reconocePermisos() {
        Rol cliente = new Rol(
                1,
                "  CLIENTE  ",
                "Cliente"
        );

        when(rolRepository.findById(1))
                .thenReturn(Optional.of(cliente));

        when(usuarioRepository.countByRol_IdRol(1))
                .thenReturn(10L);

        RolAdminDTO resultado = rolAdminService.obtenerRol(1);

        assertEquals(10L, resultado.getCantidadUsuarios());
        assertTrue(resultado.getPermisos().contains(
                "Solicitar cotizaciones"
        ));
        assertTrue(resultado.getPermisos().contains(
                "Consultar sus pólizas"
        ));
    }

    @Test
    void obtenerRol_nombreDesconocido_retornaListaDePermisosVacia() {
        Rol desconocido = new Rol(
                4,
                "supervisor",
                "Rol personalizado"
        );

        when(rolRepository.findById(4))
                .thenReturn(Optional.of(desconocido));

        when(usuarioRepository.countByRol_IdRol(4))
                .thenReturn(0L);

        RolAdminDTO resultado = rolAdminService.obtenerRol(4);

        assertNotNull(resultado.getPermisos());
        assertTrue(resultado.getPermisos().isEmpty());
        assertEquals(0L, resultado.getCantidadUsuarios());
    }
}

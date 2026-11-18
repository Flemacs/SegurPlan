
package com.segurplan.service;

import com.segurplan.model.Rol;
import com.segurplan.model.Usuario;
import com.segurplan.repository.UsuarioRepository;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SegurplanUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private SegurplanUserDetailsService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new SegurplanUserDetailsService(usuarioRepository);

        usuario = new Usuario();
        usuario.setCorreo("cliente@segurplan.com");
        usuario.setPassword("$2a$10$hash");
        usuario.setEstado("Activo");
        usuario.setRol(new Rol(1, "CLIENTE", "Rol cliente"));
    }

    @Test
    void loadUserByUsername_usuarioExistente_retornaUserDetails() {
        when(usuarioRepository.findByCorreo("cliente@segurplan.com"))
                .thenReturn(Optional.of(usuario));

        UserDetails resultado =
                service.loadUserByUsername("cliente@segurplan.com");

        assertNotNull(resultado);
        assertEquals("cliente@segurplan.com", resultado.getUsername());
        assertEquals("$2a$10$hash", resultado.getPassword());
        assertTrue(resultado.isEnabled());

        verify(usuarioRepository).findByCorreo("cliente@segurplan.com");
    }

    @Test
    void loadUserByUsername_asignaAutoridadSegunRol() {
        when(usuarioRepository.findByCorreo("cliente@segurplan.com"))
                .thenReturn(Optional.of(usuario));

        UserDetails resultado =
                service.loadUserByUsername("cliente@segurplan.com");

        assertTrue(resultado.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_CLIENTE")
        ));
    }

    @Test
    void loadUserByUsername_estadoActivoMayusculas_esHabilitado() {
        usuario.setEstado("ACTIVO");

        when(usuarioRepository.findByCorreo("cliente@segurplan.com"))
                .thenReturn(Optional.of(usuario));

        UserDetails resultado =
                service.loadUserByUsername("cliente@segurplan.com");

        assertTrue(resultado.isEnabled());
    }

    @Test
    void loadUserByUsername_estadoInactivo_deshabilitaUsuario() {
        usuario.setEstado("Inactivo");

        when(usuarioRepository.findByCorreo("cliente@segurplan.com"))
                .thenReturn(Optional.of(usuario));

        UserDetails resultado =
                service.loadUserByUsername("cliente@segurplan.com");

        assertFalse(resultado.isEnabled());
    }

    @Test
    void loadUserByUsername_correoInexistente_lanzaExcepcion() {
        when(usuarioRepository.findByCorreo("noexiste@segurplan.com"))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("noexiste@segurplan.com")
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
    }
}

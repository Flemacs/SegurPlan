
package com.segurplan.service;

import com.segurplan.model.Usuario;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SegurplanUserDetailsService
        implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    

    public SegurplanUserDetailsService(
            UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado"
                        ));

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getPassword())
                .roles(usuario.getRol().getNombre())
                .disabled(
                        !"Activo".equalsIgnoreCase(
                                usuario.getEstado()
                        )
                )
                .build();
    }
}
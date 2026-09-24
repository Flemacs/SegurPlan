
package com.segurplan.service;

import com.segurplan.dto.LoginRequest;
import com.segurplan.dto.LoginResponse;
import com.segurplan.dto.RegistroRequest;

import com.segurplan.model.Rol;
import com.segurplan.model.Usuario;

import com.segurplan.repository.RolRepository;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // REGISTRO
    public Usuario registrar(RegistroRequest request) {

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new IllegalArgumentException(
                    "El correo ya está registrado"
            );
        }

        Rol rolCliente = rolRepository.findByNombre("Cliente")
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe el rol Cliente"
                        )
                );

        Usuario usuario = new Usuario();

        usuario.setNombres(request.getNombres());
        usuario.setApellidos(request.getApellidos());
        usuario.setCorreo(request.getCorreo());
        usuario.setTelefono(request.getTelefono());

        usuario.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        usuario.setRol(rolCliente);

        return usuarioRepository.save(usuario);
    }

    // VALIDACIÓN DE CREDENCIALES
    public LoginResponse login(LoginRequest request) {

        Usuario usuario = usuarioRepository
                .findByCorreo(request.getCorreo())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Correo o contraseña incorrectos"
                        )
                );

        if (!"Activo".equalsIgnoreCase(usuario.getEstado())) {
            throw new IllegalArgumentException(
                    "El usuario no se encuentra activo"
            );
        }

        boolean passwordCorrecto = passwordEncoder.matches(
                request.getPassword(),
                usuario.getPassword()
        );

        if (!passwordCorrecto) {
            throw new IllegalArgumentException(
                    "Correo o contraseña incorrectos"
            );
        }

        return new LoginResponse(
                true,
                "Inicio de sesión correcto",
                usuario.getIdUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getRol().getNombre()
        );
    }
}
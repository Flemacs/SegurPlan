package com.segurplan.controller;

import com.segurplan.dto.ActualizarUsuarioRequest;
import com.segurplan.dto.LoginRequest;
import com.segurplan.dto.LoginResponse;
import com.segurplan.dto.RegistroRequest;
import com.segurplan.dto.UsuarioAdminDTO;

import com.segurplan.model.Usuario;

import com.segurplan.service.SegurplanUserDetailsService;
import com.segurplan.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication
        .UsernamePasswordAuthenticationToken;

import org.springframework.security.core.context
        .SecurityContext;

import org.springframework.security.core.context
        .SecurityContextHolder;

import org.springframework.security.web.context
        .HttpSessionSecurityContextRepository;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    private final SegurplanUserDetailsService
            usuarioDetailsService;

    public UsuarioController(
            UsuarioService usuarioService,
            SegurplanUserDetailsService usuarioDetailsService) {

        this.usuarioService = usuarioService;
        this.usuarioDetailsService =
                usuarioDetailsService;
    }

    // =====================================================
    // REGISTRO
    // =====================================================

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(
            @RequestBody RegistroRequest request) {

        try {

            Usuario usuario =
                    usuarioService.registrar(request);

            Map<String, Object> respuesta =
                    new HashMap<>();

            respuesta.put(
                    "mensaje",
                    "Usuario registrado correctamente"
            );

            respuesta.put(
                    "idUsuario",
                    usuario.getIdUsuario()
            );

            respuesta.put(
                    "nombres",
                    usuario.getNombres()
            );

            respuesta.put(
                    "apellidos",
                    usuario.getApellidos()
            );

            respuesta.put(
                    "correo",
                    usuario.getCorreo()
            );

            respuesta.put(
                    "rol",
                    usuario.getRol().getNombre()
            );

            return ResponseEntity.ok(respuesta);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "mensaje",
                                    e.getMessage()
                            )
                    );
        }
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        try {

            LoginResponse respuesta =
                    usuarioService.login(request);

            var usuarioAutenticado =
                    usuarioDetailsService
                            .loadUserByUsername(
                                    request.getCorreo()
                            );

            var authentication =
                    UsernamePasswordAuthenticationToken
                            .authenticated(
                                    usuarioAutenticado,
                                    null,
                                    usuarioAutenticado
                                            .getAuthorities()
                            );

            SecurityContext context =
                    SecurityContextHolder
                            .createEmptyContext();

            context.setAuthentication(
                    authentication
            );

            var session =
                    httpRequest.getSession(false);

            if (session == null) {

                session =
                        httpRequest.getSession(true);

            } else {

                httpRequest.changeSessionId();
            }

            session.setAttribute(
                    HttpSessionSecurityContextRepository
                            .SPRING_SECURITY_CONTEXT_KEY,
                    context
            );

            SecurityContextHolder.setContext(
                    context
            );

            return ResponseEntity.ok(respuesta);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "mensaje",
                                    e.getMessage()
                            )
                    );
        }
    }

    // =====================================================
    // ADMIN - LISTAR USUARIOS
    // =====================================================

    @GetMapping("/admin")
    public ResponseEntity<?> listarUsuarios() {

        try {

            List<UsuarioAdminDTO> usuarios =
                    usuarioService.listarUsuarios();

            return ResponseEntity.ok(usuarios);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "mensaje",
                                    e.getMessage()
                            )
                    );
        }
    }

    // =====================================================
    // ADMIN - OBTENER USUARIO
    // =====================================================

    @GetMapping("/admin/{idUsuario}")
    public ResponseEntity<?> obtenerUsuario(
            @PathVariable Long idUsuario) {

        try {

            return ResponseEntity.ok(
                    usuarioService.obtenerUsuario(
                            idUsuario
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "mensaje",
                                    e.getMessage()
                            )
                    );
        }
    }

    // =====================================================
    // ADMIN - ACTUALIZAR USUARIO
    // =====================================================

    @PutMapping("/admin/{idUsuario}")
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Long idUsuario,
            @RequestBody
            ActualizarUsuarioRequest request) {

        try {

            UsuarioAdminDTO usuario =
                    usuarioService.actualizarUsuario(
                            idUsuario,
                            request
                    );

            Map<String, Object> respuesta =
                    new HashMap<>();

            respuesta.put(
                    "mensaje",
                    "Usuario actualizado correctamente"
            );

            respuesta.put(
                    "usuario",
                    usuario
            );

            return ResponseEntity.ok(respuesta);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "mensaje",
                                    e.getMessage()
                            )
                    );
        }
    }

    // =====================================================
    // ADMIN - CAMBIAR ESTADO
    // =====================================================

    @PutMapping("/admin/{idUsuario}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long idUsuario,
            @RequestBody
            Map<String, String> request) {

        try {

            String estado =
                    request.get("estado");

            UsuarioAdminDTO usuario =
                    usuarioService.cambiarEstado(
                            idUsuario,
                            estado
                    );

            Map<String, Object> respuesta =
                    new HashMap<>();

            respuesta.put(
                    "mensaje",
                    "Estado actualizado correctamente"
            );

            respuesta.put(
                    "usuario",
                    usuario
            );

            return ResponseEntity.ok(respuesta);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "mensaje",
                                    e.getMessage()
                            )
                    );
        }
    }
}
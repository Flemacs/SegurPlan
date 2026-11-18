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

@io.swagger.v3.oas.annotations.tags.Tag(name = "Autenticación", description = "Registro, inicio de sesión y rutas heredadas de administración de usuarios.")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    private final SegurplanUserDetailsService
            usuarioDetailsService;
    private final com.segurplan.service.NotificacionService notificaciones;
    private final com.segurplan.service.LoginAttemptService intentos;

    public UsuarioController(
            UsuarioService usuarioService,
            SegurplanUserDetailsService usuarioDetailsService,
            com.segurplan.service.NotificacionService notificaciones,
            com.segurplan.service.LoginAttemptService intentos) {
        this.usuarioService = usuarioService;
        this.usuarioDetailsService =
                usuarioDetailsService;
        this.notificaciones = notificaciones;
        this.intentos = intentos;
    }

    // =====================================================
    // REGISTRO
    // =====================================================

    @io.swagger.v3.oas.annotations.Operation(summary = "Registrar un cliente nuevo", description = "**Roles:** Público (sin sesión). Crea una cuenta con rol Cliente. Contraseña: mínimo 8 caracteres con letras y números.")
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(
            @RequestBody RegistroRequest request) {

        try {

            Usuario usuario =
                    usuarioService.registrar(request);
            notificaciones.notificar(usuario.getIdUsuario(), "BIENVENIDA",
                    "¡Bienvenido a SEGURPLAN!",
                    "Su cuenta fue creada. Ya puede cotizar seguros, simular su AFP y registrar siniestros.",
                    "USUARIO", usuario.getIdUsuario());

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

    @io.swagger.v3.oas.annotations.Operation(summary = "Iniciar sesión", description = "**Roles:** Público (sin sesión). Crea la sesión (cookie JSESSIONID). Tras 5 intentos fallidos del mismo correo (o 20 de la misma IP) responde 429 durante 15 minutos.")
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        // Bloqueo temporal por intentos fallidos (fuerza bruta)
        String claveUsuario = "u:" + (request.getCorreo() == null
                ? ""
                : request.getCorreo().trim().toLowerCase());
        String claveIp = "ip:" + httpRequest.getRemoteAddr();

        if (intentos.estaBloqueado(claveUsuario)
                || intentos.estaBloqueado(claveIp)) {

            long minutos = Math.max(
                    intentos.minutosRestantes(claveUsuario),
                    intentos.minutosRestantes(claveIp));

            return ResponseEntity
                    .status(429)
                    .header("Retry-After", String.valueOf(minutos * 60))
                    .body(Map.of("mensaje",
                            "Demasiados intentos fallidos. "
                            + "Intente nuevamente en " + minutos
                            + " minuto(s)."));
        }

        try {

            LoginResponse respuesta =
                    usuarioService.login(request);
            intentos.registrarExito(claveUsuario);

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

            intentos.registrarFallo(claveUsuario);
            intentos.registrarFallo(claveIp);

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

    @io.swagger.v3.oas.annotations.Operation(summary = "Listar usuarios (ruta heredada)", description = "**Roles:** Administrador. Duplicado de GET /api/admin/usuarios; se conserva por compatibilidad.")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Ver un usuario (ruta heredada)", description = "**Roles:** Administrador. Duplicado de GET /api/admin/usuarios/{idUsuario}.")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Actualizar un usuario (ruta heredada)", description = "**Roles:** Administrador. Duplicado de PUT /api/admin/usuarios/{idUsuario}.")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Activar o desactivar un usuario (ruta heredada)", description = "**Roles:** Administrador. Duplicado de PUT /api/admin/usuarios/{idUsuario}/estado.")
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
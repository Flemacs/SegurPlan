package com.segurplan.controller;

import com.segurplan.dto.ActualizarUsuarioRequest;
import com.segurplan.dto.UsuarioAdminDTO;
import com.segurplan.service.UsuarioService;

import java.util.HashMap;
import java.util.Map;
import com.segurplan.dto.CambiarRolRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioAdminController {

    private final UsuarioService usuarioService;

    public UsuarioAdminController(
            UsuarioService usuarioService) {

        this.usuarioService = usuarioService;
    }

    // =====================================================
    // LISTAR TODOS LOS USUARIOS
    // =====================================================
    @GetMapping
    public ResponseEntity<?> listarUsuarios() {

        try {

            return ResponseEntity.ok(
                    usuarioService.listarUsuarios()
            );

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }

    // =====================================================
    // OBTENER USUARIO POR ID
    // =====================================================
    @GetMapping("/{idUsuario}")
    public ResponseEntity<?> obtenerUsuario(
            @PathVariable Long idUsuario) {

        try {

            return ResponseEntity.ok(
                    usuarioService.obtenerUsuario(
                            idUsuario
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }

    // =====================================================
    // ACTUALIZAR DATOS DEL USUARIO
    // =====================================================
    @PutMapping("/{idUsuario}")
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Long idUsuario,
            @RequestBody ActualizarUsuarioRequest request) {

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

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }

    // =====================================================
    // CAMBIAR ESTADO DEL USUARIO
    // Activo <-> Inactivo
    // =====================================================
    @PutMapping("/{idUsuario}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long idUsuario,
            @RequestBody Map<String, String> request) {

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
                    "Estado del usuario actualizado correctamente"
            );

            respuesta.put(
                    "usuario",
                    usuario
            );

            return ResponseEntity.ok(respuesta);

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }
    @PutMapping("/{idUsuario}/rol")
public ResponseEntity<UsuarioAdminDTO>
        cambiarRol(
                @PathVariable Long idUsuario,
                @RequestBody CambiarRolRequest request) {

    UsuarioAdminDTO usuario =
            usuarioService.cambiarRol(
                    idUsuario,
                    request.getIdRol()
            );

    return ResponseEntity.ok(
            usuario
    );
}
}
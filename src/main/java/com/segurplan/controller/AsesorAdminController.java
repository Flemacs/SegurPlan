package com.segurplan.controller;

import com.segurplan.dto.ActualizarUsuarioRequest;
import com.segurplan.dto.RegistroAsesorRequest;
import com.segurplan.dto.UsuarioAdminDTO;
import com.segurplan.service.UsuarioService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/asesores")
public class AsesorAdminController {

    private final UsuarioService usuarioService;

    public AsesorAdminController(
            UsuarioService usuarioService) {

        this.usuarioService = usuarioService;
    }

    // =====================================================
    // LISTAR ASESORES
    // =====================================================

    @GetMapping
    public ResponseEntity<?> listarAsesores() {

        try {

            return ResponseEntity.ok(
                    usuarioService.listarAsesores()
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
    // OBTENER ASESOR POR ID
    // =====================================================

    @GetMapping("/{idUsuario}")
    public ResponseEntity<?> obtenerAsesor(
            @PathVariable Long idUsuario) {

        try {

            return ResponseEntity.ok(
                    usuarioService.obtenerAsesor(
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
    // REGISTRAR ASESOR
    // =====================================================

    @PostMapping
    public ResponseEntity<?> registrarAsesor(
            @RequestBody RegistroAsesorRequest request) {

        try {

            UsuarioAdminDTO asesor =
                    usuarioService.registrarAsesor(
                            request
                    );

            Map<String, Object> respuesta =
                    new HashMap<>();

            respuesta.put(
                    "mensaje",
                    "Asesor registrado correctamente"
            );

            respuesta.put(
                    "asesor",
                    asesor
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(respuesta);

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }

    // =====================================================
    // ACTUALIZAR ASESOR
    // =====================================================

    @PutMapping("/{idUsuario}")
    public ResponseEntity<?> actualizarAsesor(
            @PathVariable Long idUsuario,
            @RequestBody ActualizarUsuarioRequest request) {

        try {

            UsuarioAdminDTO asesor =
                    usuarioService.actualizarAsesor(
                            idUsuario,
                            request
                    );

            Map<String, Object> respuesta =
                    new HashMap<>();

            respuesta.put(
                    "mensaje",
                    "Asesor actualizado correctamente"
            );

            respuesta.put(
                    "asesor",
                    asesor
            );

            return ResponseEntity.ok(
                    respuesta
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
    // CAMBIAR ESTADO DEL ASESOR
    // Activo <-> Inactivo
    // =====================================================

    @PutMapping("/{idUsuario}/estado")
    public ResponseEntity<?> cambiarEstadoAsesor(
            @PathVariable Long idUsuario,
            @RequestBody Map<String, String> request) {

        try {

            String estado =
                    request.get("estado");

            UsuarioAdminDTO asesor =
                    usuarioService
                            .cambiarEstadoAsesor(
                                    idUsuario,
                                    estado
                            );

            Map<String, Object> respuesta =
                    new HashMap<>();

            respuesta.put(
                    "mensaje",
                    "Estado del asesor actualizado correctamente"
            );

            respuesta.put(
                    "asesor",
                    asesor
            );

            return ResponseEntity.ok(
                    respuesta
            );

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }
}
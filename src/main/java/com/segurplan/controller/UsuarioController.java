package com.segurplan.controller;

import com.segurplan.dto.LoginRequest;
import com.segurplan.dto.LoginResponse;
import com.segurplan.dto.RegistroRequest;
import com.segurplan.model.Usuario;
import com.segurplan.service.UsuarioService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // =========================
    // REGISTRO
    // =========================
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(
            @RequestBody RegistroRequest request) {

        try {

            Usuario usuario = usuarioService.registrar(request);

            Map<String, Object> respuesta = new HashMap<>();

            respuesta.put("mensaje", "Usuario registrado correctamente");
            respuesta.put("idUsuario", usuario.getIdUsuario());
            respuesta.put("nombres", usuario.getNombres());
            respuesta.put("apellidos", usuario.getApellidos());
            respuesta.put("correo", usuario.getCorreo());
            respuesta.put("rol", usuario.getRol().getNombre());

            return ResponseEntity.ok(respuesta);

        } catch (RuntimeException e) {

            Map<String, String> error = new HashMap<>();
            error.put("mensaje", e.getMessage());

            return ResponseEntity
                    .badRequest()
                    .body(error);
        }
    }

    // =========================
    // LOGIN
    // =========================
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        try {

            LoginResponse respuesta =
                    usuarioService.login(request);

            return ResponseEntity.ok(respuesta);

        } catch (RuntimeException e) {

            Map<String, String> error = new HashMap<>();
            error.put("mensaje", e.getMessage());

            return ResponseEntity
                    .badRequest()
                    .body(error);
        }
    }
}
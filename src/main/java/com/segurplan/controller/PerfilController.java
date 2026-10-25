package com.segurplan.controller;

import com.segurplan.service.PerfilService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Perfil del usuario autenticado (RF003). */
@RestController
@RequestMapping("/api/usuarios/perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public ResponseEntity<?> obtener(Principal principal) {
        return ResponseEntity.ok(perfilService.obtener(principal.getName()));
    }

    @PutMapping
    public ResponseEntity<?> actualizar(@RequestBody Map<String, String> body,
                                        Principal principal,
                                        HttpServletRequest request) {
        Map<String, Object> r = new LinkedHashMap<>(perfilService.actualizar(
                principal.getName(),
                body.get("nombres"), body.get("apellidos"), body.get("correo"),
                body.get("telefono"), body.get("direccion")));
        boolean cambioCorreo = Boolean.TRUE.equals(r.get("correoCambiado"));
        r.put("mensaje", cambioCorreo
                ? "Perfil actualizado. Como cambió su correo debe iniciar sesión nuevamente."
                : "Perfil actualizado correctamente.");
        if (cambioCorreo) {
            // la sesión sigue ligada al correo anterior: se cierra para evitar inconsistencias
            HttpSession sesion = request.getSession(false);
            if (sesion != null) {
                sesion.invalidate();
            }
            SecurityContextHolder.clearContext();
        }
        return ResponseEntity.ok(r);
    }

    @PutMapping("/password")
    public ResponseEntity<?> cambiarPassword(@RequestBody Map<String, String> body,
                                             Principal principal) {
        String nueva = body.get("nueva");
        String confirmar = body.get("confirmar");
        if (confirmar != null && !confirmar.equals(nueva)) {
            throw new IllegalArgumentException("La confirmación no coincide con la nueva contraseña.");
        }
        perfilService.cambiarPassword(principal.getName(), body.get("actual"), nueva);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente."));
    }
}

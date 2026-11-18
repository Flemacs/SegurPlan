package com.segurplan.controller;

import com.segurplan.service.AccesoService;
import com.segurplan.service.HistorialService;
import com.segurplan.service.PermisoAdminService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/** Permisos por rol (RF030) y actividad de un usuario (RF029). Solo Administrador. */
@RestController
@RequestMapping("/api/admin")
public class PermisoAdminController {

    private final PermisoAdminService permisos;
    private final HistorialService historialService;
    private final AccesoService acceso;

    public PermisoAdminController(PermisoAdminService permisos, HistorialService historialService,
                                  AccesoService acceso) {
        this.permisos = permisos;
        this.historialService = historialService;
        this.acceso = acceso;
    }

    @GetMapping("/roles/{idRol}/permisos")
    public ResponseEntity<?> obtener(@PathVariable Integer idRol) {
        return ResponseEntity.ok(Map.of("modulos", PermisoAdminService.MODULOS,
                "permisos", permisos.obtener(idRol)));
    }

    @PutMapping("/roles/{idRol}/permisos")
    public ResponseEntity<?> guardar(@PathVariable Integer idRol,
                                     @RequestBody List<Map<String, Object>> body,
                                     org.springframework.security.core.Authentication auth) {
        permisos.guardar(acceso.idUsuario(auth), idRol, body);
        return ResponseEntity.ok(Map.of("mensaje", "Permisos actualizados correctamente."));
    }

    /** Historial de actividades de un usuario o asesor. */
    @GetMapping("/usuarios/{idUsuario}/actividad")
    public ResponseEntity<?> actividad(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(historialService.listarPorUsuario(idUsuario));
    }
}

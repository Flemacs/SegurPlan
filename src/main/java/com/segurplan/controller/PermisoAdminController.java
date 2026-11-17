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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin · Roles y permisos", description = "Roles, matriz de permisos y actividad.")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Ver la matriz de permisos de un rol", description = "**Roles:** Administrador. La matriz se guarda y administra; la autorización real es por rol (SecurityConfig).")
    @GetMapping("/roles/{idRol}/permisos")
    public ResponseEntity<?> obtener(@PathVariable Integer idRol) {
        return ResponseEntity.ok(Map.of("modulos", PermisoAdminService.MODULOS,
                "permisos", permisos.obtener(idRol)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Guardar la matriz de permisos de un rol", description = "**Roles:** Administrador. Actualiza la matriz; no cambia la autorización real, que es por rol.")
    @PutMapping("/roles/{idRol}/permisos")
    public ResponseEntity<?> guardar(@PathVariable Integer idRol,
                                     @RequestBody List<Map<String, Object>> body,
                                     org.springframework.security.core.Authentication auth) {
        permisos.guardar(acceso.idUsuario(auth), idRol, body);
        return ResponseEntity.ok(Map.of("mensaje", "Permisos actualizados correctamente."));
    }

    /** Historial de actividades de un usuario o asesor. */
    @io.swagger.v3.oas.annotations.Operation(summary = "Actividad de un usuario", description = "**Roles:** Administrador. Acciones registradas en el historial.")
    @GetMapping("/usuarios/{idUsuario}/actividad")
    public ResponseEntity<?> actividad(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(historialService.listarPorUsuario(idUsuario));
    }
}

package com.segurplan.controller;

import com.segurplan.dto.RolAdminDTO;
import com.segurplan.service.RolAdminService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin · Roles y permisos", description = "Roles, matriz de permisos y actividad.")
@RestController
@RequestMapping("/api/admin/roles")
public class RolAdminController {

    private final RolAdminService rolAdminService;

    public RolAdminController(
            RolAdminService rolAdminService) {

        this.rolAdminService =
                rolAdminService;
    }

    // =====================================================
    // LISTAR ROLES
    // =====================================================

    @io.swagger.v3.oas.annotations.Operation(summary = "Listar roles", description = "**Roles:** Administrador. Roles del sistema.")
    @GetMapping
    public ResponseEntity<List<RolAdminDTO>>
            listarRoles() {

        return ResponseEntity.ok(
                rolAdminService.listarRoles()
        );
    }

    // =====================================================
    // OBTENER ROL
    // =====================================================

    @io.swagger.v3.oas.annotations.Operation(summary = "Ver un rol", description = "**Roles:** Administrador. Detalle del rol.")
    @GetMapping("/{idRol}")
    public ResponseEntity<RolAdminDTO>
            obtenerRol(
                    @PathVariable Integer idRol) {

        return ResponseEntity.ok(
                rolAdminService.obtenerRol(
                        idRol
                )
        );
    }
}
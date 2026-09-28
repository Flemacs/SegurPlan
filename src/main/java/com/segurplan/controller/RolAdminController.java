package com.segurplan.controller;

import com.segurplan.dto.RolAdminDTO;
import com.segurplan.service.RolAdminService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
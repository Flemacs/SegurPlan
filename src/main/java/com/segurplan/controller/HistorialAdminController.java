package com.segurplan.controller;

import com.segurplan.dto.HistorialDTO;
import com.segurplan.service.HistorialService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin · Historial", description = "Bitácora de cambios (inmutable).")
@RestController
@RequestMapping("/api/admin/historial")
public class HistorialAdminController {

    private final HistorialService historialService;

    public HistorialAdminController(
            HistorialService historialService) {

        this.historialService =
                historialService;
    }


    // =====================================================
    // TODO EL HISTORIAL
    // =====================================================

    @io.swagger.v3.oas.annotations.Operation(summary = "Listar el historial de cambios", description = "**Roles:** Administrador. Bitácora inmutable (un trigger impide UPDATE y DELETE).")
    @GetMapping
    public ResponseEntity<List<HistorialDTO>>
            listarHistorial() {

        return ResponseEntity.ok(
                historialService.listarTodos()
        );
    }


    // =====================================================
    // FILTRAR POR ENTIDAD
    // =====================================================

    @io.swagger.v3.oas.annotations.Operation(summary = "Historial por entidad", description = "**Roles:** Administrador. Filtra por tipo de registro.")
    @GetMapping("/entidad/{entidad}")
    public ResponseEntity<List<HistorialDTO>>
            listarPorEntidad(
                    @PathVariable String entidad) {

        return ResponseEntity.ok(
                historialService.listarPorEntidad(
                        entidad
                )
        );
    }
}
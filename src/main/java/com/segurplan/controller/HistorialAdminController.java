package com.segurplan.controller;

import com.segurplan.dto.HistorialDTO;
import com.segurplan.service.HistorialService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
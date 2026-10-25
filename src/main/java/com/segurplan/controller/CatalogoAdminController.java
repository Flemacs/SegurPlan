package com.segurplan.controller;

import com.segurplan.dto.AseguradoraRequest;
import com.segurplan.dto.CambiarEstadoRequest;
import com.segurplan.dto.TipoSeguroRequest;
import com.segurplan.service.CatalogoAdminService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mantenimiento de catálogos (solo Administrador, ver SecurityConfig:
 * /api/admin/** exige el rol Administrador).
 */
@RestController
@RequestMapping("/api/admin/catalogos")
public class CatalogoAdminController {

    private final CatalogoAdminService catalogoAdminService;

    public CatalogoAdminController(CatalogoAdminService catalogoAdminService) {
        this.catalogoAdminService = catalogoAdminService;
    }

    // ---------------- ASEGURADORAS ----------------

    @GetMapping("/aseguradoras")
    public ResponseEntity<?> listarAseguradoras() {
        return ResponseEntity.ok(catalogoAdminService.listarAseguradoras());
    }

    @PostMapping("/aseguradoras")
    public ResponseEntity<?> crearAseguradora(
            @RequestBody AseguradoraRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(
                    catalogoAdminService.crearAseguradora(request, auth.getName()));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PutMapping("/aseguradoras/{id}")
    public ResponseEntity<?> actualizarAseguradora(
            @PathVariable Integer id,
            @RequestBody AseguradoraRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(
                    catalogoAdminService.actualizarAseguradora(id, request, auth.getName()));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PutMapping("/aseguradoras/{id}/estado")
    public ResponseEntity<?> estadoAseguradora(
            @PathVariable Integer id,
            @RequestBody CambiarEstadoRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(
                    catalogoAdminService.cambiarEstadoAseguradora(
                            id, request.getEstado(), auth.getName()));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // ---------------- TIPOS DE SEGURO ----------------

    @GetMapping("/tipos-seguro")
    public ResponseEntity<?> listarTiposSeguro() {
        return ResponseEntity.ok(catalogoAdminService.listarTiposSeguro());
    }

    @PostMapping("/tipos-seguro")
    public ResponseEntity<?> crearTipoSeguro(
            @RequestBody TipoSeguroRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(
                    catalogoAdminService.crearTipoSeguro(request, auth.getName()));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PutMapping("/tipos-seguro/{id}")
    public ResponseEntity<?> actualizarTipoSeguro(
            @PathVariable Integer id,
            @RequestBody TipoSeguroRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(
                    catalogoAdminService.actualizarTipoSeguro(id, request, auth.getName()));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PutMapping("/tipos-seguro/{id}/estado")
    public ResponseEntity<?> estadoTipoSeguro(
            @PathVariable Integer id,
            @RequestBody CambiarEstadoRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(
                    catalogoAdminService.cambiarEstadoTipoSeguro(
                            id, request.getEstado(), auth.getName()));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    private ResponseEntity<?> error(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("mensaje", e.getMessage()));
    }
}

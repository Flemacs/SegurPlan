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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin · Catálogos", description = "Mantenimiento de aseguradoras y tipos de seguro.")
@RestController
@RequestMapping("/api/admin/catalogos")
public class CatalogoAdminController {

    private final CatalogoAdminService catalogoAdminService;

    public CatalogoAdminController(CatalogoAdminService catalogoAdminService) {
        this.catalogoAdminService = catalogoAdminService;
    }

    // ---------------- ASEGURADORAS ----------------

    @io.swagger.v3.oas.annotations.Operation(summary = "Listar aseguradoras (todas)", description = "**Roles:** Administrador. Incluye las inactivas.")
    @GetMapping("/aseguradoras")
    public ResponseEntity<?> listarAseguradoras() {
        return ResponseEntity.ok(catalogoAdminService.listarAseguradoras());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Crear una aseguradora", description = "**Roles:** Administrador. Alta en el catálogo.")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Actualizar una aseguradora", description = "**Roles:** Administrador. Datos de la aseguradora.")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Activar o desactivar una aseguradora", description = "**Roles:** Administrador. Una aseguradora inactiva deja de ofrecerse en los formularios.")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Listar tipos de seguro (todos)", description = "**Roles:** Administrador. Incluye los inactivos.")
    @GetMapping("/tipos-seguro")
    public ResponseEntity<?> listarTiposSeguro() {
        return ResponseEntity.ok(catalogoAdminService.listarTiposSeguro());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Crear un tipo de seguro", description = "**Roles:** Administrador. Alta en el catálogo.")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Actualizar un tipo de seguro", description = "**Roles:** Administrador. Datos del tipo.")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Activar o desactivar un tipo de seguro", description = "**Roles:** Administrador. Un tipo inactivo deja de ofrecerse.")
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

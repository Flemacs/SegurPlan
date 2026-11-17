package com.segurplan.controller;

import com.segurplan.repository.UsuarioRepository;
import com.segurplan.service.BackupService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Respaldo y restauración de la base de datos.
 * Solo Administrador (SecurityConfig: /api/admin/** → hasRole Administrador).
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin · Respaldos", description = "Respaldo, descarga y restauración de la base de datos.")
@RestController
@RequestMapping("/api/admin/backups")
public class BackupAdminController {

    private final BackupService backupService;
    private final UsuarioRepository usuarioRepository;

    public BackupAdminController(
            BackupService backupService,
            UsuarioRepository usuarioRepository) {

        this.backupService = backupService;
        this.usuarioRepository = usuarioRepository;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Listar respaldos", description = "**Roles:** Administrador. Archivos .sql disponibles.")
    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            return ResponseEntity.ok(backupService.listar());
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Crear un respaldo ahora", description = "**Roles:** Administrador. Ejecuta pg_dump. También hay respaldo automático cada 6 horas.")
    @PostMapping
    public ResponseEntity<?> crear(Authentication auth) {
        try {
            return ResponseEntity.ok(backupService.crear(idActor(auth)));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Descargar un respaldo", description = "**Roles:** Administrador. Entrega el archivo .sql.")
    @GetMapping("/{nombre}/descargar")
    public ResponseEntity<?> descargar(@PathVariable String nombre) {
        try {
            Resource recurso = new FileSystemResource(
                    backupService.obtenerArchivo(nombre));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + nombre + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(recurso);
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Restaurar un respaldo", description = "**Roles:** Administrador. Operación destructiva: reemplaza los datos actuales.")
    @PostMapping("/{nombre}/restaurar")
    public ResponseEntity<?> restaurar(
            @PathVariable String nombre, Authentication auth) {
        try {
            backupService.restaurar(nombre, idActor(auth));
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Base de datos restaurada desde " + nombre));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Eliminar un respaldo", description = "**Roles:** Administrador. Borra el archivo del servidor.")
    @DeleteMapping("/{nombre}")
    public ResponseEntity<?> eliminar(
            @PathVariable String nombre, Authentication auth) {
        try {
            backupService.eliminar(nombre, idActor(auth));
            return ResponseEntity.ok(Map.of("mensaje", "Respaldo eliminado."));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    private Long idActor(Authentication auth) {
        return usuarioRepository.findByCorreo(auth.getName())
                .map(u -> u.getIdUsuario())
                .orElse(null);
    }

    private ResponseEntity<?> error(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("mensaje", e.getMessage()));
    }
}

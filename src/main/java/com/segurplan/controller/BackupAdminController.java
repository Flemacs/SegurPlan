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

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            return ResponseEntity.ok(backupService.listar());
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(Authentication auth) {
        try {
            return ResponseEntity.ok(backupService.crear(idActor(auth)));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

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

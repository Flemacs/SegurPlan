package com.segurplan.controller;

import com.segurplan.model.Documento;
import com.segurplan.service.AccesoService;
import com.segurplan.service.DocumentoGestionService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;

/** Validación documental (RF011), adjuntos de cotización y descarga de archivos. */
@RestController
@RequestMapping("/api/documentos")
public class DocumentoGestionController {

    private final DocumentoGestionService servicio;
    private final AccesoService acceso;

    public DocumentoGestionController(DocumentoGestionService servicio, AccesoService acceso) {
        this.servicio = servicio;
        this.acceso = acceso;
    }

    /** Solo Asesor/Administrador (SecurityConfig). */
    @PutMapping("/{id}/validacion")
    public ResponseEntity<?> validar(@PathVariable Long id, @RequestBody Map<String, String> body,
                                     Principal principal) {
        Documento d = servicio.validar(principal.getName(), id, body.get("estado"), body.get("observacion"));
        return ResponseEntity.ok(Map.of("mensaje", "Documento " + d.getEstado().toLowerCase() + ".",
                "estado", d.getEstado()));
    }

    /** Descarga o visualización: el dueño o el personal. */
    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> archivo(@PathVariable Long id, Authentication auth) throws Exception {
        Documento d = servicio.obtener(id);
        if (!acceso.puedeVer(auth, d.getUsuario().getIdUsuario())) {
            throw new SecurityException("No tiene permiso para ver este documento.");
        }
        Path ruta = Paths.get(d.getRutaArchivo()).toAbsolutePath().normalize();
        Path base = Paths.get("uploads").toAbsolutePath().normalize();
        if (!ruta.startsWith(base) || !Files.exists(ruta)) {
            throw new IllegalArgumentException("El archivo ya no está disponible.");
        }
        String tipo = Files.probeContentType(ruta);
        MediaType mt = tipo == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(tipo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\""
                        + d.getNombreArchivo().replace("\"", "") + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(mt)
                .body(new FileSystemResource(ruta));
    }

    // ---- adjuntos de la cotización ----
    @PostMapping("/cotizacion/{idCotizacion}")
    public ResponseEntity<?> adjuntar(@PathVariable Long idCotizacion,
                                      @RequestParam(value = "tipoDocumento", required = false) String tipo,
                                      @RequestParam("archivo") MultipartFile archivo,
                                      Principal principal) {
        Documento d = servicio.adjuntarCotizacion(principal.getName(), idCotizacion, tipo, archivo);
        return ResponseEntity.ok(Map.of("mensaje", "Archivo adjuntado.", "idDocumento", d.getIdDocumento()));
    }

    @GetMapping("/cotizacion/{idCotizacion}")
    public ResponseEntity<?> listarAdjuntos(@PathVariable Long idCotizacion, Authentication auth) {
        List<Map<String, Object>> r = new ArrayList<>();
        for (Documento d : servicio.listarCotizacion(idCotizacion)) {
            if (!acceso.puedeVer(auth, d.getUsuario().getIdUsuario())) {
                throw new SecurityException("No tiene permiso para ver estos adjuntos.");
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idDocumento", d.getIdDocumento());
            m.put("tipoDocumento", d.getTipoDocumento());
            m.put("nombreArchivo", d.getNombreArchivo());
            m.put("estado", d.getEstado());
            m.put("observacion", d.getObservacion());
            m.put("fechaCarga", d.getFechaCarga());
            r.add(m);
        }
        return ResponseEntity.ok(r);
    }
}

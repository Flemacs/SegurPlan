package com.segurplan.controller;

import com.segurplan.model.Documento;
import com.segurplan.service.DocumentoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Documentos", description = "Evidencias de siniestros y adjuntos de cotización.")
@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentoController.class);

    private final DocumentoService documentoService;
    private final com.segurplan.service.SiniestroGestionService gestionService;

    public DocumentoController(
            DocumentoService documentoService,
            com.segurplan.service.SiniestroGestionService gestionService) {
        this.documentoService = documentoService;
        this.gestionService = gestionService;
    }

    // =====================================================
    // SUBIR EVIDENCIA
    // =====================================================

    @io.swagger.v3.oas.annotations.Operation(summary = "Subir una evidencia de siniestro", description = "**Roles:** Cualquier usuario autenticado. multipart/form-data. PDF, JPG o PNG hasta 10 MB; video hasta 100 MB. Solo el dueño.")
    @PostMapping(
            value = "/siniestro/{idSiniestro}",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<?> subirEvidencia(
            @PathVariable Long idSiniestro,
            @RequestParam("tipoDocumento") String tipoDocumento,
            @RequestParam("archivo") MultipartFile archivo,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "mensaje",
                            "Debe iniciar sesión."
                    ));
        }

        try {

            Documento documento
                    = documentoService.guardarEvidencia(
                            principal.getName(),
                            idSiniestro,
                            tipoDocumento,
                            archivo
                    );
            if (documento.getUsuario() != null) {
                gestionService.alCargarEvidencia(
                        idSiniestro, documento.getUsuario().getIdUsuario());
            }

            Map<String, Object> respuesta
                    = convertirDocumento(documento);

            respuesta.put(
                    "mensaje",
                    "Evidencia cargada correctamente."
            );

            return ResponseEntity.ok(respuesta);

        } catch (SecurityException e) {

            return ResponseEntity.status(403)
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));

        } catch (Exception e) {

            log.error("Error al guardar la evidencia", e);

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "mensaje",
                            "No se pudo guardar la evidencia."
                    ));
        }
    }

    // =====================================================
    // LISTAR EVIDENCIAS
    // =====================================================

    @io.swagger.v3.oas.annotations.Operation(summary = "Listar evidencias de mi siniestro", description = "**Roles:** Cualquier usuario autenticado. Solo el dueño del siniestro.")
    @GetMapping("/siniestro/{idSiniestro}")
    public ResponseEntity<?> listarEvidencias(
            @PathVariable Long idSiniestro,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "mensaje",
                            "Debe iniciar sesión."
                    ));
        }

        try {

            List<Map<String, Object>> documentos
                    = documentoService
                            .listarPorSiniestro(
                                    principal.getName(),
                                    idSiniestro
                            )
                            .stream()
                            .map(this::convertirDocumento)
                            .toList();

            return ResponseEntity.ok(documentos);

        } catch (SecurityException e) {

            return ResponseEntity.status(403)
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));

        } catch (Exception e) {

            log.error("Error al consultar las evidencias del siniestro", e);

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "mensaje",
                            "No se pudieron consultar las evidencias."
                    ));
        }
    }

    // =====================================================
    // ELIMINAR EVIDENCIA
    // =====================================================

    @io.swagger.v3.oas.annotations.Operation(summary = "Eliminar una evidencia", description = "**Roles:** Cualquier usuario autenticado. Solo el dueño del documento; el servicio rechaza (403) si no le pertenece.")
    @DeleteMapping("/{idDocumento}")
    public ResponseEntity<?> eliminarEvidencia(
            @PathVariable Long idDocumento,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "mensaje",
                            "Debe iniciar sesión."
                    ));
        }

        try {

            documentoService.eliminar(
                    principal.getName(),
                    idDocumento
            );

            return ResponseEntity.ok(
                    Map.of(
                            "mensaje",
                            "Evidencia eliminada correctamente."
                    )
            );

        } catch (SecurityException e) {

            return ResponseEntity.status(403)
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));

        } catch (Exception e) {

            log.error("Error al eliminar la evidencia", e);

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "mensaje",
                            "No se pudo eliminar la evidencia."
                    ));
        }
    }

    // =====================================================
    // CONVERTIR ENTIDAD A JSON
    // =====================================================

    private Map<String, Object> convertirDocumento(
            Documento documento) {

        Map<String, Object> dto
                = new LinkedHashMap<>();

        dto.put(
                "idDocumento",
                documento.getIdDocumento()
        );

        dto.put(
                "tipoDocumento",
                documento.getTipoDocumento()
        );

        dto.put(
                "nombreArchivo",
                documento.getNombreArchivo()
        );

        dto.put(
                "version",
                documento.getVersion()
        );

        dto.put(
                "fechaCarga",
                documento.getFechaCarga()
        );

        dto.put(
                "estado",
                documento.getEstado()
        );

        if (documento.getSiniestro() != null) {
            dto.put(
                    "idSiniestro",
                    documento.getSiniestro()
                            .getIdSiniestro()
            );
        }

        dto.put("observacion", documento.getObservacion());
        return dto;
    }
    // =====================================================
// EVIDENCIAS PARA REVISIÓN DEL ASESOR
// =====================================================

@io.swagger.v3.oas.annotations.Operation(summary = "Listar evidencias (vista del asesor)", description = "**Roles:** Asesor, Administrador. Todas las evidencias del siniestro.")
@GetMapping("/asesor/siniestro/{idSiniestro}")
public ResponseEntity<?> listarParaRevision(
        @PathVariable Long idSiniestro,
        Principal principal) {

    if (principal == null) {

        return ResponseEntity
                .status(401)
                .body(Map.of(
                        "mensaje",
                        "Debe iniciar sesión."
                ));
    }

    try {

        List<Map<String, Object>> documentos =
                documentoService
                        .listarParaRevision(
                                idSiniestro
                        )
                        .stream()
                        .map(this::convertirDocumento)
                        .toList();

        return ResponseEntity.ok(
                documentos
        );

    } catch (IllegalArgumentException e) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "mensaje",
                        e.getMessage()
                ));

    } catch (Exception e) {

        log.error("Error al consultar las evidencias para revisión", e);

        return ResponseEntity
                .internalServerError()
                .body(Map.of(
                        "mensaje",
                        "No se pudieron consultar las evidencias."
                ));
    }
}
}
package com.segurplan.controller;

import com.segurplan.model.Documento;
import com.segurplan.service.DocumentoService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(
            DocumentoService documentoService) {

        this.documentoService = documentoService;
    }

    // =====================================================
    // SUBIR EVIDENCIA
    // =====================================================

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

            e.printStackTrace();

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

            e.printStackTrace();

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

            e.printStackTrace();

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

        return dto;
    }
}
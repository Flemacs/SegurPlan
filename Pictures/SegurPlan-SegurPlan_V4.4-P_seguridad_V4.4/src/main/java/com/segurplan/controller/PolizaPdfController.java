package com.segurplan.controller;

import com.segurplan.model.Poliza;
import com.segurplan.repository.PolizaRepository;
import com.segurplan.service.AccesoService;
import com.segurplan.service.PdfService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Documento digital de la póliza en PDF (RF008). */
@RestController
@RequestMapping("/api/polizas")
public class PolizaPdfController {

    private final PolizaRepository polizaRepository;
    private final PdfService pdfService;
    private final AccesoService acceso;

    public PolizaPdfController(PolizaRepository polizaRepository, PdfService pdfService,
                               AccesoService acceso) {
        this.polizaRepository = polizaRepository;
        this.pdfService = pdfService;
        this.acceso = acceso;
    }

    @GetMapping("/{id}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, Authentication auth) {
        Poliza p = polizaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La póliza indicada no existe."));
        if (!acceso.puedeVer(auth, p.getContratacion().getUsuario().getIdUsuario())) {
            throw new SecurityException("No tiene permiso para ver esta póliza.");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"poliza-" + p.getNumeroPoliza() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfService.poliza(p));
    }
}

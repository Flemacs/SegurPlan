package com.segurplan.controller;

import com.segurplan.service.JasperReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes/jasper")
public class JasperReportController {

    private final JasperReportService jasperReportService;

    public JasperReportController(JasperReportService jasperReportService) {
        this.jasperReportService = jasperReportService;
    }

    @GetMapping(value = "/{codigo}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable String codigo) {
        byte[] pdf;
        String fileName;
        try {
            pdf = jasperReportService.generarPdf(codigo);
            fileName = codigo.toUpperCase() + ".pdf";
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .body(pdf);
    }
}

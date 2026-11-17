package com.segurplan.controller;

import com.segurplan.service.JasperReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Reportes PDF (JasperReports)", description = "Reportes en PDF generados en el servidor con plantillas .jrxml.")
@RestController
@RequestMapping("/api/reportes/jasper")
public class JasperReportController {

    private final JasperReportService jasperReportService;

    public JasperReportController(JasperReportService jasperReportService) {
        this.jasperReportService = jasperReportService;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Descargar un reporte PDF (JasperReports)", description = "**Roles:** Administrador. Códigos: REP-DG-01, REP-DG-02, REP-LA-01, REP-MV-01, REP-MV-02, REP-MV-05, REP-OT-02, REP-PO-01, REP-PO-02, REP-PO-03. Código desconocido: 404.")
    @GetMapping(value = "/{codigo}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable String codigo) {
        byte[] pdf;
        String fileName;
        try {
            pdf = jasperReportService.generarPdf(codigo);
            fileName = codigo.toUpperCase() + ".pdf";
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(JasperReportController.class)
                    .error("Error generando reporte {}", codigo, e);
            return ResponseEntity.status(500).build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .body(pdf);
    }
}

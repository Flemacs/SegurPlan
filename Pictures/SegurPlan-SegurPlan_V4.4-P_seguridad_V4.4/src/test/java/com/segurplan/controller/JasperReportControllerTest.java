
package com.segurplan.controller;

import com.segurplan.service.JasperReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JasperReportControllerTest {

    private JasperReportService service;
    private JasperReportController controller;

    @BeforeEach
    void setUp() {
        service = mock(JasperReportService.class);
        controller = new JasperReportController(service);
    }

    @Test
    void descargarPdfCorrectamente() {
        byte[] contenido = {1, 2, 3, 4};

        when(service.generarPdf("reporte"))
                .thenReturn(contenido);

        ResponseEntity<byte[]> respuesta =
                controller.descargarPdf("reporte");

        assertEquals(200, respuesta.getStatusCode().value());
        assertArrayEquals(contenido, respuesta.getBody());
        assertEquals(MediaType.APPLICATION_PDF,
                respuesta.getHeaders().getContentType());
        assertEquals(
                "attachment; filename=\"REPORTE.pdf\"",
                respuesta.getHeaders().getFirst(
                        HttpHeaders.CONTENT_DISPOSITION)
        );

        verify(service).generarPdf("reporte");
    }

    @Test
    void descargarPdfCuandoNoExiste() {
        when(service.generarPdf("inexistente"))
                .thenThrow(new IllegalArgumentException("No existe"));

        ResponseEntity<byte[]> respuesta =
                controller.descargarPdf("inexistente");

        assertEquals(404, respuesta.getStatusCode().value());
        assertNull(respuesta.getBody());
    }

    @Test
    void descargarPdfCuandoOcurreErrorInterno() {
        when(service.generarPdf("reporte"))
                .thenThrow(new RuntimeException("Error interno"));

        ResponseEntity<byte[]> respuesta =
                controller.descargarPdf("reporte");

        assertEquals(500, respuesta.getStatusCode().value());
        assertNull(respuesta.getBody());
    }
}


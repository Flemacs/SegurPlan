
package com.segurplan.controller;

import com.segurplan.service.ReporteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReporteControllerTest {

    private ReporteService service;
    private ReporteController controller;

    private final Map<String, Object> reporte =
            Map.of("total", 10, "periodo", "2026-11");

    @BeforeEach
    void setUp() {
        service = mock(ReporteService.class);
        controller = new ReporteController(service);
    }

    @Test
    void reporteGeneralCorrectamente() {
        when(service.generarReporteGeneral(2026, 11)).thenReturn(reporte);

        ResponseEntity<?> respuesta = controller.reporteGeneral(2026, 11);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(reporte, respuesta.getBody());
    }

    @Test
    void reporteGeneralConError() {
        when(service.generarReporteGeneral(2026, 11))
                .thenThrow(new IllegalArgumentException("Periodo inválido"));

        ResponseEntity<?> respuesta = controller.reporteGeneral(2026, 11);

        assertEquals(400, respuesta.getStatusCode().value());
    }

    @Test
    void reporteSiniestrosCorrectamente() {
        when(service.generarReporteSiniestros(2026, 11)).thenReturn(reporte);

        ResponseEntity<?> respuesta = controller.reporteSiniestros(2026, 11);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(reporte, respuesta.getBody());
    }

    @Test
    void reporteSiniestrosConError() {
        when(service.generarReporteSiniestros(2026, 11))
                .thenThrow(new IllegalArgumentException("Error de periodo"));

        ResponseEntity<?> respuesta = controller.reporteSiniestros(2026, 11);

        assertEquals(400, respuesta.getStatusCode().value());
    }

    @Test
    void reporteAfpCorrectamente() {
        when(service.generarReporteAfp(2026, 11)).thenReturn(reporte);

        ResponseEntity<?> respuesta = controller.reporteAfp(2026, 11);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(reporte, respuesta.getBody());
    }

    @Test
    void reporteAfpConError() {
        when(service.generarReporteAfp(2026, 11))
                .thenThrow(new IllegalArgumentException("Error AFP"));

        ResponseEntity<?> respuesta = controller.reporteAfp(2026, 11);

        assertEquals(400, respuesta.getStatusCode().value());
    }

    @Test
    void cotizacionesPorEstadoCorrectamente() {
        when(service.generarCotizacionesPorEstado(2026, 11))
                .thenReturn(reporte);

        ResponseEntity<?> respuesta =
                controller.cotizacionesPorEstado(2026, 11);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(reporte, respuesta.getBody());
    }

    @Test
    void conversionVentasCorrectamente() {
        when(service.generarConversionVentas(2026, 11))
                .thenReturn(reporte);

        ResponseEntity<?> respuesta =
                controller.conversionVentas(2026, 11);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(reporte, respuesta.getBody());
    }

    @Test
    void carteraPolizasCorrectamente() {
        when(service.generarCarteraPolizas(2026, 11))
                .thenReturn(reporte);

        ResponseEntity<?> respuesta =
                controller.carteraPolizas(2026, 11);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(reporte, respuesta.getBody());
    }

    @Test
    void documentosPorTipoEstadoCorrectamente() {
        when(service.generarDocumentosPorTipoEstado(2026, 11))
                .thenReturn(reporte);

        ResponseEntity<?> respuesta =
                controller.documentosPorTipoEstado(2026, 11);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(reporte, respuesta.getBody());
    }

    @Test
    void reporteAfpPorPeriodoCorrectamente() {
        when(service.generarReporteAfp(2026, 11))
                .thenReturn(reporte);

        ResponseEntity<?> respuesta =
                controller.reporteAfpPorPeriodo(2026, 11);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(reporte, respuesta.getBody());
    }

    @Test
    void siniestrosPorAseguradoraCorrectamente() {
        when(service.generarSiniestrosPorAseguradora(2026, 11))
                .thenReturn(reporte);

        ResponseEntity<?> respuesta =
                controller.siniestrosPorAseguradora(2026, 11);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(reporte, respuesta.getBody());
    }

    @Test
    void indicadoresGerencialesCorrectamente() {
        when(service.generarIndicadoresGerenciales(2026, 11))
                .thenReturn(reporte);

        ResponseEntity<?> respuesta =
                controller.indicadoresGerenciales(2026, 11);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(reporte, respuesta.getBody());
    }

    @Test
    void trazabilidadCorrectamente() {
        when(service.generarTrazabilidad(2026, 11))
                .thenReturn(reporte);

        ResponseEntity<?> respuesta =
                controller.trazabilidad(2026, 11);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(reporte, respuesta.getBody());
    }
}


package com.segurplan.service;

import com.segurplan.repository.HistorialRepository;

import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private HistorialRepository historialRepository;

    @InjectMocks
    private ReporteService reporteService;

    private List<BiConsumer<Integer, Integer>> reportes() {
        return List.of(
                reporteService::generarReporteGeneral,
                reporteService::generarReporteSiniestros,
                reporteService::generarReporteAfp,
                reporteService::generarCotizacionesPorEstado,
                reporteService::generarConversionVentas,
                reporteService::generarCarteraPolizas,
                reporteService::generarDocumentosPorTipoEstado,
                reporteService::generarReporteAfpPorPeriodo,
                reporteService::generarSiniestrosPorAseguradora,
                reporteService::generarIndicadoresGerenciales,
                reporteService::generarTrazabilidad
        );
    }

    private void verificarErrorAnio(Integer anio) {
        for (BiConsumer<Integer, Integer> reporte : reportes()) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> reporte.accept(anio, 5)
            );

            assertEquals(
                    "El año indicado no es válido.",
                    exception.getMessage()
            );
        }
    }

    private void verificarErrorMes(Integer mes) {
        for (BiConsumer<Integer, Integer> reporte : reportes()) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> reporte.accept(2026, mes)
            );

            assertEquals(
                    "El mes indicado no es válido.",
                    exception.getMessage()
            );
        }
    }

    @Test
    void reportes_anioNulo_lanzanExcepcion() {
        verificarErrorAnio(null);
    }

    @Test
    void reportes_anioMenorAlLimite_lanzanExcepcion() {
        verificarErrorAnio(1999);
    }

    @Test
    void reportes_anioMayorAlLimite_lanzanExcepcion() {
        verificarErrorAnio(2101);
    }

    @Test
    void reportes_mesNulo_lanzanExcepcion() {
        verificarErrorMes(null);
    }

    @Test
    void reportes_mesNegativo_lanzanExcepcion() {
        verificarErrorMes(-1);
    }

    @Test
    void reportes_mesMayorA12_lanzanExcepcion() {
        verificarErrorMes(13);
    }

    @Test
    void reportes_anioYMesInvalidos_priorizaErrorDeAnio() {
        for (BiConsumer<Integer, Integer> reporte : reportes()) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> reporte.accept(1999, 13)
            );

            assertEquals(
                    "El año indicado no es válido.",
                    exception.getMessage()
            );
        }
    }
}

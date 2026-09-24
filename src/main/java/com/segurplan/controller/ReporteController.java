package com.segurplan.controller;

import com.segurplan.service.ReporteService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(
            ReporteService reporteService) {

        this.reporteService
                = reporteService;
    }

    // =========================================================
    // REPORTE GENERAL
    // =========================================================
    @GetMapping("/general")
    public ResponseEntity<?> reporteGeneral(
            @RequestParam Integer anio,
            @RequestParam Integer mes) {

        try {

            return ResponseEntity.ok(
                    reporteService
                            .generarReporteGeneral(
                                    anio,
                                    mes
                            )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "mensaje",
                                    e.getMessage()
                            )
                    );
        }
    }
    // =========================================================
// REPORTE DE SINIESTROS
// =========================================================

    @GetMapping("/siniestros")
    public ResponseEntity<?> reporteSiniestros(
            @RequestParam Integer anio,
            @RequestParam Integer mes) {

        try {

            return ResponseEntity.ok(
                    reporteService
                            .generarReporteSiniestros(
                                    anio,
                                    mes
                            )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "mensaje",
                                    e.getMessage()
                            )
                    );
        }
    }
    // =========================================================
// REPORTE AFP
// =========================================================

    @GetMapping("/afp")
    public ResponseEntity<?> reporteAfp(
            @RequestParam Integer anio,
            @RequestParam Integer mes) {

        try {

            return ResponseEntity.ok(
                    reporteService
                            .generarReporteAfp(
                                    anio,
                                    mes
                            )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "mensaje",
                                    e.getMessage()
                            )
                    );
        }
    }
    // =========================================================
// REP-MV-01 - COTIZACIONES POR ESTADO
// =========================================================
@GetMapping("/marketing/cotizaciones-estado")
public ResponseEntity<?> cotizacionesPorEstado(
        @RequestParam Integer anio,
        @RequestParam Integer mes) {

    try {

        return ResponseEntity.ok(
                reporteService.generarCotizacionesPorEstado(
                        anio,
                        mes
                )
        );

    } catch (IllegalArgumentException e) {

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "mensaje",
                                e.getMessage()
                        )
                );
    }
}
// =========================================================
// REP-MV-02 - CONVERSIÓN DE VENTAS
// =========================================================
@GetMapping("/marketing/conversion-ventas")
public ResponseEntity<?> conversionVentas(
        @RequestParam Integer anio,
        @RequestParam Integer mes) {

    try {

        return ResponseEntity.ok(
                reporteService.generarConversionVentas(
                        anio,
                        mes
                )
        );

    } catch (IllegalArgumentException e) {

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "mensaje",
                                e.getMessage()
                        )
                );
    }
}
// =========================================================
// REP-MV-05 - CARTERA DE PÓLIZAS
// =========================================================
@GetMapping("/marketing/cartera-polizas")
public ResponseEntity<?> carteraPolizas(
        @RequestParam Integer anio,
        @RequestParam Integer mes) {

    try {

        return ResponseEntity.ok(
                reporteService.generarCarteraPolizas(
                        anio,
                        mes
                )
        );

    } catch (IllegalArgumentException e) {

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "mensaje",
                                e.getMessage()
                        )
                );
    }
}
}

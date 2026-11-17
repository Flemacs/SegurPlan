package com.segurplan.controller;
import com.segurplan.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Reportes (JSON)", description = "Datos de los reportes para pantalla y exportación. Solo Administrador.")
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
    @io.swagger.v3.oas.annotations.Operation(summary = "Reporte general", description = "**Roles:** Administrador. Parámetros anio y mes (0 = todo el histórico / año completo).")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Reporte de siniestros", description = "**Roles:** Administrador. Parámetros anio y mes.")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Reporte AFP", description = "**Roles:** Administrador. Parámetros anio y mes.")
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
@io.swagger.v3.oas.annotations.Operation(summary = "Cotizaciones por estado", description = "**Roles:** Administrador. Parámetros anio y mes.")
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
@io.swagger.v3.oas.annotations.Operation(summary = "Conversión de ventas", description = "**Roles:** Administrador. Parámetros anio y mes.")
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
@io.swagger.v3.oas.annotations.Operation(summary = "Cartera de pólizas", description = "**Roles:** Administrador. Parámetros anio y mes.")
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
// =========================================================
// REP-LA-01 - DOCUMENTOS POR TIPO Y ESTADO
// =========================================================
@io.swagger.v3.oas.annotations.Operation(summary = "Reporte de documentos", description = "**Roles:** Administrador. Parámetros anio y mes.")
@GetMapping("/logistica/documentos")
public ResponseEntity<?> documentosPorTipoEstado(
        @RequestParam Integer anio,
        @RequestParam Integer mes) {

    try {

        return ResponseEntity.ok(
                reporteService.generarDocumentosPorTipoEstado(
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
// REP-PO-01 - REPORTE AFP
// =========================================================
@io.swagger.v3.oas.annotations.Operation(summary = "Reporte operativo AFP", description = "**Roles:** Administrador. Parámetros anio y mes.")
@GetMapping("/operaciones/afp")
public ResponseEntity<?> reporteAfpPorPeriodo(
        @RequestParam Integer anio,
        @RequestParam Integer mes) {

    try {

        return ResponseEntity.ok(
                reporteService.generarReporteAfp(
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
// REP-PO-03 - SINIESTROS POR ASEGURADORA
// =========================================================
@io.swagger.v3.oas.annotations.Operation(summary = "Siniestros por aseguradora", description = "**Roles:** Administrador. Parámetros anio y mes.")
@GetMapping("/operaciones/siniestros-aseguradora")
public ResponseEntity<?> siniestrosPorAseguradora(
        @RequestParam Integer anio,
        @RequestParam Integer mes) {

    try {

        return ResponseEntity.ok(
                reporteService.generarSiniestrosPorAseguradora(
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
// REP-DG-02 - INDICADORES GERENCIALES
// =========================================================
@io.swagger.v3.oas.annotations.Operation(summary = "Indicadores de gerencia", description = "**Roles:** Administrador. Parámetros anio y mes.")
@GetMapping("/gerencia/indicadores")
public ResponseEntity<?> indicadoresGerenciales(
        @RequestParam Integer anio,
        @RequestParam Integer mes) {

    try {

        return ResponseEntity.ok(
                reporteService.generarIndicadoresGerenciales(
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
// REP-OT-02 - HISTORIAL Y TRAZABILIDAD
// =========================================================
@io.swagger.v3.oas.annotations.Operation(summary = "Trazabilidad", description = "**Roles:** Administrador. Parámetros anio y mes.")
@GetMapping("/trazabilidad")
public ResponseEntity<?> trazabilidad(
        @RequestParam Integer anio,
        @RequestParam Integer mes) {

    try {

        return ResponseEntity.ok(
                reporteService.generarTrazabilidad(
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

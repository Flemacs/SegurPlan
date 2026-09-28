package com.segurplan.controller;

import com.segurplan.dto.SiniestroRequest;
import com.segurplan.model.Siniestro;
import com.segurplan.service.SiniestroService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/siniestros")
public class SiniestroController {

    private final SiniestroService siniestroService;

    public SiniestroController(
            SiniestroService siniestroService) {

        this.siniestroService =
                siniestroService;
    }


    // =====================================================
    // REGISTRAR SINIESTRO
    // =====================================================

    @PostMapping
    public ResponseEntity<?> registrar(
            @RequestBody SiniestroRequest request,
            Principal principal) {

        try {

            Siniestro siniestro =
                    siniestroService.registrar(
                            principal.getName(),
                            request
                    );

            Map<String, Object> respuesta =
                    convertir(siniestro);

            respuesta.put(
                    "mensaje",
                    "Siniestro registrado correctamente."
            );

            return ResponseEntity.ok(
                    respuesta
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


    // =====================================================
    // MIS SINIESTROS
    // =====================================================

    @GetMapping("/mis-siniestros")
    public ResponseEntity<?> misSiniestros(
            Principal principal) {

        try {

            List<Map<String, Object>> respuesta =
                    siniestroService
                            .listarDelUsuario(
                                    principal.getName()
                            )
                            .stream()
                            .map(this::convertir)
                            .toList();

            return ResponseEntity.ok(
                    respuesta
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


    // =====================================================
    // OBTENER UNO
    // =====================================================

    @GetMapping("/{idSiniestro}")
    public ResponseEntity<?> obtener(
            @PathVariable Long idSiniestro,
            Principal principal) {

        try {

            Siniestro siniestro =
                    siniestroService
                            .obtenerDelUsuario(
                                    idSiniestro,
                                    principal.getName()
                            );

            return ResponseEntity.ok(
                    convertir(siniestro)
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


    // =====================================================
    // CONVERSIÓN A JSON
    // =====================================================

    private Map<String, Object> convertir(
            Siniestro s) {

        Map<String, Object> item =
                new LinkedHashMap<>();

        item.put(
                "idSiniestro",
                s.getIdSiniestro()
        );

        item.put(
                "numeroSiniestro",
                s.getNumeroSiniestro()
        );

        item.put(
                "fechaRegistro",
                s.getFechaRegistro()
        );

        item.put(
                "fechaAccidente",
                s.getFechaAccidente()
        );

        item.put(
                "placa",
                s.getPlaca()
        );

        item.put(
                "marca",
                s.getMarca()
        );

        item.put(
                "modelo",
                s.getModelo()
        );

        item.put(
                "anio",
                s.getAnio()
        );

        item.put(
                "lugar",
                s.getLugar()
        );

        item.put(
                "descripcion",
                s.getDescripcion()
        );

        item.put(
                "resultadoEvaluacion",
                s.getResultadoEvaluacion()
        );

        item.put(
                "observaciones",
                s.getObservaciones()
        );

        item.put(
                "estado",
                s.getEstado()
        );

        return item;
    }
    // =====================================================
// DERIVAR SINIESTRO A ASEGURADORA
// =====================================================

@PutMapping("/{idSiniestro}/derivar")
public ResponseEntity<?> derivarAseguradora(
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

        Siniestro siniestro =
                siniestroService.derivarAseguradora(
                        principal.getName(),
                        idSiniestro
                );

        return ResponseEntity.ok(
                Map.of(
                        "mensaje",
                        "Siniestro derivado correctamente a la aseguradora.",
                        "idSiniestro",
                        siniestro.getIdSiniestro(),
                        "numeroSiniestro",
                        siniestro.getNumeroSiniestro(),
                        "estado",
                        siniestro.getEstado()
                )
        );

    } catch (IllegalArgumentException e) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "mensaje",
                        e.getMessage()
                ));

    } catch (Exception e) {

        e.printStackTrace();

        return ResponseEntity
                .internalServerError()
                .body(Map.of(
                        "mensaje",
                        "No se pudo derivar el siniestro."
                ));
    }
}
// =====================================================
// SINIESTROS PENDIENTES - ASESOR
// =====================================================

@GetMapping("/asesor/pendientes")
public ResponseEntity<?> listarPendientesAsesor(
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

        List<Map<String, Object>> resultado =
                siniestroService
                        .listarPendientesAsesor()
                        .stream()
                        .map(this::convertirSiniestroAsesor)
                        .toList();

        return ResponseEntity.ok(resultado);

    } catch (Exception e) {

        e.printStackTrace();

        return ResponseEntity
                .internalServerError()
                .body(Map.of(
                        "mensaje",
                        "No se pudieron consultar los siniestros pendientes."
                ));
    }
}


// =====================================================
// CONVERTIR SINIESTRO PARA PANEL DEL ASESOR
// =====================================================

private Map<String, Object> convertirSiniestroAsesor(
        Siniestro siniestro) {

    Map<String, Object> dto =
            new LinkedHashMap<>();

    // =====================================================
    // SINIESTRO
    // =====================================================

    dto.put(
            "idSiniestro",
            siniestro.getIdSiniestro()
    );

    dto.put(
            "numeroSiniestro",
            siniestro.getNumeroSiniestro()
    );

    dto.put(
            "fechaAccidente",
            siniestro.getFechaAccidente()
    );

    dto.put(
            "fechaRegistro",
            siniestro.getFechaRegistro()
    );

    dto.put(
            "estado",
            siniestro.getEstado()
    );

    // =====================================================
    // VEHÍCULO
    // =====================================================

    dto.put(
            "placa",
            siniestro.getPlaca()
    );

    dto.put(
            "marca",
            siniestro.getMarca()
    );

    dto.put(
            "modelo",
            siniestro.getModelo()
    );

    dto.put(
            "anio",
            siniestro.getAnio()
    );

    // =====================================================
    // ACCIDENTE
    // =====================================================

    dto.put(
            "lugar",
            siniestro.getLugar()
    );

    dto.put(
            "descripcion",
            siniestro.getDescripcion()
    );

    dto.put(
            "resultadoEvaluacion",
            siniestro.getResultadoEvaluacion()
    );

    dto.put(
            "observaciones",
            siniestro.getObservaciones()
    );

    // =====================================================
    // USUARIO
    // =====================================================

    if (siniestro.getUsuario() != null) {

        dto.put(
                "idUsuario",
                siniestro.getUsuario()
                        .getIdUsuario()
        );

        String nombres =
                siniestro.getUsuario()
                        .getNombres() != null
                ? siniestro.getUsuario()
                        .getNombres()
                : "";

        String apellidos =
                siniestro.getUsuario()
                        .getApellidos() != null
                ? siniestro.getUsuario()
                        .getApellidos()
                : "";

        String nombreCompleto =
                (nombres + " " + apellidos)
                        .trim();

        dto.put(
                "nombreUsuario",
                nombreCompleto
        );

        dto.put(
                "correoUsuario",
                siniestro.getUsuario()
                        .getCorreo()
        );
    }

    // =====================================================
    // ASEGURADORA
    // =====================================================

    if (siniestro.getAseguradora() != null) {

        dto.put(
                "idAseguradora",
                siniestro.getAseguradora()
                        .getIdAseguradora()
        );

        dto.put(
                "nombreAseguradora",
                siniestro.getAseguradora()
                        .getNombre()
        );
    }

    return dto;
}
// =====================================================
// CONSULTAR SINIESTRO PARA REVISIÓN INTERNA
// =====================================================

@GetMapping("/asesor/{idSiniestro}")
public ResponseEntity<?> obtenerParaRevision(
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

        Siniestro siniestro =
                siniestroService
                        .obtenerParaRevision(
                                idSiniestro
                        );

        return ResponseEntity.ok(
                convertirSiniestroAsesor(
                        siniestro
                )
        );

    } catch (IllegalArgumentException e) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "mensaje",
                        e.getMessage()
                ));

    } catch (Exception e) {

        e.printStackTrace();

        return ResponseEntity
                .internalServerError()
                .body(Map.of(
                        "mensaje",
                        "No se pudo consultar el siniestro."
                ));
    }
}
}
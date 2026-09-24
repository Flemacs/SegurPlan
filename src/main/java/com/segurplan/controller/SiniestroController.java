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
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.segurplan.controller;

import com.segurplan.dto.PolizaRequest;
import com.segurplan.model.Poliza;
import com.segurplan.service.PolizaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/polizas")
public class PolizaController {
    private final PolizaService polizaService;

    public PolizaController(PolizaService polizaService) {
        this.polizaService = polizaService;
    }

    @PostMapping
    public ResponseEntity<?> generar(@RequestBody PolizaRequest request) {
        try {
            Poliza poliza = polizaService.generar(request);

            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("mensaje", "Póliza generada correctamente");
            respuesta.put("idPoliza", poliza.getIdPoliza());
            respuesta.put("numeroPoliza", poliza.getNumeroPoliza());
            respuesta.put("estado", poliza.getEstado());
            respuesta.put("fechaInicio", poliza.getFechaInicio());
            respuesta.put("fechaFin", poliza.getFechaFin());

            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", e.getMessage()));
        }
    }
}
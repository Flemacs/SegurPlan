/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.segurplan.controller;

import com.segurplan.dto.ContratacionRequest;
import com.segurplan.model.Contratacion;
import com.segurplan.service.ContratacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Contrataciones", description = "Contrataciones de seguros (uso del personal).")
@RestController
@RequestMapping("/api/contrataciones")
public class ContratacionController {

    private final ContratacionService contratacionService;

    public ContratacionController(
            ContratacionService contratacionService) {

        this.contratacionService =
                contratacionService;
    }
    @io.swagger.v3.oas.annotations.Operation(summary = "Listar contrataciones pendientes", description = "**Roles:** Asesor, Administrador. Contrataciones aún sin póliza.")
    @GetMapping("/pendientes")
    public ResponseEntity<?> listarPendientes() {
    return ResponseEntity.ok(
        contratacionService.listarPendientes()
    );
}

    @io.swagger.v3.oas.annotations.Operation(summary = "Registrar una contratación", description = "**Roles:** Asesor, Administrador. Crea la contratación de una cotización aceptada.")
    @PostMapping
    public ResponseEntity<?> crear(
            @RequestBody ContratacionRequest request) {

        try {

            Contratacion c =
                    contratacionService.crear(request);

            Map<String, Object> respuesta =
                    new HashMap<>();

            respuesta.put(
                    "mensaje",
                    "Contratación registrada correctamente"
            );

            respuesta.put(
                    "idContratacion",
                    c.getIdContratacion()
            );

            respuesta.put(
                    "idCotizacion",
                    c.getCotizacion().getIdCotizacion()
            );

            respuesta.put(
                    "estado",
                    c.getEstado()
            );

            respuesta.put(
                    "fechaContratacion",
                    c.getFechaContratacion()
            );

            return ResponseEntity.ok(
                    respuesta
            );

        } catch (RuntimeException e) {

            Map<String, String> error =
                    new HashMap<>();

            error.put(
                    "mensaje",
                    e.getMessage()
            );

            return ResponseEntity
                    .badRequest()
                    .body(error);
        }
    }
    
}
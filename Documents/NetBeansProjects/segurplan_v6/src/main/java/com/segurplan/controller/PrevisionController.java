package com.segurplan.controller;

import com.segurplan.dto.PrevisionRequest;
import com.segurplan.model.Prevision;
import com.segurplan.service.PrevisionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/previsiones")
public class PrevisionController {

    private final PrevisionService previsionService;

    public PrevisionController(
            PrevisionService previsionService) {

        this.previsionService = previsionService;
    }

    // =========================================================
    // CREAR SIMULACIÓN AFP
    // =========================================================
    @PostMapping
    public ResponseEntity<?> simular(
            @RequestBody PrevisionRequest request,
            Principal principal) {

        try {

            Prevision prevision
                    = previsionService.simular(
                            principal.getName(),
                            request
                    );

            return ResponseEntity.ok(
                    convertir(prevision)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }

    // =========================================================
    // MIS SIMULACIONES
    // =========================================================
    @GetMapping("/mis-simulaciones")
    public ResponseEntity<?> misSimulaciones(
            Principal principal) {

        List<Map<String, Object>> resultado
                = previsionService
                        .listarPorUsuario(
                                principal.getName()
                        )
                        .stream()
                        .map(this::convertir)
                        .toList();

        return ResponseEntity.ok(resultado);
    }

    // =========================================================
    // OBTENER SIMULACIÓN
    // =========================================================
    @GetMapping("/{idPrevision}")
    public ResponseEntity<?> obtener(
            @PathVariable Long idPrevision,
            Principal principal) {

        try {

            Prevision prevision
                    = previsionService
                            .obtenerDelUsuario(
                                    idPrevision,
                                    principal.getName()
                            );

            return ResponseEntity.ok(
                    convertir(prevision)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }
// =========================================================
// SOLICITAR ORIENTACIÓN
// CLIENTE
// =========================================================

    @PutMapping("/{idPrevision}/solicitar-orientacion")
    public ResponseEntity<?> solicitarOrientacion(
            @PathVariable Long idPrevision,
            Principal principal) {

        try {

            Prevision prevision
                    = previsionService.solicitarOrientacion(
                            idPrevision,
                            principal.getName()
                    );

            return ResponseEntity.ok(
                    convertir(prevision)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }

// =========================================================
// SOLICITUDES AFP PENDIENTES PARA EL ASESOR
// =========================================================
    @GetMapping("/asesor/pendientes")
    public ResponseEntity<?> solicitudesAsesor() {

        List<Map<String, Object>> resultado
                = previsionService
                        .listarSolicitudesAsesor()
                        .stream()
                        .map(this::convertir)
                        .toList();

        return ResponseEntity.ok(
                resultado
        );
    }

// =========================================================
// RESPONDER SOLICITUD AFP
// ASESOR
// =========================================================
    @PutMapping("/{idPrevision}/respuesta")
    public ResponseEntity<?> responder(
            @PathVariable Long idPrevision,
            @RequestBody Map<String, String> body) {

        try {

            Prevision prevision
                    = previsionService
                            .responderOrientacion(
                                    idPrevision,
                                    body.get("respuesta")
                            );

            return ResponseEntity.ok(
                    convertir(prevision)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }

    // =========================================================
    // CONVERTIR ENTIDAD A JSON
    // =========================================================
    private Map<String, Object> convertir(
            Prevision p) {

        Map<String, Object> json
                = new LinkedHashMap<>();

        json.put(
                "idPrevision",
                p.getIdPrevision()
        );
        json.put(
                "idUsuario",
                p.getUsuario().getIdUsuario()
        );

        json.put(
                "cliente",
                p.getUsuario().getNombres()
                + " "
                + p.getUsuario().getApellidos()
        );
        json.put(
                "tipoSistema",
                p.getTipoSistema()
        );

        json.put(
                "edad",
                p.getEdad()
        );

        json.put(
                "ingresoMensual",
                p.getIngresoMensual()
        );

        json.put(
                "aniosAporte",
                p.getAniosAporte()
        );

        json.put(
                "fechaRegistro",
                p.getFechaRegistro()
        );

        json.put(
                "montoPensionEstimado",
                p.getMontoPensionEstimado()
        );

        json.put(
                "resultadoSimulacion",
                p.getResultadoSimulacion()
        );

        json.put(
                "consulta",
                p.getConsulta()
        );

        json.put(
                "respuesta",
                p.getRespuesta()
        );

        json.put(
                "estado",
                p.getEstado()
        );

        return json;
    }
}

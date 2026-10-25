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
    private final com.segurplan.service.NotificacionService notificaciones;

    public PrevisionController(
            PrevisionService previsionService,
            com.segurplan.service.NotificacionService notificaciones) {

        this.previsionService = previsionService;
        this.notificaciones = notificaciones;
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

            notificaciones.notificarRol("Asesor", "AFP_SOLICITUD",
                    "Nueva solicitud de orientación AFP",
                    "Un cliente solicitó orientación sobre su simulación N° " + idPrevision + ".",
                    "PREVISION", idPrevision);

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

            try {
                notificaciones.notificar(
                        prevision.getUsuario().getIdUsuario(),
                        "AFP_RESPUESTA",
                        "Respuesta a su consulta AFP",
                        "Un asesor respondió su solicitud de orientación.",
                        "PREVISION", idPrevision);
            } catch (RuntimeException ignorada) {
                // la notificación nunca debe impedir responder
            }

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

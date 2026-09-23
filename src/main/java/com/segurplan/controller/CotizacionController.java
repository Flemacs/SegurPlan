package com.segurplan.controller;

import com.segurplan.dto.CotizacionRequest;
import com.segurplan.model.Cotizacion;
import com.segurplan.service.CotizacionService;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.segurplan.dto.RespuestaCotizacionRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @PostMapping
    public ResponseEntity<?> crear(
            @RequestBody CotizacionRequest request) {

        try {

            Cotizacion cotizacion =
                    cotizacionService.crear(request);

            Map<String, Object> respuesta = new HashMap<>();

            respuesta.put(
                    "mensaje",
                    "Cotización solicitada correctamente"
            );

            respuesta.put(
                    "idCotizacion",
                    cotizacion.getIdCotizacion()
            );

            respuesta.put(
                    "estado",
                    cotizacion.getEstado()
            );

            respuesta.put(
                    "fechaSolicitud",
                    cotizacion.getFechaSolicitud()
            );

            return ResponseEntity.ok(respuesta);

        } catch (RuntimeException e) {

            Map<String, String> error = new HashMap<>();
            error.put("mensaje", e.getMessage());

            return ResponseEntity
                    .badRequest()
                    .body(error);
        }
    }
    @GetMapping("/{idCotizacion}")
public ResponseEntity<?> buscarPorId(
        @PathVariable Long idCotizacion) {

    try {

        Cotizacion cotizacion =
                cotizacionService.buscarPorId(idCotizacion);

        Map<String, Object> respuesta =
                new HashMap<>();

        respuesta.put(
                "idCotizacion",
                cotizacion.getIdCotizacion()
        );

        respuesta.put(
                "fechaSolicitud",
                cotizacion.getFechaSolicitud()
        );

        respuesta.put(
                "descripcionSolicitud",
                cotizacion.getDescripcionSolicitud()
        );

        respuesta.put(
                "estado",
                cotizacion.getEstado()
        );

        // Usuario
        respuesta.put(
                "idUsuario",
                cotizacion.getUsuario().getIdUsuario()
        );

        respuesta.put(
                "nombreSolicitante",
                cotizacion.getUsuario().getNombres()
                + " "
                + cotizacion.getUsuario().getApellidos()
        );

        // Tipo de seguro
        respuesta.put(
                "idTipoSeguro",
                cotizacion.getTipoSeguro().getIdTipoSeguro()
        );

        respuesta.put(
                "tipoSeguro",
                cotizacion.getTipoSeguro().getNombre()
        );

        // Aseguradora
        respuesta.put(
                "idAseguradora",
                cotizacion.getAseguradora().getIdAseguradora()
        );

        respuesta.put(
                "aseguradora",
                cotizacion.getAseguradora().getNombre()
        );

        respuesta.put(
                "precio",
                cotizacion.getPrecio()
        );

        respuesta.put(
                "vigencia",
                cotizacion.getVigencia()
        );

        respuesta.put(
                "cobertura",
                cotizacion.getCobertura()
        );

        respuesta.put(
                "deducible",
                cotizacion.getDeducible()
        );

        return ResponseEntity.ok(respuesta);

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

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Cotizacion>> listarPorUsuario(
            @PathVariable Long idUsuario) {

        return ResponseEntity.ok(
                cotizacionService.listarPorUsuario(idUsuario)
        );
    }

    
    @GetMapping
    public ResponseEntity<?> listarTodas() {

    List<Cotizacion> cotizaciones =
            cotizacionService.listarTodas();

    List<Map<String, Object>> respuesta =
            new ArrayList<>();

    for (Cotizacion c : cotizaciones) {

        Map<String, Object> item =
                new HashMap<>();

        item.put(
                "idCotizacion",
                c.getIdCotizacion()
        );

        item.put(
                "cliente",
                c.getUsuario().getNombres()
                + " "
                + c.getUsuario().getApellidos()
        );

        item.put(
                "idUsuario",
                c.getUsuario().getIdUsuario()
        );

        item.put(
                "tipoSeguro",
                c.getTipoSeguro().getNombre()
        );

        item.put(
                "aseguradora",
                c.getAseguradora().getNombre()
        );

        item.put(
                "fechaSolicitud",
                c.getFechaSolicitud()
        );

        item.put(
                "estado",
                c.getEstado()
        );

        item.put(
                "descripcionSolicitud",
                c.getDescripcionSolicitud()
        );

        respuesta.add(item);
    }

    return ResponseEntity.ok(respuesta);
}
    @PutMapping("/{idCotizacion}/respuesta")
public ResponseEntity<?> responderCotizacion(
        @PathVariable Long idCotizacion,
        @RequestBody RespuestaCotizacionRequest request) {

    try {

        Cotizacion cotizacion =
                cotizacionService.responderCotizacion(
                        idCotizacion,
                        request
                );

        Map<String, Object> respuesta =
                new HashMap<>();

        respuesta.put(
                "mensaje",
                "Cotización actualizada correctamente"
        );

        respuesta.put(
                "idCotizacion",
                cotizacion.getIdCotizacion()
        );

        respuesta.put(
                "estado",
                cotizacion.getEstado()
        );

        respuesta.put(
                "cobertura",
                cotizacion.getCobertura()
        );

        respuesta.put(
                "vigencia",
                cotizacion.getVigencia()
        );

        respuesta.put(
                "fechaRespuesta",
                cotizacion.getFechaRespuesta()
        );

        return ResponseEntity.ok(respuesta);

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
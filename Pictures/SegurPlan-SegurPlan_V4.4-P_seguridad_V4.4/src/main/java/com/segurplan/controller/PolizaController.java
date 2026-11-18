
package com.segurplan.controller;

import com.segurplan.dto.PolizaRequest;
import com.segurplan.dto.PolizaResumenDTO;
import com.segurplan.model.Poliza;
import com.segurplan.service.PolizaService;
import com.segurplan.model.Usuario;
import com.segurplan.repository.UsuarioRepository;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/polizas")
public class PolizaController {

    private final PolizaService polizaService;
    private final UsuarioRepository usuarioRepository;
    private final com.segurplan.service.AccesoService acceso;
    private final com.segurplan.service.NotificacionService notificaciones;

    public PolizaController(
        PolizaService polizaService,
        UsuarioRepository usuarioRepository,
        com.segurplan.service.AccesoService acceso,
        com.segurplan.service.NotificacionService notificaciones) {

    this.polizaService = polizaService;
    this.usuarioRepository = usuarioRepository;
    this.acceso = acceso;
    this.notificaciones = notificaciones;
}

    @PostMapping
    public ResponseEntity<?> generar(
            @RequestBody PolizaRequest request
    ) {
        try {
            Poliza poliza = polizaService.generar(request);

            try {
                notificaciones.notificar(
                        poliza.getContratacion().getUsuario().getIdUsuario(),
                        "POLIZA_GENERADA",
                        "Póliza " + poliza.getNumeroPoliza() + " generada",
                        "Ya puede consultar y descargar su póliza en PDF.",
                        "POLIZA", poliza.getIdPoliza());
            } catch (RuntimeException ignorada) {
                // la notificación nunca debe impedir la emisión de la póliza
            }

            Map<String, Object> respuesta =
                    new LinkedHashMap<>();

            respuesta.put(
                    "mensaje",
                    "Póliza generada correctamente"
            );

            respuesta.put(
                    "idPoliza",
                    poliza.getIdPoliza()
            );

            respuesta.put(
                    "numeroPoliza",
                    poliza.getNumeroPoliza()
            );

            respuesta.put(
                    "estado",
                    poliza.getEstado()
            );

            respuesta.put(
                    "fechaInicio",
                    poliza.getFechaInicio()
            );

            respuesta.put(
                    "fechaFin",
                    poliza.getFechaFin()
            );

            return ResponseEntity.ok(respuesta);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<?> listarPorUsuario(
            @PathVariable Long idUsuario,
            org.springframework.security.core.Authentication auth
    ) {
        if (!acceso.puedeVer(auth, idUsuario)) {
            return ResponseEntity.status(403).body(Map.of(
                    "mensaje", "No tiene permiso para ver estas pólizas."));
        }
        try {
            List<PolizaResumenDTO> polizas =
                    polizaService.listarPorUsuario(idUsuario);

            return ResponseEntity.ok(polizas);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje",
                            e.getMessage()
                    ));
        }
    }
    
@GetMapping("/mis-polizas")
public ResponseEntity<?> misPolizas(Principal principal) {

    if (principal == null) {
        return ResponseEntity.status(401)
                .body(Map.of(
                        "mensaje",
                        "Debe iniciar sesión."
                ));
    }

    Usuario usuario = usuarioRepository
            .findByCorreo(principal.getName())
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Usuario no encontrado."
                    ));

    return ResponseEntity.ok(
            polizaService.listarPorUsuario(
                    usuario.getIdUsuario()
            )
    );
}
}
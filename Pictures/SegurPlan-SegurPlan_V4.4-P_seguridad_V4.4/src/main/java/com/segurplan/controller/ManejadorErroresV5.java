package com.segurplan.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Respuestas de error homogéneas para los controladores incorporados en la v5. */
@RestControllerAdvice(assignableTypes = {
        NotificacionController.class,
        PerfilController.class,
        SiniestroGestionController.class,
        DocumentoGestionController.class,
        CotizacionExtraController.class,
        PolizaPdfController.class,
        PermisoAdminController.class
})
public class ManejadorErroresV5 {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresV5.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> argumento(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> acceso(SecurityException e) {
        return ResponseEntity.status(403).body(Map.of("mensaje", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> general(Exception e) {
        log.error("Error no controlado", e);
        return ResponseEntity.internalServerError()
                .body(Map.of("mensaje", "No se pudo completar la operación."));
    }
}

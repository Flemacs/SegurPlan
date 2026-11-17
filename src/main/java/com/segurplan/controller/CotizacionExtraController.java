package com.segurplan.controller;

import com.segurplan.model.ComunicacionAseguradora;
import com.segurplan.model.Cotizacion;
import com.segurplan.service.AccesoService;
import com.segurplan.service.CotizacionExtraService;
import com.segurplan.service.CotizacionService;
import com.segurplan.service.PdfService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

/** Comunicaciones con la aseguradora (RF005) y PDF de la cotización (RF006). */
@io.swagger.v3.oas.annotations.tags.Tag(name = "Cotizaciones", description = "Solicitud, consulta y respuesta de cotizaciones.")
@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionExtraController {

    private final CotizacionExtraService extra;
    private final CotizacionService cotizacionService;
    private final PdfService pdfService;
    private final AccesoService acceso;

    public CotizacionExtraController(CotizacionExtraService extra, CotizacionService cotizacionService,
                                     PdfService pdfService, AccesoService acceso) {
        this.extra = extra;
        this.cotizacionService = cotizacionService;
        this.pdfService = pdfService;
        this.acceso = acceso;
    }

    /** Solo Asesor/Administrador (SecurityConfig). */
    @io.swagger.v3.oas.annotations.Operation(summary = "Registrar comunicación con la aseguradora", description = "**Roles:** Asesor, Administrador. Deja constancia de un contacto con la aseguradora sobre la cotización.")
    @PostMapping("/{id}/comunicaciones")
    public ResponseEntity<?> registrar(@PathVariable Long id, @RequestBody Map<String, String> body,
                                       Principal principal) {
        extra.registrar(principal.getName(), id, body.get("medio"), body.get("contacto"),
                body.get("observaciones"));
        return ResponseEntity.ok(Map.of("mensaje", "Comunicación registrada."));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Listar comunicaciones con la aseguradora", description = "**Roles:** Asesor, Administrador. Historial de contactos de la cotización.")
    @GetMapping("/{id}/comunicaciones")
    public ResponseEntity<?> listar(@PathVariable Long id) {
        List<Map<String, Object>> r = new ArrayList<>();
        for (ComunicacionAseguradora c : extra.listar(id)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getIdComunicacion());
            m.put("fecha", c.getFecha());
            m.put("medio", c.getMedio());
            m.put("contacto", c.getContacto());
            m.put("observaciones", c.getObservaciones());
            m.put("asesor", c.getAsesor().getNombres() + " " + c.getAsesor().getApellidos());
            r.add(m);
        }
        return ResponseEntity.ok(r);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Descargar la cotización en PDF", description = "**Roles:** Dueño del registro, Asesor o Administrador. PDF generado con el generador propio (SimplePdf/PdfService).")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, Authentication auth) {
        Cotizacion c = cotizacionService.buscarPorId(id);
        if (!acceso.puedeVer(auth, c.getUsuario().getIdUsuario())) {
            throw new SecurityException("No tiene permiso para ver esta cotización.");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cotizacion-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfService.cotizacion(c));
    }
}

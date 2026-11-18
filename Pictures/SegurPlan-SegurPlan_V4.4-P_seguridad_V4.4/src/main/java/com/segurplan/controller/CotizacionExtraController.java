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
    @PostMapping("/{id}/comunicaciones")
    public ResponseEntity<?> registrar(@PathVariable Long id, @RequestBody Map<String, String> body,
                                       Principal principal) {
        extra.registrar(principal.getName(), id, body.get("medio"), body.get("contacto"),
                body.get("observaciones"));
        return ResponseEntity.ok(Map.of("mensaje", "Comunicación registrada."));
    }

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

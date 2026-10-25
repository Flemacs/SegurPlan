package com.segurplan.controller;

import com.segurplan.model.Siniestro;
import com.segurplan.service.AccesoService;
import com.segurplan.service.HistorialService;
import com.segurplan.service.PdfService;
import com.segurplan.service.SiniestroGestionService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Flujo del asesor sobre siniestros (RF022 a RF027). Las rutas /gestion/**
 * solo son accesibles para Asesor y Administrador (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/siniestros")
public class SiniestroGestionController {

    private final SiniestroGestionService gestion;
    private final AccesoService acceso;
    private final PdfService pdfService;
    private final HistorialService historialService;

    public SiniestroGestionController(SiniestroGestionService gestion, AccesoService acceso,
                                      PdfService pdfService, HistorialService historialService) {
        this.gestion = gestion;
        this.acceso = acceso;
        this.pdfService = pdfService;
        this.historialService = historialService;
    }

    // ---------------- Asesor ----------------
    @GetMapping("/gestion")
    public ResponseEntity<?> listar(@RequestParam(required = false) String estado) {
        List<Map<String, Object>> r = new ArrayList<>();
        for (Siniestro s : gestion.listar(estado)) {
            r.add(resumen(s));
        }
        return ResponseEntity.ok(r);
    }

    @GetMapping("/gestion/{id}")
    public ResponseEntity<?> detalle(@PathVariable Long id) {
        Map<String, Object> d = resumen(gestion.obtener(id));
        d.putAll(gestion.lineaDeTiempo(id));
        return ResponseEntity.ok(d);
    }

    /** accion: INICIAR | APROBAR_DERIVACION | PENDIENTE_INFO */
    @PostMapping("/gestion/{id}/revision")
    public ResponseEntity<?> revision(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                      Principal principal) {
        String accion = String.valueOf(body.getOrDefault("accion", ""));
        String mensaje = body.get("mensaje") == null ? null : String.valueOf(body.get("mensaje"));
        Siniestro s;
        switch (accion) {
            case "INICIAR" -> s = gestion.iniciarRevision(principal.getName(), id);
            case "APROBAR_DERIVACION" -> s = gestion.aprobarParaDerivacion(principal.getName(), id, mensaje);
            case "PENDIENTE_INFO" -> {
                List<String> items = new ArrayList<>();
                if (body.get("items") instanceof List<?> lista) {
                    for (Object o : lista) {
                        items.add(String.valueOf(o));
                    }
                }
                s = gestion.solicitarInformacion(principal.getName(), id, items, mensaje);
            }
            default -> throw new IllegalArgumentException("Acción de revisión no válida.");
        }
        return ResponseEntity.ok(Map.of("mensaje", "Revisión registrada.", "estado", s.getEstado()));
    }

    @PutMapping("/gestion/{id}/derivar")
    public ResponseEntity<?> derivar(@PathVariable Long id, Principal principal) {
        Siniestro s = gestion.derivar(principal.getName(), id);
        return ResponseEntity.ok(Map.of("mensaje", "Siniestro derivado a la aseguradora.", "estado", s.getEstado()));
    }

    @PutMapping("/gestion/{id}/evaluacion")
    public ResponseEntity<?> evaluacion(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                        Principal principal) {
        BigDecimal monto = null;
        Object m = body.get("monto");
        if (m != null && !String.valueOf(m).isBlank()) {
            try {
                monto = new BigDecimal(String.valueOf(m));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El monto aprobado no es un número válido.");
            }
        }
        LocalDateTime fecha = null;
        Object f = body.get("fechaEvaluacion");
        if (f != null && !String.valueOf(f).isBlank()) {
            try {
                String txt = String.valueOf(f);
                fecha = txt.length() == 10 ? java.time.LocalDate.parse(txt).atStartOfDay()
                        : LocalDateTime.parse(txt);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("La fecha de evaluación no es válida.");
            }
        }
        Siniestro s = gestion.registrarEvaluacion(principal.getName(), id,
                String.valueOf(body.get("dictamen")), monto,
                body.get("observaciones") == null ? null : String.valueOf(body.get("observaciones")), fecha);
        return ResponseEntity.ok(Map.of("mensaje", "Evaluación registrada.", "estado", s.getEstado()));
    }

    @PutMapping("/gestion/{id}/estado")
    public ResponseEntity<?> estado(@PathVariable Long id, @RequestBody Map<String, String> body,
                                    Principal principal) {
        Siniestro s = gestion.cambiarEstado(principal.getName(), id, body.get("estado"), body.get("observacion"));
        return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado.", "estado", s.getEstado()));
    }

    // ---------------- Dueño o personal ----------------
    @GetMapping("/{id}/timeline")
    public ResponseEntity<?> timeline(@PathVariable Long id, Authentication auth) {
        Siniestro s = gestion.obtener(id);
        verificar(auth, s);
        return ResponseEntity.ok(gestion.lineaDeTiempo(id));
    }

    @GetMapping("/{id}/resumen.pdf")
    public ResponseEntity<byte[]> resumenPdf(@PathVariable Long id, Authentication auth) {
        Siniestro s = gestion.obtener(id);
        verificar(auth, s);
        byte[] pdf = pdfService.resumenSiniestro(s, historialService.listarPorRegistro("SINIESTRO", id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"siniestro-" + s.getNumeroSiniestro() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private void verificar(Authentication auth, Siniestro s) {
        if (!acceso.puedeVer(auth, s.getUsuario().getIdUsuario())) {
            throw new SecurityException("No tiene permiso para ver este siniestro.");
        }
    }

    private Map<String, Object> resumen(Siniestro s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idSiniestro", s.getIdSiniestro());
        m.put("numeroSiniestro", s.getNumeroSiniestro());
        m.put("estado", s.getEstado());
        m.put("fechaRegistro", s.getFechaRegistro());
        m.put("fechaAccidente", s.getFechaAccidente());
        m.put("fechaDerivacion", s.getFechaDerivacion());
        m.put("fechaEvaluacion", s.getFechaEvaluacion());
        m.put("montoAprobado", s.getMontoAprobado());
        m.put("resultadoEvaluacion", s.getResultadoEvaluacion());
        m.put("placa", s.getPlaca());
        m.put("marca", s.getMarca());
        m.put("modelo", s.getModelo());
        m.put("anio", s.getAnio());
        m.put("lugar", s.getLugar());
        m.put("descripcion", s.getDescripcion());
        m.put("observaciones", s.getObservaciones());
        m.put("cliente", s.getUsuario().getNombres() + " " + s.getUsuario().getApellidos());
        m.put("correoCliente", s.getUsuario().getCorreo());
        m.put("telefonoCliente", s.getUsuario().getTelefono());
        m.put("aseguradora", s.getAseguradora() == null ? null : s.getAseguradora().getNombre());
        return m;
    }
}

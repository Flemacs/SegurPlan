package com.segurplan.controller;

import com.segurplan.model.Cotizacion;
import com.segurplan.model.Prevision;
import com.segurplan.model.Siniestro;
import com.segurplan.model.Usuario;
import com.segurplan.repository.ContratacionRepository;
import com.segurplan.repository.CotizacionRepository;
import com.segurplan.repository.PolizaRepository;
import com.segurplan.repository.UsuarioRepository;
import com.segurplan.service.PrevisionService;
import com.segurplan.service.SiniestroService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Historial unificado del cliente: cotizaciones, siniestros y AFP.
 * Cada fila indica cuál es el siguiente paso (accion) del proceso.
 */
@RestController
@RequestMapping("/api/mis-procesos")
public class HistorialClienteController {

    private final UsuarioRepository usuarioRepository;
    private final CotizacionRepository cotizacionRepository;
    private final ContratacionRepository contratacionRepository;
    private final PolizaRepository polizaRepository;
    private final SiniestroService siniestroService;
    private final PrevisionService previsionService;

    public HistorialClienteController(
            UsuarioRepository usuarioRepository,
            CotizacionRepository cotizacionRepository,
            ContratacionRepository contratacionRepository,
            PolizaRepository polizaRepository,
            SiniestroService siniestroService,
            PrevisionService previsionService) {
        this.usuarioRepository = usuarioRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.contratacionRepository = contratacionRepository;
        this.polizaRepository = polizaRepository;
        this.siniestroService = siniestroService;
        this.previsionService = previsionService;
    }

    @GetMapping
    public ResponseEntity<?> listar(Principal principal) {

        Usuario u = usuarioRepository.findByCorreo(principal.getName())
                .orElse(null);
        if (u == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("mensaje", "No se encontró el usuario autenticado."));
        }

        List<Map<String, Object>> filas = new ArrayList<>();

        for (Cotizacion c : cotizacionRepository
                .findByUsuario_IdUsuario(u.getIdUsuario())) {
            filas.add(filaCotizacion(c));
        }
        for (Siniestro s : siniestroService.listarDelUsuario(principal.getName())) {
            filas.add(filaSiniestro(s));
        }
        for (Prevision p : previsionService.listarPorUsuario(principal.getName())) {
            filas.add(filaPrevision(p));
        }

        filas.sort(Comparator.comparing(
                (Map<String, Object> f) -> (LocalDateTime) f.get("_fecha"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        filas.forEach(f -> {
            Object fecha = f.remove("_fecha");
            f.put("fecha", fecha == null ? null : fecha.toString());
        });

        return ResponseEntity.ok(filas);
    }

    // ---------------------------------------------------------
    private Map<String, Object> filaCotizacion(Cotizacion c) {
        String estado = c.getEstado();
        String texto = null, url = null, clave = "sp_last_quote";

        boolean tienePoliza = contratacionRepository
                .findByCotizacion_IdCotizacion(c.getIdCotizacion())
                .flatMap(ct -> polizaRepository
                        .findByContratacion_IdContratacion(ct.getIdContratacion()))
                .isPresent();

        switch (estado) {
            case "Disponible" -> { texto = "Continuar con la compra"; url = "s05-poliza.html"; }
            case "Solicitada", "En revisión" -> { texto = "Ver cotización"; url = "s04-cotizacion-recibida.html"; }
            case "Aceptada" -> {
                if (tienePoliza) { texto = "Ver mi póliza"; url = "s19-mis-polizas.html"; clave = null; }
                else { texto = "Ver estado"; url = "s04-cotizacion-recibida.html"; }
            }
            default -> { texto = "Cotizar de nuevo"; url = "s03-cotizacion.html"; clave = null; }
        }

        Map<String, Object> f = base("COTIZACION", c.getIdCotizacion(),
                "COT-" + String.format("%06d", c.getIdCotizacion()),
                c.getTipoSeguro() != null ? c.getTipoSeguro().getNombre() : "Seguro",
                c.getAseguradora() != null ? c.getAseguradora().getNombre() : "",
                estado, c.getFechaSolicitud());
        accion(f, texto, url, clave, c.getIdCotizacion(),
                "Disponible".equals(estado));
        return f;
    }

    private Map<String, Object> filaSiniestro(Siniestro s) {
        String estado = s.getEstado();
        boolean falta = "Registrado".equals(estado)
                || "Información pendiente".equals(estado);

        Map<String, Object> f = base("SINIESTRO", s.getIdSiniestro(),
                s.getNumeroSiniestro() != null ? s.getNumeroSiniestro()
                        : "SIN-" + s.getIdSiniestro(),
                "Siniestro vehicular",
                s.getAseguradora() != null ? s.getAseguradora().getNombre() : "",
                estado, s.getFechaRegistro());
        accion(f, falta ? "Continuar: subir evidencias" : "Ver seguimiento",
                falta ? "s10-evidencias.html" : "s12-seguimiento.html",
                "sp_last_sin", s.getIdSiniestro(), falta);
        return f;
    }

    private Map<String, Object> filaPrevision(Prevision p) {
        String estado = p.getEstado();
        boolean continuar = "Registrada".equals(estado) || "Simulada".equals(estado);

        Map<String, Object> f = base("AFP", p.getIdPrevision(),
                "PRE-" + String.format("%06d", p.getIdPrevision()),
                "Simulación " + p.getTipoSistema(), "",
                estado, p.getFechaRegistro());
        accion(f, continuar ? "Continuar: solicitar orientación" : "Ver resultado",
                "s08-resultado-afp.html", "afp_id_prevision",
                p.getIdPrevision(), continuar);
        return f;
    }

    // ---------------------------------------------------------
    private Map<String, Object> base(String tipo, Long id, String ref,
            String titulo, String detalle, String estado, LocalDateTime fecha) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("tipo", tipo);
        f.put("id", id);
        f.put("referencia", ref);
        f.put("titulo", titulo);
        f.put("detalle", detalle);
        f.put("estado", estado);
        f.put("_fecha", fecha);
        return f;
    }

    private void accion(Map<String, Object> f, String texto, String url,
            String clave, Long valor, boolean destacada) {
        f.put("accionTexto", texto);
        f.put("accionUrl", url);
        f.put("accionClave", clave);
        f.put("accionValor", valor);
        f.put("accionDestacada", destacada);
    }
}

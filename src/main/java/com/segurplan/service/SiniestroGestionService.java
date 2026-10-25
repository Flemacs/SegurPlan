package com.segurplan.service;

import com.segurplan.dto.HistorialDTO;
import com.segurplan.model.Siniestro;
import com.segurplan.model.Usuario;
import com.segurplan.repository.SiniestroRepository;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gestión del siniestro por el asesor (RF022, RF023, RF024, RF025, RF026, RF027).
 * Toda transición se valida con {@link FlujoSiniestro}, se registra en el
 * historial y se notifica al cliente.
 */
@Service
public class SiniestroGestionService {

    private static final Set<String> DICTAMENES = Set.of("Aprobado", "Rechazado", "Observado");

    private final SiniestroRepository siniestroRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialService historialService;
    private final NotificacionService notificacionService;

    public SiniestroGestionService(
            SiniestroRepository siniestroRepository,
            UsuarioRepository usuarioRepository,
            HistorialService historialService,
            NotificacionService notificacionService) {
        this.siniestroRepository = siniestroRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialService = historialService;
        this.notificacionService = notificacionService;
    }

    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Siniestro> listar(String estado) {
        if (estado == null || estado.isBlank()) {
            return siniestroRepository.findAllByOrderByFechaRegistroDesc();
        }
        return siniestroRepository.findByEstadoOrderByFechaRegistroDesc(estado);
    }

    @Transactional(readOnly = true)
    public Siniestro obtener(Long id) {
        return buscar(id);
    }

    // ------------------------------------------------------------------
    @Transactional
    public Siniestro iniciarRevision(String correo, Long id) {
        return aplicar(actor(correo), buscar(id), "En revisión", "REVISION",
                "Revisión preliminar iniciada", "SINIESTRO_ESTADO");
    }

    @Transactional
    public Siniestro aprobarParaDerivacion(String correo, Long id, String observacion) {
        return aplicar(actor(correo), buscar(id), "Información completa", "APROBAR_DERIVACION",
                observacion, "SINIESTRO_ESTADO");
    }

    @Transactional
    public Siniestro solicitarInformacion(String correo, Long id,
                                          List<String> items, String mensaje) {
        boolean sinItems = items == null || items.isEmpty();
        boolean sinMensaje = mensaje == null || mensaje.isBlank();
        if (sinItems && sinMensaje) {
            throw new IllegalArgumentException(
                    "Indique qué información o documentos debe aportar el cliente.");
        }
        StringBuilder detalle = new StringBuilder();
        if (!sinItems) {
            detalle.append("Falta: ").append(String.join(", ", items)).append(". ");
        }
        if (!sinMensaje) {
            detalle.append(mensaje.trim());
        }
        return aplicar(actor(correo), buscar(id), "Información pendiente", "SOLICITUD_INFO",
                detalle.toString().trim(), "SOLICITUD_INFO");
    }

    @Transactional
    public Siniestro derivar(String correo, Long id) {
        Usuario actor = actor(correo);
        Siniestro s = buscar(id);
        if (s.getAseguradora() == null) {
            throw new IllegalArgumentException("El siniestro no tiene una aseguradora asociada.");
        }
        s.setFechaDerivacion(LocalDateTime.now());
        return aplicar(actor, s, "Derivado", "DERIVAR",
                "Derivado a " + s.getAseguradora().getNombre(), "SINIESTRO_ESTADO");
    }

    @Transactional
    public Siniestro registrarEvaluacion(String correo, Long id, String dictamen,
                                         BigDecimal monto, String observaciones,
                                         LocalDateTime fechaEvaluacion) {
        if (dictamen == null || !DICTAMENES.contains(dictamen)) {
            throw new IllegalArgumentException(
                    "El dictamen debe ser Aprobado, Rechazado u Observado.");
        }
        if (monto != null && monto.signum() < 0) {
            throw new IllegalArgumentException("El monto aprobado no puede ser negativo.");
        }
        if ("Aprobado".equals(dictamen) && (monto == null || monto.signum() <= 0)) {
            throw new IllegalArgumentException(
                    "Ingrese el monto aprobado por la aseguradora (mayor a 0).");
        }
        if (!"Aprobado".equals(dictamen) && (observaciones == null || observaciones.isBlank())) {
            throw new IllegalArgumentException(
                    "Indique el motivo o las observaciones del dictamen.");
        }
        Usuario actor = actor(correo);
        Siniestro s = buscar(id);
        FlujoSiniestro.validar(s.getEstado(), dictamen);

        s.setResultadoEvaluacion("Dictamen: " + dictamen
                + (observaciones == null || observaciones.isBlank() ? "" : ". " + observaciones.trim()));
        s.setMontoAprobado("Aprobado".equals(dictamen) ? monto : null);
        s.setFechaEvaluacion(fechaEvaluacion != null ? fechaEvaluacion : LocalDateTime.now());

        String detalle = "Dictamen " + dictamen
                + ("Aprobado".equals(dictamen) ? " - monto S/ " + monto : "")
                + (observaciones == null || observaciones.isBlank() ? "" : " - " + observaciones.trim());
        return aplicar(actor, s, dictamen, "EVALUACION", detalle, "SINIESTRO_ESTADO");
    }

    @Transactional
    public Siniestro cambiarEstado(String correo, Long id, String nuevoEstado, String observacion) {
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el nuevo estado.");
        }
        if ("Derivado".equals(nuevoEstado)) {
            return derivar(correo, id);
        }
        if (DICTAMENES.contains(nuevoEstado)) {
            throw new IllegalArgumentException(
                    "Use \"Registrar evaluación\" para registrar el dictamen de la aseguradora.");
        }
        return aplicar(actor(correo), buscar(id), nuevoEstado, "CAMBIO_ESTADO",
                observacion, "SINIESTRO_ESTADO");
    }

    // ------------------------------------------------------------------
    /** El cliente aporta evidencias: si estaba pendiente de información vuelve a revisión. */
    @Transactional
    public void alCargarEvidencia(Long idSiniestro, Long idUsuario) {
        Siniestro s = siniestroRepository.findById(idSiniestro).orElse(null);
        if (s == null || !"Información pendiente".equals(s.getEstado())) {
            return;
        }
        s.setEstado("En revisión");
        siniestroRepository.save(s);
        historialService.registrar(idUsuario, "SINIESTRO", s.getIdSiniestro(),
                "INFO_APORTADA", "Estado: Información pendiente",
                "Estado: En revisión - el cliente aportó nueva información");
        notificacionService.notificarRol("Asesor", "INFO_APORTADA",
                "Información recibida: " + s.getNumeroSiniestro(),
                "El cliente cargó nueva información. El caso volvió a revisión.",
                "SINIESTRO", s.getIdSiniestro());
    }

    /** Aviso a los asesores cuando se registra un siniestro nuevo. */
    public void alRegistrar(Siniestro s) {
        notificacionService.notificarRol("Asesor", "SINIESTRO_NUEVO",
                "Nuevo siniestro " + s.getNumeroSiniestro(),
                "Hay un siniestro pendiente de revisión preliminar.",
                "SINIESTRO", s.getIdSiniestro());
        if (s.getUsuario() != null) {
            notificacionService.notificar(s.getUsuario().getIdUsuario(), "SINIESTRO_REGISTRADO",
                    "Siniestro registrado: " + s.getNumeroSiniestro(),
                    FlujoSiniestro.descripcion("Registrado"), "SINIESTRO", s.getIdSiniestro());
        }
    }

    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public Map<String, Object> lineaDeTiempo(Long id) {
        Siniestro s = buscar(id);
        List<Map<String, Object>> eventos = new ArrayList<>();
        for (HistorialDTO h : historialService.listarPorRegistro("SINIESTRO", id)) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("fecha", h.getFechaHora());
            e.put("accion", h.getAccion());
            e.put("usuario", h.getUsuario());
            e.put("anterior", h.getValorAnterior());
            e.put("detalle", h.getValorNuevo());
            eventos.add(e);
        }
        List<Map<String, String>> proximos = new ArrayList<>();
        for (String sig : FlujoSiniestro.siguientes(s.getEstado())) {
            proximos.add(Map.of("estado", sig, "descripcion", FlujoSiniestro.descripcion(sig)));
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("idSiniestro", s.getIdSiniestro());
        r.put("numeroSiniestro", s.getNumeroSiniestro());
        r.put("estado", s.getEstado());
        r.put("descripcionEstado", FlujoSiniestro.descripcion(s.getEstado()));
        r.put("montoAprobado", s.getMontoAprobado());
        r.put("resultadoEvaluacion", s.getResultadoEvaluacion());
        r.put("eventos", eventos);
        r.put("proximosPasos", proximos);
        return r;
    }

    // ------------------------------------------------------------------
    private Siniestro aplicar(Usuario actor, Siniestro s, String nuevo, String accion,
                              String detalle, String tipoNotificacion) {
        String anterior = s.getEstado();
        FlujoSiniestro.validar(anterior, nuevo);
        s.setEstado(nuevo);
        Siniestro guardado = siniestroRepository.save(s);

        String texto = "Estado: " + nuevo
                + (detalle == null || detalle.isBlank() ? "" : " - " + detalle.trim());
        historialService.registrar(actor.getIdUsuario(), "SINIESTRO", guardado.getIdSiniestro(),
                accion, "Estado: " + anterior, texto);

        String msg = FlujoSiniestro.descripcion(nuevo)
                + (detalle == null || detalle.isBlank() ? "" : " " + detalle.trim());
        notificacionService.notificar(guardado.getUsuario().getIdUsuario(), tipoNotificacion,
                "Siniestro " + guardado.getNumeroSiniestro() + ": " + nuevo,
                msg, "SINIESTRO", guardado.getIdSiniestro());
        return guardado;
    }

    private Siniestro buscar(Long id) {
        return siniestroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El siniestro indicado no existe."));
    }

    private Usuario actor(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario autenticado."));
    }
}

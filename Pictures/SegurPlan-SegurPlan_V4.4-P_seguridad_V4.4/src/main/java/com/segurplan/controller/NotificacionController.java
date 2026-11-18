package com.segurplan.controller;

import com.segurplan.model.Notificacion;
import com.segurplan.service.NotificacionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Notificaciones internas del usuario autenticado (RF032). */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<?> listar(Principal principal) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Notificacion n : notificacionService.listar(principal.getName())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getIdNotificacion());
            m.put("tipo", n.getTipo());
            m.put("titulo", n.getTitulo());
            m.put("mensaje", n.getMensaje());
            m.put("entidad", n.getEntidad());
            m.put("idRegistro", n.getIdRegistro());
            m.put("leida", n.isLeida());
            m.put("fecha", n.getFecha());
            items.add(m);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("noLeidas", notificacionService.contarNoLeidas(principal.getName()));
        r.put("items", items);
        return ResponseEntity.ok(r);
    }

    @PutMapping("/{id}/leida")
    public ResponseEntity<?> marcarLeida(@PathVariable Long id, Principal principal) {
        notificacionService.marcarLeida(principal.getName(), id);
        return ResponseEntity.ok(Map.of("mensaje", "Notificación marcada como leída."));
    }

    @PutMapping("/leidas")
    public ResponseEntity<?> marcarTodas(Principal principal) {
        int n = notificacionService.marcarTodasLeidas(principal.getName());
        return ResponseEntity.ok(Map.of("mensaje", "Notificaciones marcadas como leídas.", "cantidad", n));
    }
}

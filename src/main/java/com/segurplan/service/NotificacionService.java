package com.segurplan.service;

import com.segurplan.model.Notificacion;
import com.segurplan.model.Usuario;
import com.segurplan.repository.NotificacionRepository;
import com.segurplan.repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Notificaciones internas (RF032). Un fallo al notificar nunca debe
 * interrumpir la operación de negocio, por eso se captura y se registra.
 */
@Service
public class NotificacionService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(
            NotificacionRepository notificacionRepository,
            UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void notificar(Long idUsuario, String tipo, String titulo,
                          String mensaje, String entidad, Long idRegistro) {
        try {
            if (idUsuario == null) {
                return;
            }
            Optional<Usuario> usuario = usuarioRepository.findById(idUsuario);
            if (usuario.isEmpty()) {
                return;
            }
            Notificacion n = new Notificacion();
            n.setUsuario(usuario.get());
            n.setTipo(tipo);
            n.setTitulo(recortar(titulo, 150));
            n.setMensaje(mensaje);
            n.setEntidad(entidad);
            n.setIdRegistro(idRegistro);
            notificacionRepository.save(n);
        } catch (RuntimeException e) {
            log.warn("No se pudo crear la notificación ({}): {}", tipo, e.getMessage());
        }
    }

    /** Notifica a todos los usuarios activos de un rol (por ejemplo, los asesores). */
    @Transactional
    public void notificarRol(String rol, String tipo, String titulo,
                             String mensaje, String entidad, Long idRegistro) {
        try {
            for (Usuario u : usuarioRepository.findByRol_NombreOrderByIdUsuarioAsc(rol)) {
                if ("Activo".equalsIgnoreCase(u.getEstado())) {
                    notificar(u.getIdUsuario(), tipo, titulo, mensaje, entidad, idRegistro);
                }
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo notificar al rol {}: {}", rol, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listar(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .map(u -> notificacionRepository
                        .findTop30ByUsuario_IdUsuarioOrderByFechaDesc(u.getIdUsuario()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .map(u -> notificacionRepository
                        .countByUsuario_IdUsuarioAndLeidaFalse(u.getIdUsuario()))
                .orElse(0L);
    }

    @Transactional
    public void marcarLeida(String correo, Long idNotificacion) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        Notificacion n = notificacionRepository.findById(idNotificacion)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada."));
        if (!n.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new SecurityException("No puede modificar esta notificación.");
        }
        n.setLeida(true);
        notificacionRepository.save(n);
    }

    @Transactional
    public int marcarTodasLeidas(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .map(u -> notificacionRepository.marcarTodasLeidas(u.getIdUsuario()))
                .orElse(0);
    }

    private String recortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}

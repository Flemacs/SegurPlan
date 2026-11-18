package com.segurplan.service;

import com.segurplan.model.Usuario;
import com.segurplan.repository.UsuarioRepository;
import com.segurplan.util.PasswordPolicy;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Perfil del usuario autenticado (RF003). */
@Service
public class PerfilService {

    private static final Pattern CORREO =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final HistorialService historialService;
    private final NotificacionService notificacionService;

    public PerfilService(UsuarioRepository usuarioRepository,
                         PasswordEncoder passwordEncoder,
                         HistorialService historialService,
                         NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.historialService = historialService;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtener(String correo) {
        return mapa(buscar(correo));
    }

    @Transactional
    public Map<String, Object> actualizar(String correoActual, String nombres, String apellidos,
                                          String correoNuevo, String telefono, String direccion) {
        Usuario u = buscar(correoActual);
        nombres = limpiar(nombres);
        apellidos = limpiar(apellidos);
        correoNuevo = limpiar(correoNuevo).toLowerCase();
        telefono = limpiar(telefono);
        direccion = limpiar(direccion);

        if (nombres.isEmpty() || apellidos.isEmpty()) {
            throw new IllegalArgumentException("Nombres y apellidos son obligatorios.");
        }
        if (!CORREO.matcher(correoNuevo).matches()) {
            throw new IllegalArgumentException("Ingrese un correo electrónico válido.");
        }
        if (!telefono.isEmpty() && !telefono.matches("^[0-9+ ]{6,20}$")) {
            throw new IllegalArgumentException("El teléfono solo puede tener números (6 a 20 dígitos).");
        }
        if (direccion.length() > 200) {
            throw new IllegalArgumentException("La dirección no puede superar los 200 caracteres.");
        }
        boolean correoCambiado = !correoNuevo.equalsIgnoreCase(u.getCorreo());
        if (correoCambiado && usuarioRepository.existsByCorreo(correoNuevo)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
        }

        String antes = u.getNombres() + " " + u.getApellidos() + " / " + u.getCorreo()
                + " / " + u.getTelefono();
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setCorreo(correoNuevo);
        u.setTelefono(telefono.isEmpty() ? null : telefono);
        u.setDireccion(direccion.isEmpty() ? null : direccion);
        u.setFechaModificacion(LocalDateTime.now());
        usuarioRepository.save(u);

        historialService.registrar(u.getIdUsuario(), "USUARIO", u.getIdUsuario(),
                "ACTUALIZAR_PERFIL", antes,
                nombres + " " + apellidos + " / " + correoNuevo + " / " + telefono);

        Map<String, Object> r = mapa(u);
        r.put("correoCambiado", correoCambiado);
        return r;
    }

    @Transactional
    public void cambiarPassword(String correo, String actual, String nueva) {
        Usuario u = buscar(correo);
        if (actual == null || !passwordEncoder.matches(actual, u.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        PasswordPolicy.validar(nueva);
        if (passwordEncoder.matches(nueva, u.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta de la actual.");
        }
        u.setPassword(passwordEncoder.encode(nueva));
        u.setFechaModificacion(LocalDateTime.now());
        usuarioRepository.save(u);
        historialService.registrar(u.getIdUsuario(), "USUARIO", u.getIdUsuario(),
                "CAMBIAR_PASSWORD", null, "Contraseña actualizada");
        notificacionService.notificar(u.getIdUsuario(), "SEGURIDAD",
                "Contraseña actualizada",
                "Se cambió la contraseña de su cuenta. Si no fue usted, contacte a un administrador.",
                "USUARIO", u.getIdUsuario());
    }

    private Map<String, Object> mapa(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idUsuario", u.getIdUsuario());
        m.put("nombres", u.getNombres());
        m.put("apellidos", u.getApellidos());
        m.put("correo", u.getCorreo());
        m.put("telefono", u.getTelefono());
        m.put("direccion", u.getDireccion());
        m.put("rol", u.getRol() == null ? null : u.getRol().getNombre());
        m.put("areaEspecialidad", u.getAreaEspecialidad());
        m.put("fechaRegistro", u.getFechaRegistro());
        m.put("fechaModificacion", u.getFechaModificacion());
        return m;
    }

    private Usuario buscar(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }

    private String limpiar(String s) {
        return s == null ? "" : s.trim();
    }
}

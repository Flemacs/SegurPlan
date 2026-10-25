package com.segurplan.service;

import com.segurplan.repository.UsuarioRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Comprobaciones de propiedad y rol para evitar acceso a datos ajenos. */
@Component
public class AccesoService {

    private final UsuarioRepository usuarioRepository;

    public AccesoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public boolean esPersonal(Authentication auth) {
        return tieneRol(auth, "Asesor")
                || tieneRol(auth, "Administrador");
    }

    public boolean esAsesorOAdmin(Authentication auth) {
        return tieneRol(auth, "Asesor") || tieneRol(auth, "Administrador");
    }

    public boolean tieneRol(Authentication auth, String rol) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + rol).equalsIgnoreCase(a.getAuthority()));
    }

    public Long idUsuario(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return usuarioRepository.findByCorreo(auth.getName())
                .map(u -> u.getIdUsuario())
                .orElse(null);
    }

    /** El dueño del registro o el personal autorizado pueden verlo. */
    public boolean puedeVer(Authentication auth, Long idDuenio) {
        if (auth == null) {
            return false;
        }
        if (esPersonal(auth)) {
            return true;
        }
        Long propio = idUsuario(auth);
        return propio != null && propio.equals(idDuenio);
    }
}

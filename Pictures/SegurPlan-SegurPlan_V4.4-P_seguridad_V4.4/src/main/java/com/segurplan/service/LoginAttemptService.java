package com.segurplan.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

/**
 * Protección contra fuerza bruta en el inicio de sesión (RNF04).
 *
 * Cada clave (correo o dirección IP) acumula intentos fallidos. Al llegar al
 * máximo, la clave queda bloqueada durante un tiempo; un inicio de sesión
 * correcto reinicia el contador del correo. El estado vive en memoria: se
 * reinicia al reiniciar la aplicación y no se comparte entre instancias.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_INTENTOS_USUARIO = 5;
    public static final int MAX_INTENTOS_IP = 20;
    public static final long BLOQUEO_MS = 15L * 60L * 1000L;

    private static final int LIMITE_PARA_PURGAR = 10_000;

    private static final class Registro {
        int fallos;
        long bloqueadoHasta;
        long ultimoFallo;
    }

    private final ConcurrentMap<String, Registro> registros = new ConcurrentHashMap<>();

    /** Reloj sobrescribible en pruebas. */
    protected long ahora() {
        return System.currentTimeMillis();
    }

    private static int maximoPara(String clave) {
        return clave != null && clave.startsWith("ip:")
                ? MAX_INTENTOS_IP
                : MAX_INTENTOS_USUARIO;
    }

    public boolean estaBloqueado(String clave) {
        Registro r = registros.get(clave);
        if (r == null) {
            return false;
        }
        synchronized (r) {
            if (r.bloqueadoHasta > ahora()) {
                return true;
            }
            if (r.bloqueadoHasta != 0) {
                // el bloqueo ya venció: se empieza de nuevo
                r.bloqueadoHasta = 0;
                r.fallos = 0;
            }
            return false;
        }
    }

    public void registrarFallo(String clave) {
        if (registros.size() > LIMITE_PARA_PURGAR) {
            purgarVencidos();
        }
        Registro r = registros.computeIfAbsent(clave, k -> new Registro());
        synchronized (r) {
            long t = ahora();
            r.ultimoFallo = t;
            r.fallos++;
            if (r.fallos >= maximoPara(clave)) {
                r.bloqueadoHasta = t + BLOQUEO_MS;
            }
        }
    }

    public void registrarExito(String clave) {
        registros.remove(clave);
    }

    /** Minutos (redondeados hacia arriba) que faltan para el desbloqueo. */
    public long minutosRestantes(String clave) {
        Registro r = registros.get(clave);
        if (r == null) {
            return 0;
        }
        synchronized (r) {
            long falta = r.bloqueadoHasta - ahora();
            return falta <= 0 ? 0 : (falta + 59_999L) / 60_000L;
        }
    }

    private void purgarVencidos() {
        long t = ahora();
        registros.entrySet().removeIf(e -> {
            Registro r = e.getValue();
            synchronized (r) {
                boolean bloqueoVigente = r.bloqueadoHasta > t;
                return !bloqueoVigente && (t - r.ultimoFallo) > BLOQUEO_MS;
            }
        });
    }
}

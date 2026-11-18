package com.segurplan.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del bloqueo temporal por intentos fallidos de inicio de sesión.
 */
class LoginAttemptServiceTest {

    /** Servicio con reloj controlable para no esperar 15 minutos reales. */
    private static class ServicioConReloj extends LoginAttemptService {
        long ahora = 1_000_000L;

        @Override
        protected long ahora() {
            return ahora;
        }
    }

    private ServicioConReloj servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioConReloj();
    }

    @Test
    void usuarioNuevoNoEstaBloqueado() {
        assertFalse(servicio.estaBloqueado("u:ana@correo.com"));
        assertEquals(0, servicio.minutosRestantes("u:ana@correo.com"));
    }

    @Test
    void cuatroFallosNoBloquean() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS_USUARIO - 1; i++) {
            servicio.registrarFallo("u:ana@correo.com");
        }
        assertFalse(servicio.estaBloqueado("u:ana@correo.com"));
    }

    @Test
    void elQuintoFalloBloqueaPorQuinceMinutos() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS_USUARIO; i++) {
            servicio.registrarFallo("u:ana@correo.com");
        }
        assertTrue(servicio.estaBloqueado("u:ana@correo.com"));
        assertEquals(15, servicio.minutosRestantes("u:ana@correo.com"));
    }

    @Test
    void elBloqueoSeLevantaCuandoVence() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS_USUARIO; i++) {
            servicio.registrarFallo("u:ana@correo.com");
        }
        servicio.ahora += LoginAttemptService.BLOQUEO_MS + 1;

        assertFalse(servicio.estaBloqueado("u:ana@correo.com"));

        // y vuelve a empezar de cero: un fallo más no bloquea
        servicio.registrarFallo("u:ana@correo.com");
        assertFalse(servicio.estaBloqueado("u:ana@correo.com"));
    }

    @Test
    void unInicioCorrectoReiniciaElContador() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS_USUARIO - 1; i++) {
            servicio.registrarFallo("u:ana@correo.com");
        }
        servicio.registrarExito("u:ana@correo.com");

        servicio.registrarFallo("u:ana@correo.com");
        assertFalse(servicio.estaBloqueado("u:ana@correo.com"));
    }

    @Test
    void loCuentaPorSeparadoParaCadaClave() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS_USUARIO; i++) {
            servicio.registrarFallo("u:ana@correo.com");
        }
        assertTrue(servicio.estaBloqueado("u:ana@correo.com"));
        assertFalse(servicio.estaBloqueado("u:luis@correo.com"));
    }

    @Test
    void laIpTieneUnLimiteMasAltoQueElUsuario() {
        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS_USUARIO; i++) {
            servicio.registrarFallo("ip:10.0.0.1");
        }
        assertFalse(servicio.estaBloqueado("ip:10.0.0.1"));

        for (int i = LoginAttemptService.MAX_INTENTOS_USUARIO;
                i < LoginAttemptService.MAX_INTENTOS_IP; i++) {
            servicio.registrarFallo("ip:10.0.0.1");
        }
        assertTrue(servicio.estaBloqueado("ip:10.0.0.1"));
    }
}

package com.segurplan.security;

import static org.junit.jupiter.api.Assertions.*;
import com.segurplan.config.ApiAccessDeniedHandler;
import com.segurplan.config.ApiAuthenticationEntryPoint;
import com.segurplan.config.SecurityConfig;
import com.segurplan.service.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class SeguridadRegresionTest {
    private final PasswordEncoder encoder = new SecurityConfig(null, null, false).passwordEncoder();

    @Test void contrasenasSeAlmacenanComoHashBCryptConSalDistinta() {
        String password = "ClaveSegura!2026";
        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);
        assertNotEquals(password, hash1);
        assertNotEquals(hash1, hash2);
        assertTrue(hash1.startsWith("$2"));
        assertTrue(encoder.matches(password, hash1));
        assertFalse(encoder.matches("incorrecta", hash1));
    }

    @Test void ataquesDeFuerzaBrutaPorUsuarioQuedanBloqueados() {
        LoginAttemptService intentos = new LoginAttemptService();
        for (int i=0; i<LoginAttemptService.MAX_INTENTOS_USUARIO; i++) intentos.registrarFallo("u:prueba@localhost");
        assertTrue(intentos.estaBloqueado("u:prueba@localhost"));
        assertTrue(intentos.minutosRestantes("u:prueba@localhost") > 0);
        intentos.registrarExito("u:prueba@localhost");
        assertFalse(intentos.estaBloqueado("u:prueba@localhost"));
    }

    @Test void limitesPorIpSonIndependientes() {
        LoginAttemptService intentos = new LoginAttemptService();
        for (int i=0; i<LoginAttemptService.MAX_INTENTOS_IP; i++) intentos.registrarFallo("ip:127.0.0.1");
        assertTrue(intentos.estaBloqueado("ip:127.0.0.1"));
        assertFalse(intentos.estaBloqueado("ip:127.0.0.2"));
    }
}

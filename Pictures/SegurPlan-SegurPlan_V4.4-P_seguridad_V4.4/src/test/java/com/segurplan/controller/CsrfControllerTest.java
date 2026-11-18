
package com.segurplan.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CsrfToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CsrfControllerTest {

    @Test
    void obtenerTokenDebeRetornarElMismoToken() {
        CsrfController controller = new CsrfController();

        CsrfToken token = mock(CsrfToken.class);

        CsrfToken resultado = controller.obtenerToken(token);

        assertNotNull(resultado);
        assertSame(token, resultado);
    }
}


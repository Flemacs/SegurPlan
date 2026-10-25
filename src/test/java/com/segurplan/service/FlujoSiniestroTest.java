package com.segurplan.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlujoSiniestroTest {

    @Test
    void flujoNormalCompleto_esValido() {
        String[] ruta = {"Registrado", "En revisión", "Información completa", "Derivado",
                "En evaluación", "Aprobado", "Cerrado"};
        for (int i = 0; i < ruta.length - 1; i++) {
            assertTrue(FlujoSiniestro.permitida(ruta[i], ruta[i + 1]),
                    ruta[i] + " -> " + ruta[i + 1]);
        }
    }

    @Test
    void registradoNoPuedeSaltarACerrado() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> FlujoSiniestro.validar("Registrado", "Cerrado"));
        assertTrue(ex.getMessage().contains("Transición no permitida"));
    }

    @Test
    void registradoNoPuedeDerivarseSinRevision() {
        assertFalse(FlujoSiniestro.permitida("Registrado", "Derivado"));
    }

    @Test
    void cerradoEsEstadoFinal() {
        assertTrue(FlujoSiniestro.siguientes("Cerrado").isEmpty());
        assertTrue(FlujoSiniestro.esFinal("Cerrado"));
    }

    @Test
    void informacionPendienteVuelveARevision() {
        assertTrue(FlujoSiniestro.permitida("Información pendiente", "En revisión"));
    }

    @Test
    void estadoDesconocidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> FlujoSiniestro.validar("Registrado", "Inventado"));
        assertThrows(IllegalArgumentException.class,
                () -> FlujoSiniestro.validar("Otro", "Cerrado"));
    }

    @Test
    void todosLosEstadosTienenDescripcion() {
        for (String e : new String[]{"Registrado", "En revisión", "Información pendiente",
                "Información completa", "Derivado", "En evaluación", "Evaluación recibida",
                "Observado", "Aprobado", "Rechazado", "Cerrado"}) {
            assertFalse(FlujoSiniestro.descripcion(e).isBlank(), e);
        }
        assertEquals("", FlujoSiniestro.descripcion("X"));
    }
}

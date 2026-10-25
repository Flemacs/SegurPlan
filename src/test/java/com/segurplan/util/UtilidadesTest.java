package com.segurplan.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilidadesTest {

    @Test
    void passwordCorta_seRechaza() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validar("Ab1"));
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validar(null));
    }

    @Test
    void passwordSoloLetrasOSoloNumeros_seRechaza() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validar("sololetras"));
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validar("12345678"));
    }

    @Test
    void passwordValida_seAcepta() {
        assertDoesNotThrow(() -> PasswordPolicy.validar("Segura2026"));
    }

    @Test
    void pdf_generaDocumentoValidoYPagina() {
        SimplePdf pdf = new SimplePdf().titulo("Prueba").campo("Cliente", "José Ñandú");
        for (int i = 0; i < 120; i++) {
            pdf.parrafo("Línea " + i + " con (paréntesis) y acentos áéíóú");
        }
        byte[] bytes = pdf.generar("SEGURPLAN");
        String texto = new String(bytes, StandardCharsets.ISO_8859_1);
        assertTrue(texto.startsWith("%PDF-1.4"));
        assertTrue(texto.contains("%%EOF"));
        assertTrue(texto.contains("/Count 3") || texto.contains("/Count 2"));
    }
}

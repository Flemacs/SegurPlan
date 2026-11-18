package com.segurplan.service;

import java.util.List;
import java.util.Map;

/**
 * Máquina de estados del siniestro (RNF11): impide saltos ilógicos,
 * por ejemplo pasar de "Registrado" a "Cerrado" sin derivar.
 */
public final class FlujoSiniestro {

    private FlujoSiniestro() {
    }

    private static final Map<String, List<String>> TRANSICIONES = Map.ofEntries(
            Map.entry("Registrado", List.of("En revisión")),
            Map.entry("En revisión", List.of("Información pendiente", "Información completa")),
            Map.entry("Información pendiente", List.of("En revisión", "Información completa")),
            Map.entry("Información completa", List.of("En revisión", "Derivado")),
            Map.entry("Derivado", List.of("En evaluación")),
            Map.entry("En evaluación", List.of("Evaluación recibida", "Observado", "Aprobado", "Rechazado")),
            Map.entry("Evaluación recibida", List.of("Observado", "Aprobado", "Rechazado")),
            Map.entry("Observado", List.of("En evaluación", "Aprobado", "Rechazado", "Cerrado")),
            Map.entry("Aprobado", List.of("Cerrado")),
            Map.entry("Rechazado", List.of("Cerrado")),
            Map.entry("Cerrado", List.of())
    );

    private static final Map<String, String> DESCRIPCION = Map.ofEntries(
            Map.entry("Registrado", "El asesor iniciará la revisión preliminar de su expediente."),
            Map.entry("En revisión", "El asesor revisa los datos y las evidencias."),
            Map.entry("Información pendiente", "Debe cargar la información adicional solicitada."),
            Map.entry("Información completa", "Expediente validado; será derivado a la aseguradora."),
            Map.entry("Derivado", "Su caso fue enviado a la aseguradora; esperando evaluación."),
            Map.entry("En evaluación", "La aseguradora evalúa el siniestro."),
            Map.entry("Evaluación recibida", "Se recibió la evaluación; el asesor registrará el dictamen."),
            Map.entry("Observado", "La aseguradora hizo observaciones al caso."),
            Map.entry("Aprobado", "La aseguradora aprobó el siniestro."),
            Map.entry("Rechazado", "La aseguradora rechazó el siniestro."),
            Map.entry("Cerrado", "El caso fue cerrado.")
    );

    public static List<String> siguientes(String estado) {
        return TRANSICIONES.getOrDefault(estado == null ? "" : estado, List.of());
    }

    public static boolean permitida(String actual, String nuevo) {
        return siguientes(actual).contains(nuevo);
    }

    public static void validar(String actual, String nuevo) {
        if (!TRANSICIONES.containsKey(actual)) {
            throw new IllegalArgumentException("Estado actual desconocido: " + actual);
        }
        if (!TRANSICIONES.containsKey(nuevo)) {
            throw new IllegalArgumentException("Estado no válido: " + nuevo);
        }
        if (!permitida(actual, nuevo)) {
            throw new IllegalArgumentException(
                    "Transición no permitida: de \"" + actual + "\" a \"" + nuevo + "\". "
                    + "Estados posibles: " + siguientes(actual));
        }
    }

    public static String descripcion(String estado) {
        return DESCRIPCION.getOrDefault(estado, "");
    }

    public static boolean esFinal(String estado) {
        return "Cerrado".equals(estado);
    }
}

package com.segurplan.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Generador mínimo de PDF (sin dependencias externas): texto con
 * Helvetica, saltos de línea automáticos y paginación.
 */
public final class SimplePdf {

    private static final Charset CP1252 = Charset.forName("windows-1252");
    private static final float ANCHO = 595f;
    private static final float ALTO = 842f;
    private static final float MARGEN = 50f;

    private final List<ByteArrayOutputStream> paginas = new ArrayList<>();
    private ByteArrayOutputStream actual;
    private float y;

    public SimplePdf() {
        nuevaPagina();
    }

    private void nuevaPagina() {
        actual = new ByteArrayOutputStream();
        paginas.add(actual);
        y = ALTO - MARGEN;
    }

    public SimplePdf titulo(String texto) {
        escribir(texto, "F2", 17f, 24f, 0f);
        y -= 4;
        return this;
    }

    public SimplePdf subtitulo(String texto) {
        escribir(texto, "F2", 12f, 18f, 0f);
        return this;
    }

    public SimplePdf parrafo(String texto) {
        escribir(texto, "F1", 10f, 14f, 0f);
        return this;
    }

    /** Línea "Etiqueta: valor". */
    public SimplePdf campo(String etiqueta, Object valor) {
        String v = valor == null || String.valueOf(valor).isBlank() ? "-" : String.valueOf(valor);
        escribir(etiqueta + ": " + v, "F1", 10f, 14f, 0f);
        return this;
    }

    public SimplePdf espacio() {
        y -= 10;
        return this;
    }

    private void escribir(String texto, String fuente, float tam, float interlineado, float sangria) {
        if (texto == null) {
            texto = "";
        }
        float factor = "F2".equals(fuente) ? 0.58f : 0.54f;
        int maxCaracteres = Math.max(20, (int) ((ANCHO - 2 * MARGEN - sangria) / (tam * factor)));
        for (String parrafo : texto.replace("\r", "").split("\n", -1)) {
            for (String linea : envolver(parrafo, maxCaracteres)) {
                if (y < MARGEN + 20) {
                    nuevaPagina();
                }
                String cmd = "BT /" + fuente + " " + tam + " Tf " + (MARGEN + sangria) + " " + y
                        + " Td (" + escapar(linea) + ") Tj ET\n";
                actual.writeBytes(cmd.getBytes(CP1252));
                y -= interlineado;
            }
        }
    }

    private static List<String> envolver(String texto, int max) {
        List<String> lineas = new ArrayList<>();
        if (texto.isEmpty()) {
            lineas.add("");
            return lineas;
        }
        StringBuilder sb = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            while (palabra.length() > max) {
                if (sb.length() > 0) {
                    lineas.add(sb.toString());
                    sb.setLength(0);
                }
                lineas.add(palabra.substring(0, max));
                palabra = palabra.substring(max);
            }
            if (sb.length() + palabra.length() + (sb.length() > 0 ? 1 : 0) > max) {
                lineas.add(sb.toString());
                sb.setLength(0);
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(palabra);
        }
        if (sb.length() > 0) {
            lineas.add(sb.toString());
        }
        return lineas;
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    public byte[] generar(String pie) {
        int total = paginas.size();
        for (int i = 0; i < total; i++) {
            String f = "BT /F1 8 Tf " + MARGEN + " 30 Td (" + escapar(pie + "  -  Pagina " + (i + 1) + " de " + total)
                    + ") Tj ET\n";
            paginas.get(i).writeBytes(f.getBytes(CP1252));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        out.writeBytes("%PDF-1.4\n".getBytes(CP1252));

        // objetos: 1 catálogo, 2 páginas, 3 F1, 4 F2, luego (página, contenido) por página
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < total; i++) {
            kids.append(5 + i * 2).append(" 0 R ");
        }
        objeto(out, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
        objeto(out, offsets, "<< /Type /Pages /Kids [ " + kids + "] /Count " + total + " >>");
        objeto(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objeto(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        for (int i = 0; i < total; i++) {
            int idContenido = 6 + i * 2;
            objeto(out, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + ANCHO + " " + ALTO
                    + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + idContenido + " 0 R >>");
            byte[] datos = paginas.get(i).toByteArray();
            offsets.add(out.size());
            out.writeBytes(((offsets.size()) + " 0 obj\n<< /Length " + datos.length + " >>\nstream\n").getBytes(CP1252));
            out.writeBytes(datos);
            out.writeBytes("endstream\nendobj\n".getBytes(CP1252));
        }
        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for (int off : offsets) {
            x.append(String.format("%010d 00000 n \n", off));
        }
        x.append("trailer\n<< /Size ").append(offsets.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        out.writeBytes(x.toString().getBytes(CP1252));
        return out.toByteArray();
    }

    private static void objeto(ByteArrayOutputStream out, List<Integer> offsets, String cuerpo) {
        offsets.add(out.size());
        out.writeBytes(((offsets.size()) + " 0 obj\n" + cuerpo + "\nendobj\n").getBytes(CP1252));
    }
}

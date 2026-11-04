package com.segurplan.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class JasperReportService {

    private static final Map<String, String> REPORTS = Map.ofEntries(
            Map.entry("REP-DG-01", "rep-dg-01-general.jrxml"),
            Map.entry("REP-DG-02", "rep-dg-02-indicadores.jrxml"),
            Map.entry("REP-LA-01", "rep-la-01-documentos.jrxml"),
            Map.entry("REP-MV-01", "rep-mv-01-cotizaciones-estado.jrxml"),
            Map.entry("REP-MV-02", "rep-mv-02-conversion-ventas.jrxml"),
            Map.entry("REP-MV-05", "rep-mv-05-cartera-polizas.jrxml"),
            Map.entry("REP-OT-02", "rep-ot-02-trazabilidad.jrxml"),
            Map.entry("REP-PO-01", "rep-po-01-afp.jrxml"),
            Map.entry("REP-PO-02", "rep-po-02-siniestros-estado.jrxml"),
            Map.entry("REP-PO-03", "rep-po-03-siniestros-aseguradora.jrxml")
    );

    private final DataSource dataSource;
    private final ConcurrentMap<String, JasperReport> compiledReports = new ConcurrentHashMap<>();

    public JasperReportService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public byte[] generarPdf(String codigo) {
        String normalizedCode = codigo == null ? "" : codigo.toUpperCase();
        if (!REPORTS.containsKey(normalizedCode)) {
            throw new IllegalArgumentException("Código de reporte no válido.");
        }

        JasperReport report = compiledReports.computeIfAbsent(
                normalizedCode,
                this::compilarReporte
        );

        try (Connection connection = dataSource.getConnection()) {
            // Debe ser un Map MUTABLE: Jasper agrega REPORT_CONNECTION, etc.
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("REPORT_LOCALE", new java.util.Locale("es", "PE"));
            JasperPrint filledReport = JasperFillManager.fillReport(
                    report,
                    parametros,
                    connection
            );
            return JasperExportManager.exportReportToPdf(filledReport);
        } catch (SQLException | JRException e) {
            throw new IllegalStateException(
                    "No se pudo generar el PDF " + normalizedCode + ".",
                    e
            );
        }
    }

    private JasperReport compilarReporte(String codigo) {
        String fileName = REPORTS.get(codigo);
        ClassPathResource resource = new ClassPathResource("reports/" + fileName);

        try (InputStream input = resource.getInputStream()) {
            return JasperCompileManager.compileReport(input);
        } catch (IOException | JRException e) {
            throw new IllegalStateException(
                    "No se pudo cargar la plantilla " + fileName + ".",
                    e
            );
        }
    }
}

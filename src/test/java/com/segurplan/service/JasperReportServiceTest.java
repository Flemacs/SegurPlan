
package com.segurplan.service;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JasperReportServiceTest {

    @Test
    void generarPdf_codigoInvalido_lanzaExcepcion() {

        DataSource dataSource = mock(DataSource.class);
        JasperReportService service =
                new JasperReportService(dataSource);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generarPdf("CODIGO-INVALIDO")
        );

        assertEquals(
                "Código de reporte no válido.",
                exception.getMessage()
        );

        verifyNoInteractions(dataSource);
    }

    @Test
    void generarPdf_codigoNulo_lanzaExcepcion() {

        DataSource dataSource = mock(DataSource.class);
        JasperReportService service =
                new JasperReportService(dataSource);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.generarPdf(null)
        );

        verifyNoInteractions(dataSource);
    }

    @Test
    void generarPdf_codigoValido_generaArchivoPdf() throws Exception {

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);

        JasperReport report = mock(JasperReport.class);
        JasperPrint print = mock(JasperPrint.class);

        byte[] pdfEsperado = {37, 80, 68, 70};

        when(dataSource.getConnection()).thenReturn(connection);

        try (
            MockedStatic<JasperCompileManager> compiler =
                    mockStatic(JasperCompileManager.class);

            MockedStatic<JasperFillManager> filler =
                    mockStatic(JasperFillManager.class);

            MockedStatic<JasperExportManager> exporter =
                    mockStatic(JasperExportManager.class)
        ) {

            compiler.when(() ->
                    JasperCompileManager.compileReport(
                            any(InputStream.class)
                    )
            ).thenReturn(report);

            filler.when(() ->
                    JasperFillManager.fillReport(
                            eq(report),
                            anyMap(),
                            eq(connection)
                    )
            ).thenReturn(print);

            exporter.when(() ->
                    JasperExportManager.exportReportToPdf(print)
            ).thenReturn(pdfEsperado);

            JasperReportService service =
                    new JasperReportService(dataSource);

            byte[] resultado = service.generarPdf("rep-dg-01");

            assertArrayEquals(pdfEsperado, resultado);

            compiler.verify(() ->
                    JasperCompileManager.compileReport(
                            any(InputStream.class)
                    ), times(1)
            );

            filler.verify(() ->
                    JasperFillManager.fillReport(
                            eq(report),
                            anyMap(),
                            eq(connection)
                    ), times(1)
            );

            exporter.verify(() ->
                    JasperExportManager.exportReportToPdf(print),
                    times(1)
            );

            verify(dataSource).getConnection();
        }
    }

    @Test
    void generarPdf_reporteCompilado_reutilizaCache() throws Exception {

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);

        JasperReport report = mock(JasperReport.class);
        JasperPrint print = mock(JasperPrint.class);

        when(dataSource.getConnection()).thenReturn(connection);

        try (
            MockedStatic<JasperCompileManager> compiler =
                    mockStatic(JasperCompileManager.class);

            MockedStatic<JasperFillManager> filler =
                    mockStatic(JasperFillManager.class);

            MockedStatic<JasperExportManager> exporter =
                    mockStatic(JasperExportManager.class)
        ) {

            compiler.when(() ->
                    JasperCompileManager.compileReport(
                            any(InputStream.class)
                    )
            ).thenReturn(report);

            filler.when(() ->
                    JasperFillManager.fillReport(
                            eq(report),
                            anyMap(),
                            eq(connection)
                    )
            ).thenReturn(print);

            exporter.when(() ->
                    JasperExportManager.exportReportToPdf(print)
            ).thenReturn(new byte[]{1, 2, 3});

            JasperReportService service =
                    new JasperReportService(dataSource);

            byte[] primero = service.generarPdf("REP-DG-01");
            byte[] segundo = service.generarPdf("REP-DG-01");

            assertArrayEquals(new byte[]{1, 2, 3}, primero);
            assertArrayEquals(primero, segundo);

            compiler.verify(() ->
                    JasperCompileManager.compileReport(
                            any(InputStream.class)
                    ), times(1)
            );

            filler.verify(() ->
                    JasperFillManager.fillReport(
                            eq(report),
                            anyMap(),
                            eq(connection)
                    ), times(2)
            );

            verify(dataSource, times(2)).getConnection();
        }
    }

    @Test
    void generarPdf_errorConexion_lanzaIllegalStateException() throws Exception {

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);

        JasperReport report = mock(JasperReport.class);

        when(dataSource.getConnection())
                .thenThrow(new SQLException("Error de conexión"));

        try (
            MockedStatic<JasperCompileManager> compiler =
                    mockStatic(JasperCompileManager.class)
        ) {

            compiler.when(() ->
                    JasperCompileManager.compileReport(
                            any(InputStream.class)
                    )
            ).thenReturn(report);

            JasperReportService service =
                    new JasperReportService(dataSource);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> service.generarPdf("REP-DG-01")
            );

            assertTrue(
                    exception.getMessage().contains(
                            "No se pudo generar el PDF REP-DG-01."
                    )
            );

            assertInstanceOf(
                    SQLException.class,
                    exception.getCause()
            );
        }
    }

    @Test
    void generarPdf_errorCompilacion_lanzaIllegalStateException()
            throws Exception {

        DataSource dataSource = mock(DataSource.class);

        try (
            MockedStatic<JasperCompileManager> compiler =
                    mockStatic(JasperCompileManager.class)
        ) {

            compiler.when(() ->
                    JasperCompileManager.compileReport(
                            any(InputStream.class)
                    )
            ).thenThrow(new JRException("Error de compilación"));

            JasperReportService service =
                    new JasperReportService(dataSource);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> service.generarPdf("REP-DG-01")
            );

            assertTrue(
                    exception.getMessage().contains(
                            "No se pudo cargar la plantilla"
                    )
            );

            assertInstanceOf(
                    JRException.class,
                    exception.getCause()
            );

            verifyNoInteractions(dataSource);
        }
    }
}


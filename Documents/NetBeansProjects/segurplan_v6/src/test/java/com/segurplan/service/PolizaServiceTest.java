package com.segurplan.service;

import com.segurplan.dto.PolizaRequest;
import com.segurplan.model.Contratacion;
import com.segurplan.model.Cotizacion;
import com.segurplan.model.Poliza;
import com.segurplan.model.Usuario;
import com.segurplan.repository.ContratacionRepository;
import com.segurplan.repository.PolizaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de las reglas de negocio de PolizaService.
 * Los repositorios (DAO) se simulan con Mockito: no se necesita base de datos.
 */
@ExtendWith(MockitoExtension.class)
class PolizaServiceTest {

    @Mock
    private PolizaRepository polizaRepository;

    @Mock
    private ContratacionRepository contratacionRepository;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private PolizaService polizaService;

    // ---------- datos de prueba ----------

    private Contratacion contratacionAceptada() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(5L);

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setEstado("Aceptada");
        cotizacion.setUsuario(usuario);

        Contratacion contratacion = new Contratacion();
        contratacion.setIdContratacion(10L);
        contratacion.setCotizacion(cotizacion);
        contratacion.setCondicionesAceptadas(true);
        return contratacion;
    }

    private PolizaRequest requestValido() {
        PolizaRequest r = new PolizaRequest();
        r.setIdContratacion(10L);
        r.setFechaInicio(LocalDate.now());
        r.setFechaFin(LocalDate.now().plusYears(1));
        r.setPrima(new BigDecimal("100.00"));
        r.setCobertura("Todo riesgo");
        r.setDeducible(BigDecimal.ZERO);
        return r;
    }

    // ---------- reglas de negocio ----------

    @Test
    void generar_sinContratacion_lanzaExcepcion() {
        PolizaRequest request = requestValido();
        request.setIdContratacion(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> polizaService.generar(request));

        assertEquals("Seleccione una contratación.", ex.getMessage());
        verifyNoInteractions(polizaRepository, historialService);
    }

    @Test
    void generar_cotizacionNoAceptada_lanzaExcepcion() {
        Contratacion contratacion = contratacionAceptada();
        contratacion.getCotizacion().setEstado("Solicitada");
        when(contratacionRepository.findById(10L))
                .thenReturn(Optional.of(contratacion));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> polizaService.generar(requestValido()));

        assertEquals("La cotización todavía no está aceptada.", ex.getMessage());
        verify(polizaRepository, never()).saveAndFlush(any());
    }

    @Test
    void generar_contratacionConPoliza_lanzaExcepcion() {
        when(contratacionRepository.findById(10L))
                .thenReturn(Optional.of(contratacionAceptada()));
        when(polizaRepository.findByContratacion_IdContratacion(10L))
                .thenReturn(Optional.of(new Poliza()));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> polizaService.generar(requestValido()));

        assertEquals("Esta contratación ya tiene una póliza.", ex.getMessage());
        verify(polizaRepository, never()).saveAndFlush(any());
    }

    @Test
    void generar_primaCero_lanzaExcepcion() {
        when(contratacionRepository.findById(10L))
                .thenReturn(Optional.of(contratacionAceptada()));
        PolizaRequest request = requestValido();
        request.setPrima(BigDecimal.ZERO);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> polizaService.generar(request));

        assertEquals("Ingrese una prima mayor que cero.", ex.getMessage());
    }

    @Test
    void generar_fechaFinAnteriorAlInicio_lanzaExcepcion() {
        when(contratacionRepository.findById(10L))
                .thenReturn(Optional.of(contratacionAceptada()));
        PolizaRequest request = requestValido();
        request.setFechaFin(request.getFechaInicio().minusDays(1));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> polizaService.generar(request));

        assertEquals("La fecha de fin debe ser posterior al inicio.", ex.getMessage());
    }

    @Test
    void generar_datosValidos_creaPolizaRecibidaYRegistraHistorial() {
        when(contratacionRepository.findById(10L))
                .thenReturn(Optional.of(contratacionAceptada()));
        when(polizaRepository.saveAndFlush(any(Poliza.class)))
                .thenAnswer(inv -> {
                    Poliza p = inv.getArgument(0);
                    p.setIdPoliza(7L);
                    return p;
                });
        when(polizaRepository.save(any(Poliza.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Poliza poliza = polizaService.generar(requestValido());

        assertEquals("POL-00000007", poliza.getNumeroPoliza());
        assertEquals("Recibida", poliza.getEstado());
        verify(historialService).registrar(
                eq(5L), eq("POLIZA"), eq(7L), eq("CREAR"),
                isNull(), contains("POL-00000007"));
    }
}

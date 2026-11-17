
package com.segurplan.controller;

import com.segurplan.dto.PagoRequest;
import com.segurplan.model.Pago;
import com.segurplan.model.Contratacion;
import com.segurplan.service.PagoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PagoControllerTest {

    private PagoService service;
    private PagoController controller;
    private Authentication authentication;
    private PagoRequest request;

    @BeforeEach
    void setUp() {
        service = mock(PagoService.class);
        controller = new PagoController(service);
        authentication = mock(Authentication.class);
        request = mock(PagoRequest.class);

        when(authentication.getName()).thenReturn("cliente@test.com");
    }

    @Test
    void datosQrDebeRetornarInformacionDelComercio() {
        ResponseEntity<?> respuesta = controller.datosQr();

        assertEquals(200, respuesta.getStatusCode().value());
        assertNotNull(respuesta.getBody());
        assertTrue(respuesta.getBody().toString().contains("numero"));
        assertTrue(respuesta.getBody().toString().contains("nombre"));
    }

    @Test
    void pagarConTarjetaAprobado() {
        Pago pago = mock(Pago.class);
        Contratacion contratacion = mock(Contratacion.class);

        when(service.pagar("cliente@test.com", request))
                .thenReturn(pago);
        when(pago.getEstado()).thenReturn("Aprobado");
        when(pago.getTarjetaUltimos4()).thenReturn("1234");
        when(pago.getContratacion()).thenReturn(contratacion);

        ResponseEntity<?> respuesta =
                controller.pagar(request, authentication);

        assertEquals(200, respuesta.getStatusCode().value());
        assertTrue(respuesta.getBody().toString().contains("aprobado=true"));
        assertTrue(respuesta.getBody().toString().contains("1234"));
        verify(service).pagar("cliente@test.com", request);
    }

    @Test
    void pagarConYapeAprobado() {
        Pago pago = mock(Pago.class);
        Contratacion contratacion = mock(Contratacion.class);

        when(service.pagar("cliente@test.com", request))
                .thenReturn(pago);
        when(pago.getEstado()).thenReturn("Aprobado");
        when(pago.getCelularOrigen()).thenReturn("987654321");
        when(pago.getDestino()).thenReturn("999888777");
        when(pago.getContratacion()).thenReturn(contratacion);

        ResponseEntity<?> respuesta =
                controller.pagar(request, authentication);

        assertEquals(200, respuesta.getStatusCode().value());
        assertTrue(respuesta.getBody().toString().contains("Celular"));
        assertTrue(respuesta.getBody().toString().contains("987654321"));
    }

    @Test
    void pagarRechazado() {
        Pago pago = mock(Pago.class);

        when(service.pagar("cliente@test.com", request))
                .thenReturn(pago);
        when(pago.getEstado()).thenReturn("Rechazado");
        when(pago.getMotivoRechazo()).thenReturn("Fondos insuficientes");

        ResponseEntity<?> respuesta =
                controller.pagar(request, authentication);

        assertEquals(402, respuesta.getStatusCode().value());
        assertTrue(respuesta.getBody().toString()
                .contains("Fondos insuficientes"));
    }

    @Test
    void pagarCuandoOcurreUnError() {
        when(service.pagar("cliente@test.com", request))
                .thenThrow(new RuntimeException("Error al procesar pago"));

        ResponseEntity<?> respuesta =
                controller.pagar(request, authentication);

        assertEquals(400, respuesta.getStatusCode().value());
        assertTrue(respuesta.getBody().toString()
                .contains("Error al procesar pago"));
    }

    @Test
    void pagarAprobadoSinDetalleAdicional() {
        Pago pago = mock(Pago.class);
        Contratacion contratacion = mock(Contratacion.class);

        when(service.pagar("cliente@test.com", request))
                .thenReturn(pago);
        when(pago.getEstado()).thenReturn("Aprobado");
        when(pago.getTarjetaUltimos4()).thenReturn(null);
        when(pago.getCelularOrigen()).thenReturn(null);
        when(pago.getContratacion()).thenReturn(contratacion);

        ResponseEntity<?> respuesta =
                controller.pagar(request, authentication);

        assertEquals(200, respuesta.getStatusCode().value());
        assertTrue(respuesta.getBody().toString().contains("detalle="));
    }
}

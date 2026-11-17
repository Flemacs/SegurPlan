
package com.segurplan.controller;

import com.segurplan.dto.ContratacionRequest;
import com.segurplan.model.Contratacion;
import com.segurplan.model.Cotizacion;
import com.segurplan.service.ContratacionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContratacionControllerTest {

    @Mock
    private ContratacionService contratacionService;

    @InjectMocks
    private ContratacionController controller;

    @Test
    void listarPendientes_retornaListaCorrectamente() {
        List<Map<String, Object>> lista = List.of(
                Map.of(
                        "idContratacion", 1L,
                        "cliente", "Cliente de prueba"
                )
        );

        when(contratacionService.listarPendientes())
                .thenReturn(lista);

        ResponseEntity<?> respuesta = controller.listarPendientes();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(lista, respuesta.getBody());

        verify(contratacionService).listarPendientes();
    }

    @Test
    void listarPendientes_sinResultados_retornaListaVacia() {
        when(contratacionService.listarPendientes())
                .thenReturn(List.of());

        ResponseEntity<?> respuesta = controller.listarPendientes();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertTrue(((List<?>) respuesta.getBody()).isEmpty());
    }

    @Test
    void crear_contratacionValida_retornaDatosCorrectos() {
        ContratacionRequest request = new ContratacionRequest();

        Contratacion contratacion = mock(Contratacion.class);
        Cotizacion cotizacion = mock(Cotizacion.class);

        LocalDateTime fecha =
                LocalDateTime.of(2026, 11, 10, 14, 30);

        when(contratacionService.crear(request))
                .thenReturn(contratacion);

        when(contratacion.getIdContratacion()).thenReturn(15L);
        when(contratacion.getCotizacion()).thenReturn(cotizacion);
        when(cotizacion.getIdCotizacion()).thenReturn(8L);
        when(contratacion.getEstado()).thenReturn("Iniciada");
        when(contratacion.getFechaContratacion()).thenReturn(fecha);

        ResponseEntity<?> respuesta = controller.crear(request);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());

        Map<?, ?> cuerpo = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "Contratación registrada correctamente",
                cuerpo.get("mensaje")
        );
        assertEquals(15L, cuerpo.get("idContratacion"));
        assertEquals(8L, cuerpo.get("idCotizacion"));
        assertEquals("Iniciada", cuerpo.get("estado"));
        assertEquals(fecha, cuerpo.get("fechaContratacion"));

        verify(contratacionService).crear(request);
    }

    @Test
    void crear_errorDelServicio_retornaBadRequest() {
        ContratacionRequest request = new ContratacionRequest();

        when(contratacionService.crear(request))
                .thenThrow(new RuntimeException("Cotización no encontrada"));

        ResponseEntity<?> respuesta = controller.crear(request);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());

        Map<?, ?> cuerpo = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "Cotización no encontrada",
                cuerpo.get("mensaje")
        );

        verify(contratacionService).crear(request);
    }
}


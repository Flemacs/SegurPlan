
package com.segurplan.controller;

import com.segurplan.dto.HistorialDTO;
import com.segurplan.service.HistorialService;

import java.util.List;

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
class HistorialAdminControllerTest {

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private HistorialAdminController controller;

    @Test
    void listarHistorial_retornaListaCorrectamente() {
        List<HistorialDTO> lista = List.of(
                mock(HistorialDTO.class),
                mock(HistorialDTO.class)
        );

        when(historialService.listarTodos()).thenReturn(lista);

        ResponseEntity<List<HistorialDTO>> respuesta =
                controller.listarHistorial();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(lista, respuesta.getBody());
        assertEquals(2, respuesta.getBody().size());

        verify(historialService).listarTodos();
    }

    @Test
    void listarHistorial_sinRegistros_retornaListaVacia() {
        when(historialService.listarTodos()).thenReturn(List.of());

        ResponseEntity<List<HistorialDTO>> respuesta =
                controller.listarHistorial();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertTrue(respuesta.getBody().isEmpty());

        verify(historialService).listarTodos();
    }

    @Test
    void listarPorEntidad_retornaResultadosCorrectamente() {
        String entidad = "POLIZA";

        List<HistorialDTO> lista = List.of(
                mock(HistorialDTO.class)
        );

        when(historialService.listarPorEntidad(entidad))
                .thenReturn(lista);

        ResponseEntity<List<HistorialDTO>> respuesta =
                controller.listarPorEntidad(entidad);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(lista, respuesta.getBody());
        assertEquals(1, respuesta.getBody().size());

        verify(historialService).listarPorEntidad(entidad);
    }

    @Test
    void listarPorEntidad_sinResultados_retornaListaVacia() {
        String entidad = "SINIESTRO";

        when(historialService.listarPorEntidad(entidad))
                .thenReturn(List.of());

        ResponseEntity<List<HistorialDTO>> respuesta =
                controller.listarPorEntidad(entidad);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertTrue(respuesta.getBody().isEmpty());

        verify(historialService).listarPorEntidad(entidad);
    }
}


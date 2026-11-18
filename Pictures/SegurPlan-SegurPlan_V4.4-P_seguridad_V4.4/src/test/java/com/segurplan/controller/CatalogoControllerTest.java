
package com.segurplan.controller;

import com.segurplan.model.Aseguradora;
import com.segurplan.model.TipoSeguro;
import com.segurplan.repository.AseguradoraRepository;
import com.segurplan.repository.TipoSeguroRepository;

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
class CatalogoControllerTest {

    @Mock
    private TipoSeguroRepository tipoSeguroRepository;

    @Mock
    private AseguradoraRepository aseguradoraRepository;

    @InjectMocks
    private CatalogoController catalogoController;

    @Test
    void listarTiposSeguro_retornaListaCorrectamente() {
        TipoSeguro tipo = new TipoSeguro();

        when(tipoSeguroRepository.findByEstadoOrderByNombreAsc("Activo"))
                .thenReturn(List.of(tipo));

        ResponseEntity<List<TipoSeguro>> respuesta =
                catalogoController.listarTiposSeguro();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(1, respuesta.getBody().size());
        assertSame(tipo, respuesta.getBody().get(0));

        verify(tipoSeguroRepository).findByEstadoOrderByNombreAsc("Activo");
    }

    @Test
    void listarTiposSeguro_sinRegistros_retornaListaVacia() {
        when(tipoSeguroRepository.findByEstadoOrderByNombreAsc("Activo"))
                .thenReturn(List.of());

        ResponseEntity<List<TipoSeguro>> respuesta =
                catalogoController.listarTiposSeguro();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertTrue(respuesta.getBody().isEmpty());

        verify(tipoSeguroRepository).findByEstadoOrderByNombreAsc("Activo");
    }

    @Test
    void listarAseguradoras_retornaListaCorrectamente() {
        Aseguradora aseguradora = new Aseguradora();

        when(aseguradoraRepository.findByEstadoOrderByNombreAsc("Activo"))
                .thenReturn(List.of(aseguradora));

        ResponseEntity<List<Aseguradora>> respuesta =
                catalogoController.listarAseguradoras();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(1, respuesta.getBody().size());
        assertSame(aseguradora, respuesta.getBody().get(0));

        verify(aseguradoraRepository).findByEstadoOrderByNombreAsc("Activo");
    }

    @Test
    void listarAseguradoras_sinRegistros_retornaListaVacia() {
        when(aseguradoraRepository.findByEstadoOrderByNombreAsc("Activo"))
                .thenReturn(List.of());

        ResponseEntity<List<Aseguradora>> respuesta =
                catalogoController.listarAseguradoras();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertTrue(respuesta.getBody().isEmpty());

        verify(aseguradoraRepository).findByEstadoOrderByNombreAsc("Activo");
    }
}

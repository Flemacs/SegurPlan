
package com.segurplan.controller;

import com.segurplan.dto.AseguradoraRequest;
import com.segurplan.dto.CambiarEstadoRequest;
import com.segurplan.dto.TipoSeguroRequest;
import com.segurplan.model.Aseguradora;
import com.segurplan.model.TipoSeguro;
import com.segurplan.service.CatalogoAdminService;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoAdminControllerTest {

    @Mock
    private CatalogoAdminService catalogoAdminService;

    @Mock
    private Authentication auth;

    @InjectMocks
    private CatalogoAdminController controller;

    @Test
    void listarAseguradoras_retornaLista() {
        List<Aseguradora> lista = List.of(mock(Aseguradora.class));
        when(catalogoAdminService.listarAseguradoras()).thenReturn(lista);

        ResponseEntity<?> respuesta = controller.listarAseguradoras();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(lista, respuesta.getBody());
    }

    @Test
    void crearAseguradora_retornaResultado() {
        AseguradoraRequest request = new AseguradoraRequest();
        Aseguradora resultado = mock(Aseguradora.class);

        when(auth.getName()).thenReturn("admin@segurplan.com");
        when(catalogoAdminService.crearAseguradora(
                request, "admin@segurplan.com")).thenReturn(resultado);

        ResponseEntity<?> respuesta = controller.crearAseguradora(request, auth);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }

    @Test
    void crearAseguradora_errorServicio_retornaBadRequest() {
        AseguradoraRequest request = new AseguradoraRequest();

        when(auth.getName()).thenReturn("admin@segurplan.com");
        when(catalogoAdminService.crearAseguradora(
                request, "admin@segurplan.com"))
                .thenThrow(new RuntimeException("Datos inválidos"));

        ResponseEntity<?> respuesta = controller.crearAseguradora(request, auth);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals(
                "Datos inválidos",
                ((Map<?, ?>) respuesta.getBody()).get("mensaje")
        );
    }

    @Test
    void actualizarAseguradora_retornaResultado() {
        AseguradoraRequest request = new AseguradoraRequest();
        Aseguradora resultado = mock(Aseguradora.class);

        when(auth.getName()).thenReturn("admin@segurplan.com");
        when(catalogoAdminService.actualizarAseguradora(
                1, request, "admin@segurplan.com")).thenReturn(resultado);

        ResponseEntity<?> respuesta =
                controller.actualizarAseguradora(1, request, auth);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }

    @Test
    void estadoAseguradora_retornaResultado() {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setEstado("Inactivo");

        Aseguradora resultado = mock(Aseguradora.class);

        when(auth.getName()).thenReturn("admin@segurplan.com");
        when(catalogoAdminService.cambiarEstadoAseguradora(
                1, "Inactivo", "admin@segurplan.com")).thenReturn(resultado);

        ResponseEntity<?> respuesta =
                controller.estadoAseguradora(1, request, auth);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }

    @Test
    void listarTiposSeguro_retornaLista() {
        List<TipoSeguro> lista = List.of(mock(TipoSeguro.class));
        when(catalogoAdminService.listarTiposSeguro()).thenReturn(lista);

        ResponseEntity<?> respuesta = controller.listarTiposSeguro();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(lista, respuesta.getBody());
    }

    @Test
    void crearTipoSeguro_retornaResultado() {
        TipoSeguroRequest request = new TipoSeguroRequest();
        TipoSeguro resultado = mock(TipoSeguro.class);

        when(auth.getName()).thenReturn("admin@segurplan.com");
        when(catalogoAdminService.crearTipoSeguro(
                request, "admin@segurplan.com")).thenReturn(resultado);

        ResponseEntity<?> respuesta = controller.crearTipoSeguro(request, auth);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }

    @Test
    void actualizarTipoSeguro_retornaResultado() {
        TipoSeguroRequest request = new TipoSeguroRequest();
        TipoSeguro resultado = mock(TipoSeguro.class);

        when(auth.getName()).thenReturn("admin@segurplan.com");
        when(catalogoAdminService.actualizarTipoSeguro(
                2, request, "admin@segurplan.com")).thenReturn(resultado);

        ResponseEntity<?> respuesta =
                controller.actualizarTipoSeguro(2, request, auth);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }

    @Test
    void estadoTipoSeguro_retornaResultado() {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setEstado("Activo");

        TipoSeguro resultado = mock(TipoSeguro.class);

        when(auth.getName()).thenReturn("admin@segurplan.com");
        when(catalogoAdminService.cambiarEstadoTipoSeguro(
                2, "Activo", "admin@segurplan.com")).thenReturn(resultado);

        ResponseEntity<?> respuesta =
                controller.estadoTipoSeguro(2, request, auth);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }
}


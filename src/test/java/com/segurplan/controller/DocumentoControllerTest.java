
package com.segurplan.controller;

import com.segurplan.model.Documento;
import com.segurplan.service.DocumentoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentoControllerTest {

    private DocumentoService service;
    private DocumentoController controller;
    private Principal principal;

    @BeforeEach
    void setUp() {
        service = mock(DocumentoService.class);
        controller = new DocumentoController(
                service,
                mock(com.segurplan.service.SiniestroGestionService.class));

        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("cliente@test.com");
    }

    private Documento crearDocumento() {
        Documento documento = new Documento();
        documento.setIdDocumento(1L);
        documento.setTipoDocumento("FOTO_ACCIDENTE");
        documento.setNombreArchivo("foto.jpg");
        documento.setVersion(1);
        documento.setFechaCarga(LocalDateTime.now());
        documento.setEstado("Pendiente");
        return documento;
    }

    @Test
    void subirEvidenciaSinPrincipal() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", new byte[]{1}
        );

        ResponseEntity<?> respuesta = controller.subirEvidencia(
                1L, "FOTO_ACCIDENTE", archivo, null
        );

        assertEquals(401, respuesta.getStatusCode().value());
    }

    @Test
    void subirEvidenciaCorrectamente() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", new byte[]{1}
        );

        when(service.guardarEvidencia(
                "cliente@test.com", 1L, "FOTO_ACCIDENTE", archivo
        )).thenReturn(crearDocumento());

        ResponseEntity<?> respuesta = controller.subirEvidencia(
                1L, "FOTO_ACCIDENTE", archivo, principal
        );

        assertEquals(200, respuesta.getStatusCode().value());
        assertTrue(((Map<?, ?>) respuesta.getBody())
                .containsKey("idDocumento"));
    }

    @Test
    void subirEvidenciaSinPermiso() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", new byte[]{1}
        );

        when(service.guardarEvidencia(
                anyString(), anyLong(), anyString(), any()
        )).thenThrow(new SecurityException("Sin permiso"));

        ResponseEntity<?> respuesta = controller.subirEvidencia(
                1L, "FOTO_ACCIDENTE", archivo, principal
        );

        assertEquals(403, respuesta.getStatusCode().value());
    }

    @Test
    void subirEvidenciaConArchivoInvalido() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", new byte[]{1}
        );

        when(service.guardarEvidencia(
                anyString(), anyLong(), anyString(), any()
        )).thenThrow(new IllegalArgumentException("Formato inválido"));

        ResponseEntity<?> respuesta = controller.subirEvidencia(
                1L, "FOTO_ACCIDENTE", archivo, principal
        );

        assertEquals(400, respuesta.getStatusCode().value());
    }

    @Test
    void listarEvidenciasSinPrincipal() {
        ResponseEntity<?> respuesta =
                controller.listarEvidencias(1L, null);

        assertEquals(401, respuesta.getStatusCode().value());
    }

    @Test
    void listarEvidenciasCorrectamente() {
        when(service.listarPorSiniestro("cliente@test.com", 1L))
                .thenReturn(List.of(crearDocumento()));

        ResponseEntity<?> respuesta =
                controller.listarEvidencias(1L, principal);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(1, ((List<?>) respuesta.getBody()).size());
    }

    @Test
    void listarEvidenciasSinPermiso() {
        when(service.listarPorSiniestro(anyString(), anyLong()))
                .thenThrow(new SecurityException("Sin permiso"));

        ResponseEntity<?> respuesta =
                controller.listarEvidencias(1L, principal);

        assertEquals(403, respuesta.getStatusCode().value());
    }

    @Test
    void eliminarEvidenciaSinPrincipal() {
        ResponseEntity<?> respuesta =
                controller.eliminarEvidencia(1L, null);

        assertEquals(401, respuesta.getStatusCode().value());
    }

    @Test
    void eliminarEvidenciaCorrectamente() {
        doNothing().when(service).eliminar("cliente@test.com", 1L);

        ResponseEntity<?> respuesta =
                controller.eliminarEvidencia(1L, principal);

        assertEquals(200, respuesta.getStatusCode().value());
    }

    @Test
    void eliminarEvidenciaSinPermiso() {
        doThrow(new SecurityException("Sin permiso"))
                .when(service).eliminar(anyString(), anyLong());

        ResponseEntity<?> respuesta =
                controller.eliminarEvidencia(1L, principal);

        assertEquals(403, respuesta.getStatusCode().value());
    }

    @Test
    void listarParaRevisionSinPrincipal() {
        ResponseEntity<?> respuesta =
                controller.listarParaRevision(1L, null);

        assertEquals(401, respuesta.getStatusCode().value());
    }

    @Test
    void listarParaRevisionCorrectamente() {
        when(service.listarParaRevision(1L))
                .thenReturn(List.of(crearDocumento()));

        ResponseEntity<?> respuesta =
                controller.listarParaRevision(1L, principal);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(1, ((List<?>) respuesta.getBody()).size());
    }

    @Test
    void listarParaRevisionConIdInvalido() {
        when(service.listarParaRevision(1L))
                .thenThrow(new IllegalArgumentException("No existe"));

        ResponseEntity<?> respuesta =
                controller.listarParaRevision(1L, principal);

        assertEquals(400, respuesta.getStatusCode().value());
    }
}

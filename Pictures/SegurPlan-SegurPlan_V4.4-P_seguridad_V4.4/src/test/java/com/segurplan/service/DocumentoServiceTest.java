package com.segurplan.service;

import com.segurplan.model.Siniestro;
import com.segurplan.model.Usuario;
import com.segurplan.repository.DocumentoRepository;
import com.segurplan.repository.SiniestroRepository;
import com.segurplan.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/**
 * Pruebas unitarias de las validaciones de carga de evidencias.
 * Todas fallan antes de escribir en disco, por lo que no se crean archivos.
 */
@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    private static final String CORREO = "cliente@segurplan.test";
    private static final long MB = 1024L * 1024L;

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SiniestroRepository siniestroRepository;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private DocumentoService documentoService;

    /** Prepara un usuario dueño del siniestro 9. */
    private void siniestroDelUsuario(long idDueno) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(5L);

        Usuario dueno = new Usuario();
        dueno.setIdUsuario(idDueno);

        Siniestro siniestro = new Siniestro();
        siniestro.setIdSiniestro(9L);
        siniestro.setUsuario(dueno);

        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(siniestroRepository.findById(9L)).thenReturn(Optional.of(siniestro));
    }

    @Test
    void guardarEvidencia_siniestroDeOtroUsuario_lanzaSecurityException() {
        siniestroDelUsuario(99L);
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", new byte[]{1});

        assertThrows(SecurityException.class,
                () -> documentoService.guardarEvidencia(
                        CORREO, 9L, "FOTO_ACCIDENTE", archivo));

        verify(documentoRepository, never()).save(any());
    }

    @Test
    void guardarEvidencia_sinArchivo_lanzaExcepcion() {
        siniestroDelUsuario(5L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> documentoService.guardarEvidencia(
                        CORREO, 9L, "FOTO_ACCIDENTE", null));

        assertEquals("Debe seleccionar un archivo.", ex.getMessage());
    }

    @Test
    void guardarEvidencia_extensionNoPermitida_lanzaExcepcion() {
        siniestroDelUsuario(5L);
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "virus.exe", "application/octet-stream", new byte[]{1});

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> documentoService.guardarEvidencia(
                        CORREO, 9L, "FOTO_ACCIDENTE", archivo));

        assertEquals("El formato del archivo no está permitido.", ex.getMessage());
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void guardarEvidencia_fotoMayorA20MB_lanzaExcepcion() {
        siniestroDelUsuario(5L);
        MultipartFile archivo = mock(MultipartFile.class);
        when(archivo.isEmpty()).thenReturn(false);
        when(archivo.getOriginalFilename()).thenReturn("foto.jpg");
        when(archivo.getSize()).thenReturn(21 * MB);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> documentoService.guardarEvidencia(
                        CORREO, 9L, "FOTO_ACCIDENTE", archivo));

        assertEquals("El archivo supera el tamaño máximo permitido.", ex.getMessage());
        verify(documentoRepository, never()).save(any());
    }
}

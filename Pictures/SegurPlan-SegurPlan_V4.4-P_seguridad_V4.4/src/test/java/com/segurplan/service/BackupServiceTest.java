
package com.segurplan.service;

import com.segurplan.dto.BackupDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/segurplan";

    private static final String USUARIO = "postgres";
    private static final String CLAVE = "test";

    @TempDir
    Path tempDir;

    @Mock
    private HistorialService historialService;

    private BackupService backupService;

    private BackupService crearServicio(
            String url,
            Path directorio,
            String binDir,
            boolean automatico) {

        return new BackupService(
                historialService,
                url,
                USUARIO,
                CLAVE,
                directorio.toString(),
                binDir,
                14,
                automatico
        );
    }

    @BeforeEach
    void setUp() {
        backupService = crearServicio(
                URL,
                tempDir.resolve("backups"),
                "",
                false
        );
    }

    @Test
    void listar_directorioInexistente_retornaListaVacia() {
        List<BackupDTO> resultado = backupService.listar();

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listar_directorioConArchivos_retornaSoloRespaldosValidos()
            throws IOException {

        Path directorio = tempDir.resolve("listado");
        Files.createDirectories(directorio);

        Path antiguo = Files.createFile(
                directorio.resolve("segurplan_20261109_100000.sql"));

        Path reciente = Files.createFile(
                directorio.resolve("segurplan_auto_20261110_100000.sql"));

        Files.createFile(directorio.resolve("archivo.txt"));
        Files.createFile(directorio.resolve("respaldo.sql"));

        Files.setLastModifiedTime(
                antiguo,
                FileTime.from(Instant.parse("2026-11-09T10:00:00Z")));

        Files.setLastModifiedTime(
                reciente,
                FileTime.from(Instant.parse("2026-11-10T10:00:00Z")));

        BackupService servicio = crearServicio(
                URL, directorio, "", false);

        List<BackupDTO> resultado = servicio.listar();

        assertEquals(2, resultado.size());
        assertEquals(
                "segurplan_auto_20261110_100000.sql",
                resultado.get(0).getNombre());
        assertEquals(
                "segurplan_20261109_100000.sql",
                resultado.get(1).getNombre());
    }

    @Test
    void listar_directorioVacio_retornaListaVacia()
            throws IOException {

        Path directorio = Files.createDirectory(
                tempDir.resolve("vacio"));

        BackupService servicio = crearServicio(
                URL, directorio, "", false);

        assertTrue(servicio.listar().isEmpty());
    }

    @Test
    void obtenerArchivo_archivoValido_retornaSuRuta()
            throws IOException {

        Path directorio = Files.createDirectory(
                tempDir.resolve("obtener"));

        String nombre = "segurplan_20261110_120000.sql";

        Path archivo = Files.createFile(
                directorio.resolve(nombre));

        BackupService servicio = crearServicio(
                URL, directorio, "", false);

        assertEquals(archivo, servicio.obtenerArchivo(nombre));
    }

    @Test
    void obtenerArchivo_nombreNulo_lanzaExcepcion() {

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> backupService.obtenerArchivo(null));

        assertEquals(
                "Nombre de respaldo inválido.",
                ex.getMessage());
    }

    @Test
    void obtenerArchivo_nombreInvalido_lanzaExcepcion() {

        assertThrows(
                RuntimeException.class,
                () -> backupService.obtenerArchivo("respaldo.txt"));
    }

    @Test
    void obtenerArchivo_archivoInexistente_lanzaExcepcion() {

        assertThrows(
                RuntimeException.class,
                () -> backupService.obtenerArchivo(
                        "segurplan_20261110_120000.sql"));
    }

    @Test
    void obtenerArchivo_nombreConRutaManipulada_lanzaExcepcion() {

        assertThrows(
                RuntimeException.class,
                () -> backupService.obtenerArchivo(
                        "../segurplan_20261110_120000.sql"));
    }

    @Test
    void obtenerArchivo_rutaCorrespondeADirectorio_lanzaExcepcion()
            throws IOException {

        Path directorio = Files.createDirectory(
                tempDir.resolve("directorio"));

        String nombre = "segurplan_20261110_120000.sql";

        Files.createDirectory(directorio.resolve(nombre));

        BackupService servicio = crearServicio(
                URL, directorio, "", false);

        assertThrows(
                RuntimeException.class,
                () -> servicio.obtenerArchivo(nombre));
    }

    @Test
    void eliminar_archivoExistente_eliminaYRegistraHistorial()
            throws IOException {

        Path directorio = Files.createDirectory(
                tempDir.resolve("eliminar"));

        String nombre = "segurplan_20261110_120000.sql";

        Path archivo = Files.createFile(
                directorio.resolve(nombre));

        BackupService servicio = crearServicio(
                URL, directorio, "", false);

        servicio.eliminar(nombre, 5L);

        assertFalse(Files.exists(archivo));

        verify(historialService).registrar(
                5L,
                "Backup",
                0L,
                "BACKUP_ELIMINAR",
                nombre,
                null
        );
    }

    @Test
    void eliminar_archivoInexistente_lanzaExcepcion() {

        assertThrows(
                RuntimeException.class,
                () -> backupService.eliminar(
                        "segurplan_20261110_120000.sql",
                        5L));

        verifyNoInteractions(historialService);
    }

    @Test
    void crear_urlJDBCInvalida_lanzaExcepcion() {

        BackupService servicio = crearServicio(
                "jdbc:mysql://localhost:3306/segurplan",
                tempDir.resolve("crear"),
                "",
                false
        );

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> servicio.crear(5L));

        assertEquals(
                "URL de base de datos no soportada.",
                ex.getMessage());

        verifyNoInteractions(historialService);
    }

    @Test
    void crear_pgDumpNoDisponible_lanzaExcepcionYNoRegistraHistorial() {

        Path directorio = tempDir.resolve("crear-error");

        BackupService servicio = crearServicio(
                URL,
                directorio,
                tempDir.resolve("bin-inexistente").toString(),
                false
        );

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> servicio.crear(5L));

        assertTrue(ex.getMessage().contains("No se encontró"));

        verifyNoInteractions(historialService);
    }

    @Test
    void restaurar_nombreInvalido_lanzaExcepcion() {

        assertThrows(
                RuntimeException.class,
                () -> backupService.restaurar(
                        "archivo-no-valido.sql",
                        5L));

        verifyNoInteractions(historialService);
    }

    @Test
    void respaldoAutomatico_deshabilitado_noHaceNada() {

        backupService.respaldoAutomatico();

        verifyNoInteractions(historialService);
    }

    @Test
    void respaldoAutomatico_urlInvalida_noPropagaExcepcion() {

        BackupService servicio = crearServicio(
                "jdbc:mysql://localhost:3306/segurplan",
                tempDir.resolve("automatico"),
                "",
                true
        );

        servicio.respaldoAutomatico();

        verifyNoInteractions(historialService);
    }
}

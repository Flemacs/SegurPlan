package com.segurplan.service;

import com.segurplan.dto.BackupDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Respaldo y restauración de la base de datos PostgreSQL.
 *
 * Usa las herramientas oficiales pg_dump (respaldo) y psql (restauración)
 * a través de ProcessBuilder. Los parámetros de conexión se leen de
 * spring.datasource.* para que haya una sola fuente de configuración.
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private static final Pattern JDBC =
            Pattern.compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?]+).*");

    private static final Pattern NOMBRE_VALIDO =
            Pattern.compile("^segurplan_(auto_|pre_)?\\d{8}_\\d{6}\\.sql$");

    private static final DateTimeFormatter SUFIJO =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final String url;
    private final String usuario;
    private final String clave;
    private final Path directorio;
    private final String binDir;
    private final int retencionDias;
    private final boolean automatico;

    private final HistorialService historialService;

    public BackupService(
            HistorialService historialService,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String usuario,
            @Value("${spring.datasource.password}") String clave,
            @Value("${segurplan.backup.dir:./backups}") String dir,
            @Value("${segurplan.backup.pg-bin-dir:}") String binDir,
            @Value("${segurplan.backup.retention-days:14}") int retencionDias,
            @Value("${segurplan.backup.auto-enabled:true}") boolean automatico) {

        this.historialService = historialService;
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
        this.directorio = Paths.get(dir).toAbsolutePath().normalize();
        this.binDir = binDir == null ? "" : binDir.trim();
        this.retencionDias = retencionDias;
        this.automatico = automatico;
    }

    // =====================================================
    // CREAR RESPALDO
    // =====================================================

    public BackupDTO crear(Long idUsuario) {
        return crearConPrefijo("segurplan_", idUsuario, "BACKUP_MANUAL");
    }

    private BackupDTO crearConPrefijo(String prefijo, Long idUsuario, String accion) {

        Conexion c = conexion();

        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la carpeta de respaldos.");
        }

        String nombre = prefijo + LocalDateTime.now().format(SUFIJO) + ".sql";
        Path destino = directorio.resolve(nombre);

        ejecutar(List.of(
                herramienta("pg_dump"),
                "-h", c.host, "-p", c.puerto, "-U", usuario,
                "--format=plain", "--clean", "--if-exists", "--no-owner",
                "-f", destino.toString(), c.base),
                destino);

        BackupDTO dto = aDto(destino);

        historialService.registrar(idUsuario, "Backup", 0L, accion, null, nombre);

        log.info("Respaldo creado: {} ({} bytes)", nombre, dto.getTamanoBytes());

        return dto;
    }

    // =====================================================
    // LISTAR / DESCARGAR / ELIMINAR
    // =====================================================

    public List<BackupDTO> listar() {

        List<BackupDTO> lista = new ArrayList<>();

        if (!Files.isDirectory(directorio)) {
            return lista;
        }

        try (Stream<Path> s = Files.list(directorio)) {
            s.filter(p -> NOMBRE_VALIDO.matcher(p.getFileName().toString()).matches())
                    .map(this::aDto)
                    .sorted(Comparator.comparing(BackupDTO::getFecha).reversed())
                    .forEach(lista::add);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer la carpeta de respaldos.");
        }

        return lista;
    }

    public Path obtenerArchivo(String nombre) {

        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new RuntimeException("Nombre de respaldo inválido.");
        }

        Path p = directorio.resolve(nombre).normalize();

        // Evita salir de la carpeta de respaldos (path traversal).
        if (!p.startsWith(directorio) || !Files.isRegularFile(p)) {
            throw new RuntimeException("El respaldo no existe.");
        }

        return p;
    }

    public void eliminar(String nombre, Long idUsuario) {

        Path p = obtenerArchivo(nombre);

        try {
            Files.delete(p);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo eliminar el respaldo.");
        }

        historialService.registrar(idUsuario, "Backup", 0L, "BACKUP_ELIMINAR", nombre, null);
    }

    // =====================================================
    // RESTAURAR
    // =====================================================

    /**
     * Restaura un respaldo. Antes de hacerlo genera un respaldo de
     * seguridad ("pre_") para poder volver atrás si algo sale mal.
     */
    public void restaurar(String nombre, Long idUsuario) {

        Path origen = obtenerArchivo(nombre);

        crearConPrefijo("segurplan_pre_", idUsuario, "BACKUP_PRE_RESTAURACION");

        Conexion c = conexion();

        ejecutar(List.of(
                herramienta("psql"),
                "-h", c.host, "-p", c.puerto, "-U", usuario, "-d", c.base,
                "-v", "ON_ERROR_STOP=1", "-q", "-f", origen.toString()),
                null);

        historialService.registrar(idUsuario, "Backup", 0L, "BACKUP_RESTAURAR", null, nombre);

        log.warn("Base de datos restaurada desde {}", nombre);
    }

    // =====================================================
    // RESPALDO AUTOMÁTICO DIARIO + RETENCIÓN
    // =====================================================

    @Scheduled(cron = "${segurplan.backup.cron:0 0 2 * * *}")
    public void respaldoAutomatico() {

        if (!automatico) {
            return;
        }

        try {
            crearConPrefijo("segurplan_auto_", null, "BACKUP_AUTOMATICO");
            purgarAntiguos();
        } catch (RuntimeException e) {
            log.error("Falló el respaldo automático: {}", e.getMessage());
        }
    }

    private void purgarAntiguos() {

        Instant limite = Instant.now().minusSeconds(retencionDias * 86400L);

        for (BackupDTO b : listar()) {

            // Solo se purgan los automáticos; los manuales y "pre_" se conservan.
            if (!b.getNombre().startsWith("segurplan_auto_")) {
                continue;
            }

            Path p = directorio.resolve(b.getNombre());

            try {
                if (Files.getLastModifiedTime(p).toInstant().isBefore(limite)) {
                    Files.deleteIfExists(p);
                    log.info("Respaldo antiguo eliminado: {}", b.getNombre());
                }
            } catch (IOException e) {
                log.warn("No se pudo purgar {}", b.getNombre());
            }
        }
    }

    // =====================================================
    // UTILIDADES
    // =====================================================

    private void ejecutar(List<String> comando, Path archivoSalida) {

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.environment().put("PGPASSWORD", clave);
        pb.redirectErrorStream(true);

        try {
            Process proceso = pb.start();

            String salida = new String(
                    proceso.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);

            if (!proceso.waitFor(180, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
                limpiarParcial(archivoSalida);
                throw new RuntimeException("La operación excedió el tiempo máximo.");
            }

            if (proceso.exitValue() != 0) {
                limpiarParcial(archivoSalida);
                log.error("{} terminó con error: {}", comando.get(0), salida);
                throw new RuntimeException(
                        "La operación falló. Verifique que PostgreSQL y "
                        + "sus herramientas (pg_dump/psql) estén disponibles.");
            }

        } catch (IOException e) {
            limpiarParcial(archivoSalida);
            throw new RuntimeException(
                    "No se encontró " + comando.get(0)
                    + ". Configure segurplan.backup.pg-bin-dir.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operación interrumpida.");
        }
    }

    private void limpiarParcial(Path archivo) {
        if (archivo != null) {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException ignorada) {
                // Sin acción: el archivo parcial no es crítico.
            }
        }
    }

    private String herramienta(String nombre) {
        return binDir.isEmpty() ? nombre : new File(binDir, nombre).getPath();
    }

    private BackupDTO aDto(Path p) {
        try {
            return new BackupDTO(
                    p.getFileName().toString(),
                    Files.size(p),
                    LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(p).toInstant(),
                            ZoneId.systemDefault()));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el respaldo.");
        }
    }

    private Conexion conexion() {

        Matcher m = JDBC.matcher(url);

        if (!m.matches()) {
            throw new RuntimeException("URL de base de datos no soportada.");
        }

        return new Conexion(m.group(1), m.group(2) == null ? "5432" : m.group(2), m.group(3));
    }

    private record Conexion(String host, String puerto, String base) {
    }
}

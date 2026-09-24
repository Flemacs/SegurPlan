package com.segurplan.service;

import com.segurplan.model.Documento;
import com.segurplan.model.Siniestro;
import com.segurplan.model.Usuario;
import com.segurplan.repository.DocumentoRepository;
import com.segurplan.repository.SiniestroRepository;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SiniestroRepository siniestroRepository;

    /*
     * Los archivos se guardarán físicamente en:
     *
     * uploads/siniestros/{idSiniestro}/
     */
    private final Path directorioBase = Paths.get("uploads", "siniestros");

    public DocumentoService(
            DocumentoRepository documentoRepository,
            UsuarioRepository usuarioRepository,
            SiniestroRepository siniestroRepository) {

        this.documentoRepository = documentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.siniestroRepository = siniestroRepository;
    }

    // =====================================================
    // GUARDAR EVIDENCIA
    // =====================================================

    public Documento guardarEvidencia(
            String correo,
            Long idSiniestro,
            String tipoDocumento,
            MultipartFile archivo) {

        // -------------------------------------------------
        // 1. Obtener usuario autenticado
        // -------------------------------------------------

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() ->
                new IllegalArgumentException(
                        "No se encontró el usuario autenticado."
                ));

        // -------------------------------------------------
        // 2. Obtener siniestro
        // -------------------------------------------------

        Siniestro siniestro = siniestroRepository.findById(idSiniestro)
                .orElseThrow(() ->
                new IllegalArgumentException(
                        "El siniestro indicado no existe."
                ));

        // -------------------------------------------------
        // 3. Comprobar propietario
        // -------------------------------------------------

        if (siniestro.getUsuario() == null
                || siniestro.getUsuario().getIdUsuario() == null
                || !siniestro.getUsuario()
                        .getIdUsuario()
                        .equals(usuario.getIdUsuario())) {

            throw new SecurityException(
                    "No tiene permiso para adjuntar documentos a este siniestro."
            );
        }

        // -------------------------------------------------
        // 4. Validaciones
        // -------------------------------------------------

        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            throw new IllegalArgumentException(
                    "Debe indicar el tipo de documento."
            );
        }

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un archivo."
            );
        }

        String nombreOriginal = archivo.getOriginalFilename();

        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            nombreOriginal = "archivo";
        }

        // Elimina cualquier ruta enviada en el nombre
        nombreOriginal = Paths.get(nombreOriginal)
                .getFileName()
                .toString();

        validarTipoArchivo(tipoDocumento, nombreOriginal);

        validarTamanoArchivo(tipoDocumento, archivo.getSize());

        // -------------------------------------------------
        // 5. Crear directorio
        // -------------------------------------------------

        Path directorioSiniestro = directorioBase.resolve(
                String.valueOf(idSiniestro)
        );

        try {
            Files.createDirectories(directorioSiniestro);
        } catch (IOException e) {
            throw new RuntimeException(
                    "No se pudo crear el directorio para las evidencias.",
                    e
            );
        }

        // -------------------------------------------------
        // 6. Crear nombre físico único
        // -------------------------------------------------

        String extension = obtenerExtension(nombreOriginal);

        String nombreFisico
                = UUID.randomUUID().toString()
                        .replace("-", "")
                + extension;

        Path destino = directorioSiniestro
                .resolve(nombreFisico)
                .normalize();

        /*
         * Protección adicional para evitar que una ruta salga
         * del directorio correspondiente al siniestro.
         */
        if (!destino.startsWith(directorioSiniestro.normalize())) {
            throw new IllegalArgumentException(
                    "Ruta de archivo inválida."
            );
        }

        // -------------------------------------------------
        // 7. Guardar archivo físicamente
        // -------------------------------------------------

        try {

            Files.copy(
                    archivo.getInputStream(),
                    destino,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "No se pudo guardar el archivo.",
                    e
            );
        }

        // -------------------------------------------------
        // 8. Registrar documento en PostgreSQL
        // -------------------------------------------------

        Documento documento = new Documento();

        documento.setUsuario(usuario);
        documento.setSiniestro(siniestro);

        // No pertenece a contratación
        documento.setContratacion(null);

        documento.setTipoDocumento(
                tipoDocumento.trim().toUpperCase()
        );

        documento.setNombreArchivo(nombreOriginal);

        /*
         * Guardamos una ruta relativa.
         *
         * Ejemplo:
         * uploads/siniestros/5/a12b34c56.pdf
         */
        documento.setRutaArchivo(
                destino.toString().replace("\\", "/")
        );

        documento.setVersion(1);
        documento.setFechaCarga(LocalDateTime.now());
        documento.setEstado("Cargado");

        try {

            return documentoRepository.save(documento);

        } catch (RuntimeException e) {

            /*
             * Si PostgreSQL rechaza el INSERT, intentamos
             * eliminar el archivo físico para no dejarlo
             * abandonado.
             */
            try {
                Files.deleteIfExists(destino);
            } catch (IOException ignored) {
            }

            throw e;
        }
    }

    // =====================================================
    // LISTAR EVIDENCIAS DE UN SINIESTRO
    // =====================================================

    public List<Documento> listarPorSiniestro(
            String correo,
            Long idSiniestro) {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() ->
                new IllegalArgumentException(
                        "No se encontró el usuario autenticado."
                ));

        Siniestro siniestro = siniestroRepository.findById(idSiniestro)
                .orElseThrow(() ->
                new IllegalArgumentException(
                        "El siniestro indicado no existe."
                ));

        if (siniestro.getUsuario() == null
                || siniestro.getUsuario().getIdUsuario() == null
                || !siniestro.getUsuario()
                        .getIdUsuario()
                        .equals(usuario.getIdUsuario())) {

            throw new SecurityException(
                    "No tiene permiso para consultar estas evidencias."
            );
        }

        return documentoRepository
                .findBySiniestro_IdSiniestroAndUsuario_IdUsuarioOrderByFechaCargaDesc(
                        idSiniestro,
                        usuario.getIdUsuario()
                );
    }

    // =====================================================
    // ELIMINAR EVIDENCIA
    // =====================================================

    public void eliminar(
            String correo,
            Long idDocumento) {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() ->
                new IllegalArgumentException(
                        "No se encontró el usuario autenticado."
                ));

        Documento documento = documentoRepository.findById(idDocumento)
                .orElseThrow(() ->
                new IllegalArgumentException(
                        "El documento indicado no existe."
                ));

        if (documento.getUsuario() == null
                || documento.getUsuario().getIdUsuario() == null
                || !documento.getUsuario()
                        .getIdUsuario()
                        .equals(usuario.getIdUsuario())) {

            throw new SecurityException(
                    "No tiene permiso para eliminar este documento."
            );
        }

        Path ruta = Paths.get(documento.getRutaArchivo())
                .normalize();

        documentoRepository.delete(documento);

        try {
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            System.err.println(
                    "No se pudo eliminar el archivo físico: "
                    + ruta
            );
        }
    }

    // =====================================================
    // VALIDAR EXTENSIONES
    // =====================================================

    private void validarTipoArchivo(
            String tipoDocumento,
            String nombreArchivo) {

        String extension = obtenerExtension(nombreArchivo)
                .toLowerCase();

        String tipo = tipoDocumento
                .trim()
                .toUpperCase();

        boolean permitido;

        switch (tipo) {

            case "FOTO_ACCIDENTE":
                permitido
                        = extension.equals(".jpg")
                        || extension.equals(".jpeg")
                        || extension.equals(".png")
                        || extension.equals(".heic");
                break;

            case "VIDEO_ACCIDENTE":
                permitido
                        = extension.equals(".mp4")
                        || extension.equals(".mov")
                        || extension.equals(".avi");
                break;

            default:
                permitido
                        = extension.equals(".pdf")
                        || extension.equals(".jpg")
                        || extension.equals(".jpeg")
                        || extension.equals(".png");
                break;
        }

        if (!permitido) {
            throw new IllegalArgumentException(
                    "El formato del archivo no está permitido."
            );
        }
    }

    // =====================================================
    // VALIDAR TAMAÑO
    // =====================================================

    private void validarTamanoArchivo(
            String tipoDocumento,
            long bytes) {

        String tipo = tipoDocumento
                .trim()
                .toUpperCase();

        long maximo;

        if ("VIDEO_ACCIDENTE".equals(tipo)) {

            // 100 MB
            maximo = 100L * 1024L * 1024L;

        } else if ("FOTO_ACCIDENTE".equals(tipo)) {

            // 20 MB
            maximo = 20L * 1024L * 1024L;

        } else {

            // Documentos: 10 MB
            maximo = 10L * 1024L * 1024L;
        }

        if (bytes > maximo) {
            throw new IllegalArgumentException(
                    "El archivo supera el tamaño máximo permitido."
            );
        }
    }

    // =====================================================
    // OBTENER EXTENSIÓN
    // =====================================================

    private String obtenerExtension(String nombre) {

        int punto = nombre.lastIndexOf('.');

        if (punto < 0) {
            return "";
        }

        return nombre.substring(punto);
    }
}
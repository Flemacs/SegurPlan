package com.segurplan.service;

import com.segurplan.model.Cotizacion;
import com.segurplan.model.Documento;
import com.segurplan.model.Usuario;
import com.segurplan.repository.CotizacionRepository;
import com.segurplan.repository.DocumentoRepository;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Validación documental por el asesor (RF011), adjuntos de cotización (RF004/RF009)
 * y descarga segura de archivos.
 */
@Service
public class DocumentoGestionService {

    static final long MAX_BYTES = 10L * 1024L * 1024L; // RF009: 10 MB
    private static final Set<String> EXTENSIONES = Set.of(".pdf", ".jpg", ".jpeg", ".png");
    private static final Set<String> ESTADOS_VALIDACION = Set.of("Validado", "Observado", "Rechazado");
    private static final Path DIR_COTIZACIONES = Paths.get("uploads", "cotizaciones");

    private final DocumentoRepository documentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CotizacionRepository cotizacionRepository;
    private final HistorialService historialService;
    private final NotificacionService notificacionService;

    public DocumentoGestionService(DocumentoRepository documentoRepository,
                                   UsuarioRepository usuarioRepository,
                                   CotizacionRepository cotizacionRepository,
                                   HistorialService historialService,
                                   NotificacionService notificacionService) {
        this.documentoRepository = documentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.historialService = historialService;
        this.notificacionService = notificacionService;
    }

    // ------------------------------------------------------------------
    @Transactional
    public Documento validar(String correoAsesor, Long idDocumento, String estado, String observacion) {
        if (estado == null || !ESTADOS_VALIDACION.contains(estado)) {
            throw new IllegalArgumentException("El estado debe ser Validado, Observado o Rechazado.");
        }
        if (!"Validado".equals(estado) && (observacion == null || observacion.isBlank())) {
            throw new IllegalArgumentException("Indique el motivo de la observación o rechazo.");
        }
        Usuario asesor = usuarioRepository.findByCorreo(correoAsesor)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        Documento d = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new IllegalArgumentException("El documento indicado no existe."));

        String anterior = d.getEstado();
        d.setEstado(estado);
        d.setObservacion(observacion == null || observacion.isBlank() ? null : observacion.trim());
        Documento guardado = documentoRepository.save(d);

        historialService.registrar(asesor.getIdUsuario(), "DOCUMENTO", guardado.getIdDocumento(),
                "VALIDAR", "Estado: " + anterior,
                "Estado: " + estado + (observacion == null || observacion.isBlank() ? "" : " - " + observacion.trim()));

        if (!"Validado".equals(estado)) {
            notificacionService.notificar(guardado.getUsuario().getIdUsuario(), "DOCUMENTO_OBSERVADO",
                    "Documento " + estado.toLowerCase() + ": " + guardado.getTipoDocumento(),
                    observacion, guardado.getSiniestro() != null ? "SINIESTRO" : "DOCUMENTO",
                    guardado.getSiniestro() != null ? guardado.getSiniestro().getIdSiniestro()
                            : guardado.getIdDocumento());
        }
        return guardado;
    }

    // ------------------------------------------------------------------
    @Transactional
    public Documento adjuntarCotizacion(String correo, Long idCotizacion,
                                        String tipoDocumento, MultipartFile archivo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        Cotizacion cot = cotizacionRepository.findById(idCotizacion)
                .orElseThrow(() -> new IllegalArgumentException("La cotización indicada no existe."));
        if (!cot.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new SecurityException("No puede adjuntar archivos a esta cotización.");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo.");
        }
        String original = archivo.getOriginalFilename() == null ? "archivo"
                : Paths.get(archivo.getOriginalFilename()).getFileName().toString();
        String ext = extension(original);
        if (!EXTENSIONES.contains(ext)) {
            throw new IllegalArgumentException("Formato no permitido. Use PDF, JPG o PNG.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido (10 MB).");
        }
        Path dir = DIR_COTIZACIONES.resolve(String.valueOf(idCotizacion));
        try {
            Files.createDirectories(dir);
            Path destino = dir.resolve(UUID.randomUUID().toString().replace("-", "") + ext).normalize();
            if (!destino.startsWith(dir.normalize())) {
                throw new IllegalArgumentException("Ruta de archivo inválida.");
            }
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            Documento d = new Documento();
            d.setUsuario(usuario);
            d.setCotizacion(cot);
            d.setTipoDocumento(tipoDocumento == null || tipoDocumento.isBlank() ? "ADJUNTO_COTIZACION"
                    : tipoDocumento.trim().toUpperCase());
            d.setNombreArchivo(original);
            d.setRutaArchivo(destino.toString());
            d.setVersion(1);
            d.setEstado("Pendiente");
            Documento guardado = documentoRepository.save(d);
            historialService.registrar(usuario.getIdUsuario(), "DOCUMENTO", guardado.getIdDocumento(),
                    "CREAR", null, "Adjunto de cotización " + idCotizacion + ": " + original);
            return guardado;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Documento> listarCotizacion(Long idCotizacion) {
        return documentoRepository.findByCotizacion_IdCotizacionOrderByFechaCargaDesc(idCotizacion);
    }

    @Transactional(readOnly = true)
    public Documento obtener(Long idDocumento) {
        return documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new IllegalArgumentException("El documento indicado no existe."));
    }

    /** Id del usuario dueño del documento (para validar el acceso a la descarga). */
    @Transactional(readOnly = true)
    public Long duenio(Long idDocumento) {
        return obtener(idDocumento).getUsuario().getIdUsuario();
    }

    private String extension(String nombre) {
        int i = nombre.lastIndexOf('.');
        return i < 0 ? "" : nombre.substring(i).toLowerCase();
    }
}

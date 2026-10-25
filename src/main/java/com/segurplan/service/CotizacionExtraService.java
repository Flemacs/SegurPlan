package com.segurplan.service;

import com.segurplan.model.ComunicacionAseguradora;
import com.segurplan.model.Cotizacion;
import com.segurplan.model.Usuario;
import com.segurplan.repository.ComunicacionAseguradoraRepository;
import com.segurplan.repository.CotizacionRepository;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** Comunicación del asesor con la aseguradora (RF005). */
@Service
public class CotizacionExtraService {

    private static final Set<String> MEDIOS = Set.of("Correo", "Teléfono", "Portal", "Reunión", "Otro");

    private final ComunicacionAseguradoraRepository comunicacionRepository;
    private final CotizacionRepository cotizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialService historialService;

    public CotizacionExtraService(ComunicacionAseguradoraRepository comunicacionRepository,
                                  CotizacionRepository cotizacionRepository,
                                  UsuarioRepository usuarioRepository,
                                  HistorialService historialService) {
        this.comunicacionRepository = comunicacionRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialService = historialService;
    }

    @Transactional
    public ComunicacionAseguradora registrar(String correoAsesor, Long idCotizacion,
                                             String medio, String contacto, String observaciones) {
        if (observaciones == null || observaciones.isBlank()) {
            throw new IllegalArgumentException("Describa la comunicación realizada con la aseguradora.");
        }
        if (medio == null || !MEDIOS.contains(medio)) {
            medio = "Otro";
        }
        Usuario asesor = usuarioRepository.findByCorreo(correoAsesor)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        Cotizacion cot = cotizacionRepository.findById(idCotizacion)
                .orElseThrow(() -> new IllegalArgumentException("La cotización indicada no existe."));

        ComunicacionAseguradora c = new ComunicacionAseguradora();
        c.setCotizacion(cot);
        c.setAsesor(asesor);
        c.setMedio(medio);
        c.setContacto(contacto == null || contacto.isBlank() ? null : contacto.trim());
        c.setObservaciones(observaciones.trim());
        ComunicacionAseguradora guardada = comunicacionRepository.save(c);

        historialService.registrar(asesor.getIdUsuario(), "COTIZACION", idCotizacion,
                "COMUNICACION_ASEGURADORA", null, medio + ": " + observaciones.trim());
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<ComunicacionAseguradora> listar(Long idCotizacion) {
        return comunicacionRepository.findByCotizacion_IdCotizacionOrderByFechaDesc(idCotizacion);
    }
}

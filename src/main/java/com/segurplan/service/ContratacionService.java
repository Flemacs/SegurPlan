/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.segurplan.service;

import com.segurplan.dto.ContratacionRequest;
import com.segurplan.model.Contratacion;
import com.segurplan.model.Cotizacion;
import com.segurplan.model.Usuario;
import com.segurplan.repository.ContratacionRepository;
import com.segurplan.repository.CotizacionRepository;
import com.segurplan.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContratacionService {

    private final ContratacionRepository contratacionRepository;
    private final CotizacionRepository cotizacionRepository;
    private final UsuarioRepository usuarioRepository;

    public ContratacionService(
            ContratacionRepository contratacionRepository,
            CotizacionRepository cotizacionRepository,
            UsuarioRepository usuarioRepository) {

        this.contratacionRepository = contratacionRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Contratacion crear(ContratacionRequest request) {

        Cotizacion cotizacion =
                cotizacionRepository
                        .findById(request.getIdCotizacion())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cotización no encontrada"
                                ));

        Usuario usuario =
                usuarioRepository
                        .findById(request.getIdUsuario())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Usuario no encontrado"
                                ));

        if (!cotizacion.getUsuario()
                .getIdUsuario()
                .equals(usuario.getIdUsuario())) {

            throw new RuntimeException(
                    "La cotización no pertenece al usuario"
            );
        }

        if (!"Disponible".equals(cotizacion.getEstado())) {

            throw new RuntimeException(
                    "La cotización no está disponible para contratación"
            );
        }

        if (!Boolean.TRUE.equals(
                request.getAceptaCondiciones())) {

            throw new RuntimeException(
                    "Debe aceptar las condiciones"
            );
        }

        if (contratacionRepository
                .findByCotizacion_IdCotizacion(
                        cotizacion.getIdCotizacion())
                .isPresent()) {

            throw new RuntimeException(
                    "Esta cotización ya tiene una contratación"
            );
        }

        Contratacion contratacion =
                new Contratacion();

        contratacion.setCotizacion(cotizacion);
        contratacion.setUsuario(usuario);

        contratacion.setVersionCondiciones(
                "1.0"
        );

        contratacion.setCondicionesAceptadas(
                true
        );

        contratacion.setFechaAceptacion(
                LocalDateTime.now()
        );

        contratacion.setEstado(
                "Iniciada"
        );

        Contratacion guardada =
                contratacionRepository.save(
                        contratacion
                );

        cotizacion.setEstado(
                "Aceptada"
        );

        cotizacionRepository.save(
                cotizacion
        );

        return guardada;
    }
    public List<Map<String, Object>> listarPendientes() {
    return contratacionRepository.findPendientesDePoliza()
        .stream()
        .map(c -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idContratacion", c.getIdContratacion());
            item.put("idCotizacion", c.getCotizacion().getIdCotizacion());
            item.put("cliente", c.getUsuario().getNombres() + " " +
                c.getUsuario().getApellidos());
            item.put("tipoSeguro", c.getCotizacion().getTipoSeguro().getNombre());
            item.put("aseguradora", c.getCotizacion().getAseguradora().getNombre());
            item.put("fechaContratacion", c.getFechaContratacion());
            return item;
        })
        .toList();
}
}
package com.segurplan.service;

import com.segurplan.dto.PolizaRequest;
import com.segurplan.dto.PolizaResumenDTO;
import com.segurplan.model.Contratacion;
import com.segurplan.model.Poliza;
import com.segurplan.repository.ContratacionRepository;
import com.segurplan.repository.PolizaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final ContratacionRepository contratacionRepository;

    // Servicio para registrar la trazabilidad
    private final HistorialService historialService;

    public PolizaService(
            PolizaRepository polizaRepository,
            ContratacionRepository contratacionRepository,
            HistorialService historialService) {

        this.polizaRepository = polizaRepository;
        this.contratacionRepository = contratacionRepository;
        this.historialService = historialService;
    }

    // =========================================================
    // GENERAR PÓLIZA
    // =========================================================
    @Transactional
    public Poliza generar(PolizaRequest request) {

        if (request.getIdContratacion() == null) {
            throw new IllegalArgumentException(
                    "Seleccione una contratación."
            );
        }

        Contratacion contratacion = contratacionRepository
                .findById(request.getIdContratacion())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Contratación no encontrada."
                        )
                );

        // -----------------------------------------------------
        // VALIDAR COTIZACIÓN
        // -----------------------------------------------------
        if (!"Aceptada".equals(
                contratacion
                        .getCotizacion()
                        .getEstado())) {

            throw new IllegalArgumentException(
                    "La cotización todavía no está aceptada."
            );
        }

        // -----------------------------------------------------
        // VALIDAR CONDICIONES
        // -----------------------------------------------------
        if (!Boolean.TRUE.equals(
                contratacion.getCondicionesAceptadas())) {

            throw new IllegalArgumentException(
                    "El cliente no ha aceptado las condiciones."
            );
        }

        // -----------------------------------------------------
        // VALIDAR QUE NO EXISTA OTRA PÓLIZA
        // -----------------------------------------------------
        if (polizaRepository
                .findByContratacion_IdContratacion(
                        contratacion.getIdContratacion())
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Esta contratación ya tiene una póliza."
            );
        }

        // -----------------------------------------------------
        // VALIDAR FECHAS
        // -----------------------------------------------------
        if (request.getFechaInicio() == null
                || request.getFechaFin() == null
                || request.getFechaFin()
                        .isBefore(request.getFechaInicio())) {

            throw new IllegalArgumentException(
                    "La fecha de fin debe ser posterior al inicio."
            );
        }

        // -----------------------------------------------------
        // VALIDAR PRIMA
        // -----------------------------------------------------
        if (request.getPrima() == null
                || request.getPrima()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Ingrese una prima mayor que cero."
            );
        }

        // -----------------------------------------------------
        // VALIDAR COBERTURA
        // -----------------------------------------------------
        if (request.getCobertura() == null
                || request.getCobertura().isBlank()) {

            throw new IllegalArgumentException(
                    "La cobertura es obligatoria."
            );
        }

        // -----------------------------------------------------
        // VALIDAR DEDUCIBLE
        // -----------------------------------------------------
        if (request.getDeducible() != null
                && request.getDeducible()
                        .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "El deducible no puede ser negativo."
            );
        }

        // =====================================================
        // CREAR PÓLIZA
        // =====================================================
        Poliza poliza = new Poliza();

        poliza.setContratacion(contratacion);
        poliza.setFechaInicio(request.getFechaInicio());
        poliza.setFechaFin(request.getFechaFin());
        poliza.setPrima(request.getPrima());
        poliza.setCobertura(request.getCobertura());
        poliza.setDeducible(request.getDeducible());
        poliza.setCondiciones(request.getCondiciones());
        poliza.setEstado("Recibida");

        // Número temporal hasta obtener el ID generado
        // por PostgreSQL.
        poliza.setNumeroPoliza(
                "TMP-" + java.util.UUID.randomUUID()
        );

        // Guardamos para obtener idPoliza
        poliza = polizaRepository.saveAndFlush(poliza);

        // =====================================================
        // GENERAR NÚMERO DEFINITIVO
        // =====================================================
        poliza.setNumeroPoliza(
                "POL-" +
                String.format(
                        "%08d",
                        poliza.getIdPoliza()
                )
        );

        poliza = polizaRepository.save(poliza);

        // =====================================================
        // REGISTRAR HISTORIAL / TRAZABILIDAD
        // =====================================================

        Long idUsuario = null;

        /*
         * La póliza pertenece a una contratación,
         * la contratación pertenece a una cotización,
         * y la cotización pertenece a un usuario.
         */
        if (contratacion.getCotizacion() != null
                && contratacion
                        .getCotizacion()
                        .getUsuario() != null) {

            idUsuario = contratacion
                    .getCotizacion()
                    .getUsuario()
                    .getIdUsuario();
        }

        historialService.registrar(
                idUsuario,
                "POLIZA",
                poliza.getIdPoliza(),
                "CREAR",
                null,
                "Póliza "
                        + poliza.getNumeroPoliza()
                        + " generada"
                        + " | Estado: "
                        + poliza.getEstado()
                        + " | Prima: S/ "
                        + poliza.getPrima()
        );

        return poliza;
    }

    // =========================================================
    // LISTAR PÓLIZAS POR USUARIO
    // =========================================================
    @Transactional(readOnly = true)
    public List<PolizaResumenDTO> listarPorUsuario(
            Long idUsuario) {

        if (idUsuario == null || idUsuario <= 0) {

            throw new IllegalArgumentException(
                    "El identificador del usuario no es válido."
            );
        }

        return polizaRepository
                .findByUsuario(idUsuario)
                .stream()
                .map(p ->
                        new PolizaResumenDTO(
                                p.getIdPoliza(),
                                p.getNumeroPoliza(),
                                p.getContratacion()
                                        .getIdContratacion(),
                                p.getFechaInicio(),
                                p.getFechaFin(),
                                p.getPrima(),
                                p.getCobertura(),
                                p.getDeducible(),
                                p.getCondiciones(),
                                p.getEstado()
                        )
                )
                .toList();
    }
}
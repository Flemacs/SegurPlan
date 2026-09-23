/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.segurplan.service;

import com.segurplan.dto.PolizaRequest;
import com.segurplan.model.Contratacion;
import com.segurplan.model.Poliza;
import com.segurplan.repository.ContratacionRepository;
import com.segurplan.repository.PolizaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class PolizaService {
    private final PolizaRepository polizaRepository;
    private final ContratacionRepository contratacionRepository;

    public PolizaService(PolizaRepository polizaRepository,
                         ContratacionRepository contratacionRepository) {
        this.polizaRepository = polizaRepository;
        this.contratacionRepository = contratacionRepository;
    }

    @Transactional
    public Poliza generar(PolizaRequest request) {
        if (request.getIdContratacion() == null) {
            throw new IllegalArgumentException("Seleccione una contratación.");
        }

        Contratacion contratacion = contratacionRepository
                .findById(request.getIdContratacion())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contratación no encontrada."));

        if (!"Aceptada".equals(
                contratacion.getCotizacion().getEstado())) {
            throw new IllegalArgumentException(
                    "La cotización todavía no está aceptada.");
        }

        if (!Boolean.TRUE.equals(
                contratacion.getCondicionesAceptadas())) {
            throw new IllegalArgumentException(
                    "El cliente no ha aceptado las condiciones.");
        }

        if (polizaRepository.findByContratacion_IdContratacion(
                contratacion.getIdContratacion()).isPresent()) {
            throw new IllegalArgumentException(
                    "Esta contratación ya tiene una póliza.");
        }

        if (request.getFechaInicio() == null ||
            request.getFechaFin() == null ||
            request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new IllegalArgumentException(
                    "La fecha de fin debe ser posterior al inicio.");
        }

        if (request.getPrima() == null ||
            request.getPrima().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Ingrese una prima mayor que cero.");
        }

        if (request.getCobertura() == null ||
            request.getCobertura().isBlank()) {
            throw new IllegalArgumentException(
                    "La cobertura es obligatoria.");
        }

        if (request.getDeducible() != null &&
            request.getDeducible().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El deducible no puede ser negativo.");
        }

        Poliza poliza = new Poliza();
        poliza.setContratacion(contratacion);
        poliza.setFechaInicio(request.getFechaInicio());
        poliza.setFechaFin(request.getFechaFin());
        poliza.setPrima(request.getPrima());
        poliza.setCobertura(request.getCobertura());
        poliza.setDeducible(request.getDeducible());
        poliza.setCondiciones(request.getCondiciones());
        poliza.setEstado("Recibida");

        // Número temporal único hasta obtener el ID de PostgreSQL.
        poliza.setNumeroPoliza(
                "TMP-" + java.util.UUID.randomUUID());

        poliza = polizaRepository.saveAndFlush(poliza);

        poliza.setNumeroPoliza(
                "POL-" + String.format("%08d", poliza.getIdPoliza()));

        return polizaRepository.save(poliza);
    }
}
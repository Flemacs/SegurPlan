package com.segurplan.dto;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PolizaResumenDTO(
        Long idPoliza,
        String numeroPoliza,
        Long idContratacion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        BigDecimal prima,
        String cobertura,
        BigDecimal deducible,
        String condiciones,
        String estado
) {
}
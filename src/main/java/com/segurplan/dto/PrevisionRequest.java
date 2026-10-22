package com.segurplan.dto;

import java.math.BigDecimal;

public class PrevisionRequest {

    private Integer edad;
    private BigDecimal ingresoMensual;
    private Integer aniosAporte;
    private String consulta;

    public PrevisionRequest() {
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public BigDecimal getIngresoMensual() {
        return ingresoMensual;
    }

    public void setIngresoMensual(BigDecimal ingresoMensual) {
        this.ingresoMensual = ingresoMensual;
    }

    public Integer getAniosAporte() {
        return aniosAporte;
    }

    public void setAniosAporte(Integer aniosAporte) {
        this.aniosAporte = aniosAporte;
    }

    public String getConsulta() {
        return consulta;
    }

    public void setConsulta(String consulta) {
        this.consulta = consulta;
    }
}
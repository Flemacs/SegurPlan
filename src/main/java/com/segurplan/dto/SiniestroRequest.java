package com.segurplan.dto;

import java.time.LocalDate;

public class SiniestroRequest {

    private String tipoSiniestro;
    private LocalDate fechaAccidente;

    private String placa;
    private String marca;
    private String modelo;
    private Integer anio;
    
    private String lugar;
    private String descripcion;
    private String observaciones;
    private Integer idAseguradora;

    public SiniestroRequest() {
    }

    public Integer getIdAseguradora() {
        return idAseguradora;
    }

    public void setIdAseguradora(Integer idAseguradora) {
        this.idAseguradora = idAseguradora;
    }

    public String getTipoSiniestro() {
        return tipoSiniestro;
    }

    public void setTipoSiniestro(String tipoSiniestro) {
        this.tipoSiniestro = tipoSiniestro;
    }

    public LocalDate getFechaAccidente() {
        return fechaAccidente;
    }

    public void setFechaAccidente(LocalDate fechaAccidente) {
        this.fechaAccidente = fechaAccidente;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

package com.segurplan.dto;

/** DTO de entrada para activar o desactivar un registro de catálogo. */
public class CambiarEstadoRequest {

    private String estado;

    public CambiarEstadoRequest() {
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}

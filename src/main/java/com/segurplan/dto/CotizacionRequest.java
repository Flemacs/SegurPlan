/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.segurplan.dto;

public class CotizacionRequest {

    private Long idUsuario;
    private Integer idTipoSeguro;
    private Integer idAseguradora;
    private String descripcionSolicitud;

    public CotizacionRequest() {
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getIdTipoSeguro() {
        return idTipoSeguro;
    }

    public void setIdTipoSeguro(Integer idTipoSeguro) {
        this.idTipoSeguro = idTipoSeguro;
    }

    public Integer getIdAseguradora() {
        return idAseguradora;
    }

    public void setIdAseguradora(Integer idAseguradora) {
        this.idAseguradora = idAseguradora;
    }

    public String getDescripcionSolicitud() {
        return descripcionSolicitud;
    }

    public void setDescripcionSolicitud(String descripcionSolicitud) {
        this.descripcionSolicitud = descripcionSolicitud;
    }
}
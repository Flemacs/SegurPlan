/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.segurplan.dto;

public class ContratacionRequest {

    private Long idCotizacion;
    private Long idUsuario;
    private Boolean aceptaCondiciones;

    public ContratacionRequest() {
    }

    public Long getIdCotizacion() {
        return idCotizacion;
    }

    public void setIdCotizacion(Long idCotizacion) {
        this.idCotizacion = idCotizacion;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Boolean getAceptaCondiciones() {
        return aceptaCondiciones;
    }

    public void setAceptaCondiciones(Boolean aceptaCondiciones) {
        this.aceptaCondiciones = aceptaCondiciones;
    }
}
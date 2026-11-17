package com.segurplan.dto;

import java.time.LocalDateTime;

public class HistorialDTO {

    private Long idHistorial;

    private Long idUsuario;
    private String usuario;
    private String correoUsuario;

    private String entidad;
    private Long idRegistro;

    private String accion;

    private String valorAnterior;
    private String valorNuevo;

    private LocalDateTime fechaHora;

    public HistorialDTO() {
    }

    public HistorialDTO(
            Long idHistorial,
            Long idUsuario,
            String usuario,
            String correoUsuario,
            String entidad,
            Long idRegistro,
            String accion,
            String valorAnterior,
            String valorNuevo,
            LocalDateTime fechaHora) {

        this.idHistorial = idHistorial;
        this.idUsuario = idUsuario;
        this.usuario = usuario;
        this.correoUsuario = correoUsuario;
        this.entidad = entidad;
        this.idRegistro = idRegistro;
        this.accion = accion;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.fechaHora = fechaHora;
    }

    public Long getIdHistorial() {
        return idHistorial;
    }

    public void setIdHistorial(Long idHistorial) {
        this.idHistorial = idHistorial;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getCorreoUsuario() {
        return correoUsuario;
    }

    public void setCorreoUsuario(String correoUsuario) {
        this.correoUsuario = correoUsuario;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public Long getIdRegistro() {
        return idRegistro;
    }

    public void setIdRegistro(Long idRegistro) {
        this.idRegistro = idRegistro;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(String valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public void setValorNuevo(String valorNuevo) {
        this.valorNuevo = valorNuevo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
}
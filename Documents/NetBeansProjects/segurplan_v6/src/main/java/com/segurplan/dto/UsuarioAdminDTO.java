package com.segurplan.dto;

import java.time.LocalDateTime;

public class UsuarioAdminDTO {

    private Long idUsuario;
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;
    private String estado;
    private LocalDateTime fechaRegistro;
    private String rol;

    public UsuarioAdminDTO() {
    }

    public UsuarioAdminDTO(
            Long idUsuario,
            String nombres,
            String apellidos,
            String correo,
            String telefono,
            String estado,
            LocalDateTime fechaRegistro,
            String rol) {

        this.idUsuario = idUsuario;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.telefono = telefono;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
        this.rol = rol;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
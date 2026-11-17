package com.segurplan.dto;

import java.util.List;

public class RolAdminDTO {

    private Integer idRol;
    private String nombre;
    private String descripcion;
    private long cantidadUsuarios;
    private List<String> permisos;

    public RolAdminDTO() {
    }

    public RolAdminDTO(
            Integer idRol,
            String nombre,
            String descripcion,
            long cantidadUsuarios,
            List<String> permisos) {

        this.idRol = idRol;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cantidadUsuarios = cantidadUsuarios;
        this.permisos = permisos;
    }

    public Integer getIdRol() {
        return idRol;
    }

    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public long getCantidadUsuarios() {
        return cantidadUsuarios;
    }

    public void setCantidadUsuarios(long cantidadUsuarios) {
        this.cantidadUsuarios = cantidadUsuarios;
    }

    public List<String> getPermisos() {
        return permisos;
    }

    public void setPermisos(List<String> permisos) {
        this.permisos = permisos;
    }
}
package com.segurplan.dto;

/** DTO de entrada para el mantenimiento de tipos de seguro. */
public class TipoSeguroRequest {

    private String nombre;
    private String descripcion;

    public TipoSeguroRequest() {
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}

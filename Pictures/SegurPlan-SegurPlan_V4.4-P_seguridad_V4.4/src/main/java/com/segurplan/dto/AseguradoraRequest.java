package com.segurplan.dto;

/** DTO de entrada para el mantenimiento de aseguradoras. */
public class AseguradoraRequest {

    private String nombre;
    private String ruc;
    private String telefono;
    private String correo;

    public AseguradoraRequest() {
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
}

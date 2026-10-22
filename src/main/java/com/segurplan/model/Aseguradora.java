package com.segurplan.model;

import jakarta.persistence.*;

@Entity
@Table(name = "aseguradora")
public class Aseguradora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aseguradora")
    private Integer idAseguradora;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "ruc", unique = true, length = 20)
    private String ruc;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    public Aseguradora() {
    }

    @PrePersist
    public void prePersist() {
        if (estado == null) {
            estado = "Activo";
        }
    }

    public Integer getIdAseguradora() {
        return idAseguradora;
    }

    public void setIdAseguradora(Integer idAseguradora) {
        this.idAseguradora = idAseguradora;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
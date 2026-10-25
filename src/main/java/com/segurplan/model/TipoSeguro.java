package com.segurplan.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tipo_seguro")
public class TipoSeguro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_seguro")
    private Integer idTipoSeguro;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    public TipoSeguro() {
    }

    @PrePersist
    public void prePersist() {
        if (estado == null) {
            estado = "Activo";
        }
    }

    public Integer getIdTipoSeguro() {
        return idTipoSeguro;
    }

    public void setIdTipoSeguro(Integer idTipoSeguro) {
        this.idTipoSeguro = idTipoSeguro;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
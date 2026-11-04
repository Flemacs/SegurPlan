/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.segurplan.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contratacion")
public class Contratacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contratacion")
    private Long idContratacion;

    @OneToOne
    @JoinColumn(name = "id_cotizacion", nullable = false)
    private Cotizacion cotizacion;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_contratacion", nullable = false)
    private LocalDateTime fechaContratacion;

    @Column(name = "version_condiciones")
    private String versionCondiciones;

    @Column(name = "condiciones_aceptadas", nullable = false)
    private Boolean condicionesAceptadas;

    @Column(name = "fecha_aceptacion")
    private LocalDateTime fechaAceptacion;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "observaciones")
    private String observaciones;

    public Contratacion() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaContratacion == null) {
            fechaContratacion = LocalDateTime.now();
        }

        if (condicionesAceptadas == null) {
            condicionesAceptadas = false;
        }

        if (estado == null) {
            estado = "Iniciada";
        }
    }

    public Long getIdContratacion() {
        return idContratacion;
    }

    public void setIdContratacion(Long idContratacion) {
        this.idContratacion = idContratacion;
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(Cotizacion cotizacion) {
        this.cotizacion = cotizacion;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaContratacion() {
        return fechaContratacion;
    }

    public void setFechaContratacion(LocalDateTime fechaContratacion) {
        this.fechaContratacion = fechaContratacion;
    }

    public String getVersionCondiciones() {
        return versionCondiciones;
    }

    public void setVersionCondiciones(String versionCondiciones) {
        this.versionCondiciones = versionCondiciones;
    }

    public Boolean getCondicionesAceptadas() {
        return condicionesAceptadas;
    }

    public void setCondicionesAceptadas(Boolean condicionesAceptadas) {
        this.condicionesAceptadas = condicionesAceptadas;
    }

    public LocalDateTime getFechaAceptacion() {
        return fechaAceptacion;
    }

    public void setFechaAceptacion(LocalDateTime fechaAceptacion) {
        this.fechaAceptacion = fechaAceptacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
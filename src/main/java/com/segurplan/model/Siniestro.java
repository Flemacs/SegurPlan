package com.segurplan.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "siniestro")
public class Siniestro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_siniestro")
    private Long idSiniestro;


    // =====================================================
    // USUARIO
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;


    // =====================================================
    // ASEGURADORA
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aseguradora")
    private Aseguradora aseguradora;


    // =====================================================
    // DATOS DEL SINIESTRO
    // =====================================================

    @Column(
            name = "numero_siniestro",
            nullable = false,
            unique = true
    )
    private String numeroSiniestro;


    @Column(
            name = "fecha_registro",
            nullable = false
    )
    private LocalDateTime fechaRegistro;


    @Column(
            name = "fecha_accidente",
            nullable = false
    )
    private LocalDateTime fechaAccidente;


    // =====================================================
    // VEHÍCULO
    // =====================================================

    @Column(name = "placa")
    private String placa;


    @Column(name = "marca")
    private String marca;


    @Column(name = "modelo")
    private String modelo;


    @Column(name = "anio")
    private Integer anio;


    // =====================================================
    // ACCIDENTE
    // =====================================================

    @Column(name = "lugar")
    private String lugar;


    @Column(
            name = "descripcion",
            columnDefinition = "TEXT"
    )
    private String descripcion;


    // =====================================================
    // EVALUACIÓN
    // =====================================================

    @Column(
            name = "resultado_evaluacion",
            columnDefinition = "TEXT"
    )
    private String resultadoEvaluacion;


    @Column(
            name = "observaciones",
            columnDefinition = "TEXT"
    )
    private String observaciones;


    @Column(
            name = "estado",
            nullable = false
    )
    private String estado;


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    public void prePersist() {

        if (fechaRegistro == null) {
            fechaRegistro =
                    LocalDateTime.now();
        }

        if (estado == null ||
                estado.isBlank()) {

            estado =
                    "Registrado";
        }
    }


    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public Long getIdSiniestro() {
        return idSiniestro;
    }

    public void setIdSiniestro(
            Long idSiniestro) {

        this.idSiniestro =
                idSiniestro;
    }


    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(
            Usuario usuario) {

        this.usuario =
                usuario;
    }


    public Aseguradora getAseguradora() {
        return aseguradora;
    }

    public void setAseguradora(
            Aseguradora aseguradora) {

        this.aseguradora =
                aseguradora;
    }


    public String getNumeroSiniestro() {
        return numeroSiniestro;
    }

    public void setNumeroSiniestro(
            String numeroSiniestro) {

        this.numeroSiniestro =
                numeroSiniestro;
    }


    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(
            LocalDateTime fechaRegistro) {

        this.fechaRegistro =
                fechaRegistro;
    }


    public LocalDateTime getFechaAccidente() {
        return fechaAccidente;
    }

    public void setFechaAccidente(
            LocalDateTime fechaAccidente) {

        this.fechaAccidente =
                fechaAccidente;
    }


    public String getPlaca() {
        return placa;
    }

    public void setPlaca(
            String placa) {

        this.placa =
                placa;
    }


    public String getMarca() {
        return marca;
    }

    public void setMarca(
            String marca) {

        this.marca =
                marca;
    }


    public String getModelo() {
        return modelo;
    }

    public void setModelo(
            String modelo) {

        this.modelo =
                modelo;
    }


    public Integer getAnio() {
        return anio;
    }

    public void setAnio(
            Integer anio) {

        this.anio =
                anio;
    }


    public String getLugar() {
        return lugar;
    }

    public void setLugar(
            String lugar) {

        this.lugar =
                lugar;
    }


    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(
            String descripcion) {

        this.descripcion =
                descripcion;
    }


    public String getResultadoEvaluacion() {
        return resultadoEvaluacion;
    }

    public void setResultadoEvaluacion(
            String resultadoEvaluacion) {

        this.resultadoEvaluacion =
                resultadoEvaluacion;
    }


    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(
            String observaciones) {

        this.observaciones =
                observaciones;
    }


    public String getEstado() {
        return estado;
    }

    public void setEstado(
            String estado) {

        this.estado =
                estado;
    }
}
package com.segurplan.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prevision")
public class Prevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prevision")
    private Long idPrevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo_sistema", nullable = false, length = 20)
    private String tipoSistema;

    @Column(name = "edad")
    private Integer edad;

    @Column(name = "ingreso_mensual", precision = 12, scale = 2)
    private BigDecimal ingresoMensual;

    @Column(name = "anios_aporte")
    private Integer aniosAporte;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "monto_pension_estimado", precision = 12, scale = 2)
    private BigDecimal montoPensionEstimado;

    @Column(name = "resultado_simulacion", columnDefinition = "TEXT")
    private String resultadoSimulacion;

    @Column(name = "consulta", columnDefinition = "TEXT")
    private String consulta;

    @Column(name = "respuesta", columnDefinition = "TEXT")
    private String respuesta;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    public Prevision() {
    }

    @PrePersist
    public void prePersist() {

        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }

        if (estado == null || estado.isBlank()) {
            estado = "Registrada";
        }

        if (tipoSistema == null || tipoSistema.isBlank()) {
            tipoSistema = "AFP";
        }
    }

    public Long getIdPrevision() {
        return idPrevision;
    }

    public void setIdPrevision(Long idPrevision) {
        this.idPrevision = idPrevision;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getTipoSistema() {
        return tipoSistema;
    }

    public void setTipoSistema(String tipoSistema) {
        this.tipoSistema = tipoSistema;
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public BigDecimal getIngresoMensual() {
        return ingresoMensual;
    }

    public void setIngresoMensual(BigDecimal ingresoMensual) {
        this.ingresoMensual = ingresoMensual;
    }

    public Integer getAniosAporte() {
        return aniosAporte;
    }

    public void setAniosAporte(Integer aniosAporte) {
        this.aniosAporte = aniosAporte;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public BigDecimal getMontoPensionEstimado() {
        return montoPensionEstimado;
    }

    public void setMontoPensionEstimado(BigDecimal montoPensionEstimado) {
        this.montoPensionEstimado = montoPensionEstimado;
    }

    public String getResultadoSimulacion() {
        return resultadoSimulacion;
    }

    public void setResultadoSimulacion(String resultadoSimulacion) {
        this.resultadoSimulacion = resultadoSimulacion;
    }

    public String getConsulta() {
        return consulta;
    }

    public void setConsulta(String consulta) {
        this.consulta = consulta;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
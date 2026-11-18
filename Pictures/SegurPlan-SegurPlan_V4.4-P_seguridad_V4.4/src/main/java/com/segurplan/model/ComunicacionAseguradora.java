package com.segurplan.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Registro de la comunicación del asesor con la aseguradora (RF005). */
@Entity
@Table(name = "comunicacion_aseguradora")
public class ComunicacionAseguradora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comunicacion")
    private Long idComunicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cotizacion", nullable = false)
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_asesor", nullable = false)
    private Usuario asesor;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "medio", nullable = false, length = 30)
    private String medio;

    @Column(name = "contacto", length = 150)
    private String contacto;

    @Column(name = "observaciones", nullable = false, columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    public void prePersist() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public Long getIdComunicacion() { return idComunicacion; }
    public Cotizacion getCotizacion() { return cotizacion; }
    public void setCotizacion(Cotizacion cotizacion) { this.cotizacion = cotizacion; }
    public Usuario getAsesor() { return asesor; }
    public void setAsesor(Usuario asesor) { this.asesor = asesor; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getMedio() { return medio; }
    public void setMedio(String medio) { this.medio = medio; }
    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}

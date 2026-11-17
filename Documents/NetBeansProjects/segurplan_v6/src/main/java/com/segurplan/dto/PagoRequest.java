package com.segurplan.dto;

public class PagoRequest {

    private Long idCotizacion;
    private Boolean aceptaCondiciones;

    // TARJETA (por defecto), YAPE o PLIN
    private String metodo;

    // Billetera digital: celular desde el que se paga (9 dígitos)
    private String celular;

    // Datos de la tarjeta (solo para la simulación; no se almacenan completos)
    private String numeroTarjeta;
    private String titular;
    private String vencimiento; // MM/AA
    private String cvv;

    public PagoRequest() {
    }

    public Long getIdCotizacion() { return idCotizacion; }
    public void setIdCotizacion(Long idCotizacion) { this.idCotizacion = idCotizacion; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public Boolean getAceptaCondiciones() { return aceptaCondiciones; }
    public void setAceptaCondiciones(Boolean aceptaCondiciones) { this.aceptaCondiciones = aceptaCondiciones; }

    public String getNumeroTarjeta() { return numeroTarjeta; }
    public void setNumeroTarjeta(String numeroTarjeta) { this.numeroTarjeta = numeroTarjeta; }

    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }

    public String getVencimiento() { return vencimiento; }
    public void setVencimiento(String vencimiento) { this.vencimiento = vencimiento; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
}

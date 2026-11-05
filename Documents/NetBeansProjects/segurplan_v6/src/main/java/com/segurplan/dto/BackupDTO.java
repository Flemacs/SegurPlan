package com.segurplan.dto;

import java.time.LocalDateTime;

/** DTO de salida con los datos de un archivo de respaldo. */
public class BackupDTO {

    private String nombre;
    private long tamanoBytes;
    private LocalDateTime fecha;

    public BackupDTO() {
    }

    public BackupDTO(String nombre, long tamanoBytes, LocalDateTime fecha) {
        this.nombre = nombre;
        this.tamanoBytes = tamanoBytes;
        this.fecha = fecha;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}

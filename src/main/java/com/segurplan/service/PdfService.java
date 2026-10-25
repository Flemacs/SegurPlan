package com.segurplan.service;

import com.segurplan.dto.HistorialDTO;
import com.segurplan.model.Cotizacion;
import com.segurplan.model.Poliza;
import com.segurplan.model.Siniestro;
import com.segurplan.util.SimplePdf;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Documentos PDF descargables (RF006, RF008, RF024). */
@Service
public class PdfService {

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String PIE = "SEGURPLAN - Plataforma de seguros y AFP";

    @Transactional(readOnly = true)
    public byte[] cotizacion(Cotizacion c) {
        SimplePdf pdf = new SimplePdf()
                .titulo("Cotizacion N° " + c.getIdCotizacion())
                .campo("Estado", c.getEstado())
                .campo("Fecha de solicitud", c.getFechaSolicitud() == null ? null
                        : c.getFechaSolicitud().format(FECHA_HORA))
                .espacio()
                .subtitulo("Cliente")
                .campo("Nombre", c.getUsuario().getNombres() + " " + c.getUsuario().getApellidos())
                .campo("Correo", c.getUsuario().getCorreo())
                .espacio()
                .subtitulo("Seguro solicitado")
                .campo("Tipo de seguro", c.getTipoSeguro() == null ? null : c.getTipoSeguro().getNombre())
                .campo("Aseguradora", c.getAseguradora() == null ? null : c.getAseguradora().getNombre())
                .campo("Descripcion", c.getDescripcionSolicitud())
                .espacio()
                .subtitulo("Respuesta de la aseguradora")
                .campo("Prima / precio (S/)", c.getPrecio())
                .campo("Deducible (S/)", c.getDeducible())
                .campo("Cobertura", c.getCobertura())
                .campo("Vigencia hasta", c.getVigencia())
                .campo("Fecha de respuesta", c.getFechaRespuesta() == null ? null
                        : c.getFechaRespuesta().format(FECHA_HORA));
        return pdf.generar(PIE);
    }

    @Transactional(readOnly = true)
    public byte[] poliza(Poliza p) {
        var cot = p.getContratacion().getCotizacion();
        SimplePdf pdf = new SimplePdf()
                .titulo("Poliza " + p.getNumeroPoliza())
                .campo("Estado", p.getEstado())
                .campo("Vigencia", p.getFechaInicio() + " al " + p.getFechaFin())
                .espacio()
                .subtitulo("Asegurado")
                .campo("Nombre", p.getContratacion().getUsuario().getNombres() + " "
                        + p.getContratacion().getUsuario().getApellidos())
                .campo("Correo", p.getContratacion().getUsuario().getCorreo())
                .espacio()
                .subtitulo("Seguro contratado")
                .campo("Tipo de seguro", cot.getTipoSeguro() == null ? null : cot.getTipoSeguro().getNombre())
                .campo("Aseguradora", cot.getAseguradora() == null ? null : cot.getAseguradora().getNombre())
                .campo("Prima (S/)", p.getPrima())
                .campo("Deducible (S/)", p.getDeducible())
                .campo("Cobertura", p.getCobertura())
                .espacio()
                .subtitulo("Condiciones")
                .parrafo(p.getCondiciones() == null ? "-" : p.getCondiciones());
        return pdf.generar(PIE);
    }

    @Transactional(readOnly = true)
    public byte[] resumenSiniestro(Siniestro s, List<HistorialDTO> historial) {
        SimplePdf pdf = new SimplePdf()
                .titulo("Resumen de siniestro " + s.getNumeroSiniestro())
                .campo("Estado", s.getEstado())
                .campo("Registrado", s.getFechaRegistro() == null ? null : s.getFechaRegistro().format(FECHA_HORA))
                .campo("Fecha del accidente", s.getFechaAccidente() == null ? null
                        : s.getFechaAccidente().format(FECHA_HORA))
                .espacio()
                .subtitulo("Asegurado")
                .campo("Nombre", s.getUsuario().getNombres() + " " + s.getUsuario().getApellidos())
                .campo("Correo", s.getUsuario().getCorreo())
                .campo("Telefono", s.getUsuario().getTelefono())
                .espacio()
                .subtitulo("Vehiculo")
                .campo("Placa", s.getPlaca())
                .campo("Marca / modelo / anio", s.getMarca() + " / " + s.getModelo() + " / " + s.getAnio())
                .espacio()
                .subtitulo("Accidente")
                .campo("Lugar", s.getLugar())
                .campo("Descripcion", s.getDescripcion())
                .campo("Aseguradora", s.getAseguradora() == null ? null : s.getAseguradora().getNombre())
                .espacio()
                .subtitulo("Evaluacion de la aseguradora")
                .campo("Resultado", s.getResultadoEvaluacion())
                .campo("Monto aprobado (S/)", s.getMontoAprobado())
                .espacio()
                .subtitulo("Historial del caso");
        for (HistorialDTO h : historial) {
            pdf.parrafo((h.getFechaHora() == null ? "" : h.getFechaHora().format(FECHA_HORA)) + " | "
                    + h.getAccion() + " | " + (h.getUsuario() == null ? "" : h.getUsuario()) + " | "
                    + (h.getValorNuevo() == null ? "" : h.getValorNuevo()));
        }
        return pdf.generar(PIE);
    }
}

package com.segurplan.service;

import com.segurplan.dto.CotizacionRequest;
import com.segurplan.model.Aseguradora;
import com.segurplan.model.Cotizacion;
import com.segurplan.model.TipoSeguro;
import com.segurplan.model.Usuario;
import com.segurplan.repository.AseguradoraRepository;
import com.segurplan.repository.CotizacionRepository;
import com.segurplan.repository.TipoSeguroRepository;
import com.segurplan.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import com.segurplan.dto.RespuestaCotizacionRequest;
import java.time.LocalDateTime;

import java.util.List;

@Service
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoSeguroRepository tipoSeguroRepository;
    private final AseguradoraRepository aseguradoraRepository;

    public CotizacionService(
            CotizacionRepository cotizacionRepository,
            UsuarioRepository usuarioRepository,
            TipoSeguroRepository tipoSeguroRepository,
            AseguradoraRepository aseguradoraRepository) {

        this.cotizacionRepository = cotizacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoSeguroRepository = tipoSeguroRepository;
        this.aseguradoraRepository = aseguradoraRepository;
    }
    public Cotizacion buscarPorId(Long idCotizacion) {

    return cotizacionRepository
            .findById(idCotizacion)
            .orElseThrow(() ->
                    new RuntimeException(
                            "Cotización no encontrada"
                    )
            );
}

    public Cotizacion crear(CotizacionRequest request) {

        Usuario usuario = usuarioRepository
                .findById(request.getIdUsuario())
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado"));

        TipoSeguro tipoSeguro = tipoSeguroRepository
                .findById(request.getIdTipoSeguro())
                .orElseThrow(() ->
                        new RuntimeException("Tipo de seguro no encontrado"));

        Aseguradora aseguradora = aseguradoraRepository
                .findById(request.getIdAseguradora())
                .orElseThrow(() ->
                        new RuntimeException("Aseguradora no encontrada"));

        Cotizacion cotizacion = new Cotizacion();

        cotizacion.setUsuario(usuario);
        cotizacion.setTipoSeguro(tipoSeguro);
        cotizacion.setAseguradora(aseguradora);
        cotizacion.setDescripcionSolicitud(
                request.getDescripcionSolicitud()
        );

        return cotizacionRepository.save(cotizacion);
    }

    public List<Cotizacion> listarPorUsuario(Long idUsuario) {
        return cotizacionRepository
                .findByUsuario_IdUsuario(idUsuario);
    }

    public List<Cotizacion> listarTodas() {
        return cotizacionRepository.findAll();
    }
    public Cotizacion responderCotizacion(
        Long idCotizacion,
        RespuestaCotizacionRequest request) {

    Cotizacion cotizacion =
            cotizacionRepository
                    .findById(idCotizacion)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Cotización no encontrada"
                            )
                    );

    String estado = request.getEstado();

    if (!"En revisión".equals(estado)
            && !"Disponible".equals(estado)) {

        throw new RuntimeException(
                "Estado de cotización no válido"
        );
    }

    cotizacion.setEstado(estado);

    if (request.getCobertura() != null) {
        cotizacion.setCobertura(
                request.getCobertura()
        );
    }

    if (request.getVigencia() != null) {
        cotizacion.setVigencia(
                request.getVigencia()
        );
    }

    if ("Disponible".equals(estado)) {

        cotizacion.setFechaRespuesta(
                LocalDateTime.now()
        );
    }

    return cotizacionRepository.save(cotizacion);
}
}
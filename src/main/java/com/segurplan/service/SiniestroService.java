package com.segurplan.service;

import com.segurplan.dto.SiniestroRequest;
import com.segurplan.model.Aseguradora;
import com.segurplan.model.Siniestro;
import com.segurplan.model.Usuario;
import com.segurplan.repository.AseguradoraRepository;
import com.segurplan.repository.SiniestroRepository;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class SiniestroService {

    private final SiniestroRepository siniestroRepository;
    private final UsuarioRepository usuarioRepository;
    private final AseguradoraRepository aseguradoraRepository;

    public SiniestroService(
            SiniestroRepository siniestroRepository,
            UsuarioRepository usuarioRepository,
            AseguradoraRepository aseguradoraRepository) {

        this.siniestroRepository = siniestroRepository;
        this.usuarioRepository = usuarioRepository;
        this.aseguradoraRepository = aseguradoraRepository;
    }

    // =====================================================
    // REGISTRAR SINIESTRO
    // =====================================================

    @Transactional
    public Siniestro registrar(
            String correoUsuario,
            SiniestroRequest request) {

        // -------------------------------------------------
        // 1. Validar datos recibidos
        // -------------------------------------------------

        validar(request);

        // -------------------------------------------------
        // 2. Obtener usuario autenticado
        // -------------------------------------------------

        Usuario usuario = usuarioRepository
                .findByCorreo(correoUsuario)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No se encontró el usuario autenticado."
                        )
                );

        // -------------------------------------------------
        // 3. Validar aseguradora
        // -------------------------------------------------

        if (request.getIdAseguradora() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una aseguradora."
            );
        }

        Aseguradora aseguradora = aseguradoraRepository
                .findById(request.getIdAseguradora())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La aseguradora seleccionada no existe."
                        )
                );

        // -------------------------------------------------
        // 4. Crear siniestro
        // -------------------------------------------------

        Siniestro siniestro = new Siniestro();

        siniestro.setUsuario(
                usuario
        );

        siniestro.setAseguradora(
                aseguradora
        );

        siniestro.setNumeroSiniestro(
                generarNumeroSiniestro()
        );

        siniestro.setFechaRegistro(
                LocalDateTime.now()
        );

        siniestro.setFechaAccidente(
                request.getFechaAccidente()
                        .atStartOfDay()
        );

        siniestro.setPlaca(
                limpiarMayuscula(
                        request.getPlaca()
                )
        );

        siniestro.setMarca(
                limpiar(
                        request.getMarca()
                )
        );

        siniestro.setModelo(
                limpiar(
                        request.getModelo()
                )
        );

        siniestro.setAnio(
                request.getAnio()
        );

        siniestro.setLugar(
                limpiar(
                        request.getLugar()
                )
        );

        siniestro.setDescripcion(
                limpiar(
                        request.getDescripcion()
                )
        );

        siniestro.setObservaciones(
                construirObservaciones(
                        request
                )
        );

        siniestro.setResultadoEvaluacion(
                null
        );

        siniestro.setEstado(
                "Registrado"
        );

        // -------------------------------------------------
        // 5. Guardar en PostgreSQL
        // -------------------------------------------------

        return siniestroRepository.save(
                siniestro
        );
    }

    // =====================================================
    // MIS SINIESTROS
    // =====================================================

    @Transactional(readOnly = true)
    public List<Siniestro> listarDelUsuario(
            String correoUsuario) {

        Usuario usuario = usuarioRepository
                .findByCorreo(correoUsuario)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No se encontró el usuario autenticado."
                        )
                );

        return siniestroRepository
                .findByUsuario_IdUsuarioOrderByFechaRegistroDesc(
                        usuario.getIdUsuario()
                );
    }

    // =====================================================
    // OBTENER SINIESTRO DEL USUARIO
    // =====================================================

    @Transactional(readOnly = true)
    public Siniestro obtenerDelUsuario(
            Long idSiniestro,
            String correoUsuario) {

        Siniestro siniestro = siniestroRepository
                .findById(idSiniestro)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El siniestro no existe."
                        )
                );

        if (siniestro.getUsuario() == null
                || siniestro.getUsuario().getCorreo() == null
                || !siniestro.getUsuario()
                        .getCorreo()
                        .equalsIgnoreCase(correoUsuario)) {

            throw new IllegalArgumentException(
                    "El siniestro no pertenece al usuario autenticado."
            );
        }

        return siniestro;
    }

    // =====================================================
    // VALIDACIONES
    // =====================================================

    private void validar(
            SiniestroRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Los datos del siniestro son obligatorios."
            );
        }

        if (request.getFechaAccidente() == null) {
            throw new IllegalArgumentException(
                    "La fecha del accidente es obligatoria."
            );
        }

        if (request.getFechaAccidente()
                .isAfter(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "La fecha del accidente no puede ser futura."
            );
        }

        if (request.getIdAseguradora() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una aseguradora."
            );
        }

        if (vacio(request.getPlaca())) {
            throw new IllegalArgumentException(
                    "La placa es obligatoria."
            );
        }

        if (vacio(request.getMarca())) {
            throw new IllegalArgumentException(
                    "La marca es obligatoria."
            );
        }

        if (vacio(request.getModelo())) {
            throw new IllegalArgumentException(
                    "El modelo es obligatorio."
            );
        }

        if (vacio(request.getLugar())) {
            throw new IllegalArgumentException(
                    "El lugar del accidente es obligatorio."
            );
        }

        if (vacio(request.getDescripcion())) {
            throw new IllegalArgumentException(
                    "La descripción del accidente es obligatoria."
            );
        }

        if (request.getAnio() != null) {

            int actual = Year.now().getValue();

            if (request.getAnio() < 1900
                    || request.getAnio() > actual + 1) {

                throw new IllegalArgumentException(
                        "El año del vehículo no es válido."
                );
            }
        }
    }

    // =====================================================
    // GENERAR NÚMERO DE SINIESTRO
    // =====================================================

    private String generarNumeroSiniestro() {

        String codigo = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        return "SIN-" + codigo;
    }

    // =====================================================
    // OBSERVACIONES
    // =====================================================

    private String construirObservaciones(
            SiniestroRequest request) {

        StringBuilder resultado =
                new StringBuilder();

        if (!vacio(request.getTipoSiniestro())) {

            resultado.append(
                    "Tipo de siniestro: "
            );

            resultado.append(
                    request.getTipoSiniestro()
                            .trim()
            );
        }

        if (!vacio(request.getObservaciones())) {

            if (!resultado.isEmpty()) {
                resultado.append(" | ");
            }

            resultado.append(
                    request.getObservaciones()
                            .trim()
            );
        }

        return resultado.isEmpty()
                ? null
                : resultado.toString();
    }

    // =====================================================
    // UTILIDADES
    // =====================================================

    private boolean vacio(
            String valor) {

        return valor == null
                || valor.isBlank();
    }

    private String limpiar(
            String valor) {

        return valor == null
                ? null
                : valor.trim();
    }

    private String limpiarMayuscula(
            String valor) {

        return valor == null
                ? null
                : valor.trim()
                        .toUpperCase();
    }
}
package com.segurplan.service;

import com.segurplan.dto.HistorialDTO;
import com.segurplan.model.Historial;
import com.segurplan.model.Usuario;
import com.segurplan.repository.HistorialRepository;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistorialService {

    private final HistorialRepository historialRepository;
    private final UsuarioRepository usuarioRepository;

    public HistorialService(
            HistorialRepository historialRepository,
            UsuarioRepository usuarioRepository) {

        this.historialRepository =
                historialRepository;

        this.usuarioRepository =
                usuarioRepository;
    }


    // =====================================================
    // REGISTRAR HISTORIAL
    // =====================================================

    @Transactional
    public void registrar(
            Long idUsuario,
            String entidad,
            Long idRegistro,
            String accion,
            String valorAnterior,
            String valorNuevo) {

        Historial historial =
                new Historial();


        // El usuario puede ser null según la estructura
        // de la tabla historial.

        if (idUsuario != null) {

            Usuario usuario =
                    usuarioRepository
                            .findById(idUsuario)
                            .orElse(null);

            historial.setUsuario(
                    usuario
            );
        }


        historial.setEntidad(
                entidad
        );

        historial.setIdRegistro(
                idRegistro
        );

        historial.setAccion(
                accion
        );

        historial.setValorAnterior(
                valorAnterior
        );

        historial.setValorNuevo(
                valorNuevo
        );


        historialRepository.save(
                historial
        );
    }


    // =====================================================
    // LISTAR TODO EL HISTORIAL
    // =====================================================

    @Transactional(readOnly = true)
    public List<HistorialDTO> listarTodos() {

        return historialRepository
                .findAllByOrderByFechaHoraDesc()
                .stream()
                .map(this::convertirDTO)
                .toList();
    }


    // =====================================================
    // LISTAR HISTORIAL POR ENTIDAD
    // =====================================================

    @Transactional(readOnly = true)
    public List<HistorialDTO> listarPorEntidad(
            String entidad) {

        return historialRepository
                .findByEntidadOrderByFechaHoraDesc(
                        entidad
                )
                .stream()
                .map(this::convertirDTO)
                .toList();
    }


    // =====================================================
    // LISTAR HISTORIAL POR USUARIO
    // =====================================================

    @Transactional(readOnly = true)
    public List<HistorialDTO> listarPorUsuario(
            Long idUsuario) {

        return historialRepository
                .findByUsuario_IdUsuarioOrderByFechaHoraDesc(
                        idUsuario
                )
                .stream()
                .map(this::convertirDTO)
                .toList();
    }


    // =====================================================
    // CONVERTIR ENTIDAD A DTO
    // =====================================================

    private HistorialDTO convertirDTO(
            Historial historial) {

        Usuario usuario =
                historial.getUsuario();


        Long idUsuario =
                null;

        String nombreUsuario =
                "Sistema";

        String correoUsuario =
                null;


        if (usuario != null) {

            idUsuario =
                    usuario.getIdUsuario();

            nombreUsuario =
                    (
                        (usuario.getNombres() != null
                            ? usuario.getNombres()
                            : "")
                        + " "
                        + (usuario.getApellidos() != null
                            ? usuario.getApellidos()
                            : "")
                    ).trim();

            correoUsuario =
                    usuario.getCorreo();
        }


        return new HistorialDTO(

                historial.getIdHistorial(),

                idUsuario,

                nombreUsuario,

                correoUsuario,

                historial.getEntidad(),

                historial.getIdRegistro(),

                historial.getAccion(),

                historial.getValorAnterior(),

                historial.getValorNuevo(),

                historial.getFechaHora()
        );
    }
}
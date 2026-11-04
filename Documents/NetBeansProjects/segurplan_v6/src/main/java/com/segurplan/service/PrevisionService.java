package com.segurplan.service;

import com.segurplan.dto.PrevisionRequest;
import com.segurplan.model.Prevision;
import com.segurplan.model.Usuario;
import com.segurplan.repository.PrevisionRepository;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PrevisionService {

    private final PrevisionRepository previsionRepository;
    private final UsuarioRepository usuarioRepository;

    public PrevisionService(
            PrevisionRepository previsionRepository,
            UsuarioRepository usuarioRepository) {

        this.previsionRepository = previsionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // =========================================================
    // CREAR SIMULACIÓN AFP
    // =========================================================
    @Transactional
    public Prevision simular(
            String correoUsuario,
            PrevisionRequest request) {

        Usuario usuario = usuarioRepository
                .findByCorreo(correoUsuario)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "Usuario no encontrado."
                )
                );

        validarDatos(request);

        BigDecimal pensionEstimada
                = calcularPension(
                        request.getEdad(),
                        request.getIngresoMensual(),
                        request.getAniosAporte()
                );

        Prevision prevision = new Prevision();

        prevision.setUsuario(usuario);

        // El alcance actual del proyecto es AFP.
        prevision.setTipoSistema("AFP");

        prevision.setEdad(request.getEdad());
        prevision.setIngresoMensual(request.getIngresoMensual());
        prevision.setAniosAporte(request.getAniosAporte());

        prevision.setMontoPensionEstimado(
                pensionEstimada
        );

        prevision.setResultadoSimulacion(
                generarResultado(
                        request,
                        pensionEstimada
                )
        );

        prevision.setConsulta(
                request.getConsulta()
        );

        prevision.setEstado("Simulada");

        return previsionRepository.save(prevision);
    }

    // =========================================================
    // SIMULACIONES DEL USUARIO
    // =========================================================
    @Transactional(readOnly = true)
    public List<Prevision> listarPorUsuario(
            String correoUsuario) {

        Usuario usuario = usuarioRepository
                .findByCorreo(correoUsuario)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "Usuario no encontrado."
                )
                );

        return previsionRepository
                .findByUsuario_IdUsuarioOrderByFechaRegistroDesc(
                        usuario.getIdUsuario()
                );
    }

    // =========================================================
    // OBTENER UNA SIMULACIÓN
    // =========================================================
    @Transactional(readOnly = true)
    public Prevision obtenerDelUsuario(
            Long idPrevision,
            String correoUsuario) {

        Prevision prevision = previsionRepository
                .findById(idPrevision)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "Simulación AFP no encontrada."
                )
                );

        if (!prevision.getUsuario()
                .getCorreo()
                .equalsIgnoreCase(correoUsuario)) {

            throw new IllegalArgumentException(
                    "La simulación no pertenece al usuario autenticado."
            );
        }

        return prevision;
    }

    // =========================================================
    // VALIDACIONES
    // =========================================================
    private void validarDatos(
            PrevisionRequest request) {

        if (request.getEdad() == null
                || request.getEdad() < 18
                || request.getEdad() > 70) {

            throw new IllegalArgumentException(
                    "La edad debe estar entre 18 y 70 años."
            );
        }

        if (request.getIngresoMensual() == null
                || request.getIngresoMensual()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "El ingreso mensual debe ser mayor que cero."
            );
        }

        if (request.getAniosAporte() == null
                || request.getAniosAporte() < 0) {

            throw new IllegalArgumentException(
                    "Los años de aporte no pueden ser negativos."
            );
        }

        if (request.getAniosAporte()
                > request.getEdad() - 18) {

            throw new IllegalArgumentException(
                    "Los años de aporte no son coherentes con la edad ingresada."
            );
        }
    }

    // =========================================================
    // CÁLCULO REFERENCIAL
    // =========================================================
    private BigDecimal calcularPension(
            Integer edad,
            BigDecimal ingresoMensual,
            Integer aniosAporte) {

        /*
         * SIMULACIÓN ACADÉMICA REFERENCIAL.
         *
         * No representa el cálculo oficial de una AFP.
         *
         * Se utiliza una aproximación simple para demostrar
         * el funcionamiento del módulo del proyecto.
         */
        BigDecimal tasaAporte
                = new BigDecimal("0.10");

        int mesesAportados
                = aniosAporte * 12;

        BigDecimal fondoEstimado
                = ingresoMensual
                        .multiply(tasaAporte)
                        .multiply(
                                BigDecimal.valueOf(
                                        mesesAportados
                                )
                        );

        /*
         * Distribución académica del fondo en
         * aproximadamente 20 años de pensión.
         */
        BigDecimal mesesPension
                = BigDecimal.valueOf(240);

        BigDecimal pension
                = fondoEstimado.divide(
                        mesesPension,
                        2,
                        RoundingMode.HALF_UP
                );

        /*
         * Evitar mostrar un valor superior al ingreso
         * mensual declarado.
         */
        if (pension.compareTo(ingresoMensual) > 0) {
            pension = ingresoMensual;
        }

        return pension;
    }

    private String generarResultado(
            PrevisionRequest request,
            BigDecimal pension) {

        return "Simulación AFP referencial. "
                + "Edad: " + request.getEdad()
                + " años. Años de aporte registrados: "
                + request.getAniosAporte()
                + ". Pensión mensual estimada: S/ "
                + pension.setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                + ". Este resultado es únicamente una "
                + "simulación académica y no constituye "
                + "una proyección oficial de una AFP.";
    }
    // =========================================================
// LISTAR SOLICITUDES AFP PARA EL ASESOR
// =========================================================

    @Transactional(readOnly = true)
    public List<Prevision> listarSolicitudesAsesor() {

        return previsionRepository
                .findByEstadoOrderByFechaRegistroDesc(
                        "En revisión"
                );
    }

// =========================================================
// SOLICITAR ORIENTACIÓN
// =========================================================
    @Transactional
    public Prevision solicitarOrientacion(
            Long idPrevision,
            String correoUsuario) {

        Prevision prevision
                = obtenerDelUsuario(
                        idPrevision,
                        correoUsuario
                );

        if (!"Simulada".equals(
                prevision.getEstado())) {

            throw new IllegalArgumentException(
                    "La simulación no se encuentra disponible para solicitar orientación."
            );
        }

        prevision.setEstado(
                "En revisión"
        );

        return previsionRepository.save(
                prevision
        );
    }

// =========================================================
// RESPONDER ORIENTACIÓN
// =========================================================
    @Transactional
    public Prevision responderOrientacion(
            Long idPrevision,
            String respuesta) {

        Prevision prevision
                = previsionRepository
                        .findById(idPrevision)
                        .orElseThrow(()
                                -> new IllegalArgumentException(
                                "Solicitud AFP no encontrada."
                        )
                        );

        if (!"En revisión".equals(
                prevision.getEstado())) {

            throw new IllegalArgumentException(
                    "La solicitud AFP no está pendiente de revisión."
            );
        }

        if (respuesta == null
                || respuesta.isBlank()) {

            throw new IllegalArgumentException(
                    "Debe ingresar una respuesta para el cliente."
            );
        }

        prevision.setRespuesta(
                respuesta.trim()
        );

        prevision.setEstado(
                "Respondida"
        );

        return previsionRepository.save(
                prevision
        );
    }
}

package com.segurplan.service;

import com.segurplan.dto.ContratacionRequest;
import com.segurplan.dto.PagoRequest;
import com.segurplan.model.Contratacion;
import com.segurplan.model.Cotizacion;
import com.segurplan.model.Pago;
import com.segurplan.model.Usuario;
import com.segurplan.repository.CotizacionRepository;
import com.segurplan.repository.PagoRepository;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.YearMonth;
import java.util.UUID;

/**
 * PAGOS SIMULADOS. No se conecta a ninguna pasarela ni cobra dinero real.
 *
 * Métodos: TARJETA, YAPE y PLIN (billeteras digitales con código QR).
 *
 * Reglas de la simulación:
 *  Tarjeta
 *   - 4111 1111 1111 1111  -> aprobado
 *   - 4000 0000 0000 0002  -> rechazado (fondos insuficientes)
 *   - 4000 0000 0000 0069  -> rechazado (tarjeta vencida según el banco)
 *   - cualquier otra con formato válido (Luhn) -> aprobado
 *  Yape / Plin
 *   - celular de 9 dígitos que empiece con 9
 *   - celular terminado en 000 -> rechazado (saldo insuficiente)
 *   - cualquier otro -> aprobado, con número de operación generado
 *
 * Si el pago es aprobado se registra la contratación en la misma transacción.
 */
@Service
public class PagoService {

    public static final String METODO_TARJETA = "Tarjeta (simulado)";

    // Datos FICTICIOS del comercio que recibe el pago por QR
    public static final String CELULAR_COMERCIO = "999888777";
    public static final String NOMBRE_COMERCIO = "SEGURPLAN S.A.C. (ficticio)";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PagoRepository pagoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContratacionService contratacionService;
    private final HistorialService historialService;

    public PagoService(
            PagoRepository pagoRepository,
            CotizacionRepository cotizacionRepository,
            UsuarioRepository usuarioRepository,
            ContratacionService contratacionService,
            HistorialService historialService) {

        this.pagoRepository = pagoRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.contratacionService = contratacionService;
        this.historialService = historialService;
    }

    @Transactional
    public Pago pagar(String correoUsuario, PagoRequest request) {

        if (request.getIdCotizacion() == null) {
            throw new IllegalArgumentException("Falta la cotización a pagar.");
        }

        // El usuario sale de la sesión autenticada, no del cuerpo de la petición.
        Usuario usuario = usuarioRepository
                .findByCorreo(correoUsuario)
                .orElseThrow(() ->
                        new IllegalArgumentException("Usuario no encontrado."));

        Cotizacion cotizacion = cotizacionRepository
                .findById(request.getIdCotizacion())
                .orElseThrow(() ->
                        new IllegalArgumentException("Cotización no encontrada."));

        if (!cotizacion.getUsuario().getIdUsuario()
                .equals(usuario.getIdUsuario())) {
            throw new IllegalArgumentException(
                    "La cotización no pertenece al usuario.");
        }

        if (!"Disponible".equals(cotizacion.getEstado())) {
            throw new IllegalArgumentException(
                    "La cotización no está disponible para contratación.");
        }

        if (!Boolean.TRUE.equals(request.getAceptaCondiciones())) {
            throw new IllegalArgumentException("Debe aceptar las condiciones.");
        }

        BigDecimal monto = cotizacion.getPrecio();

        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException(
                    "La cotización no tiene un precio válido para cobrar.");
        }

        if (pagoRepository.findByCotizacion_IdCotizacionAndEstado(
                cotizacion.getIdCotizacion(), "Aprobado").isPresent()) {
            throw new IllegalArgumentException(
                    "Esta cotización ya tiene un pago aprobado.");
        }

        Pago pago = new Pago();
        pago.setCotizacion(cotizacion);
        pago.setUsuario(usuario);
        pago.setMonto(monto);
        pago.setMoneda("PEN");
        pago.setReferencia(
                "PAG-" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 12).toUpperCase());

        // ---------- Método de pago ----------
        String metodo = request.getMetodo() == null
                || request.getMetodo().isBlank()
                ? "TARJETA"
                : request.getMetodo().trim().toUpperCase();

        String motivoRechazo;

        switch (metodo) {
            case "TARJETA" -> motivoRechazo = procesarTarjeta(request, pago);
            case "YAPE", "PLIN" -> motivoRechazo = procesarBilletera(metodo, request, pago);
            default -> throw new IllegalArgumentException(
                    "Método de pago no válido.");
        }

        // ---------- Pago rechazado: se guarda, no se contrata ----------
        if (motivoRechazo != null) {

            pago.setEstado("Rechazado");
            pago.setMotivoRechazo(motivoRechazo);
            pago.setNumeroOperacion(null);

            Pago guardado = pagoRepository.save(pago);

            historialService.registrar(
                    usuario.getIdUsuario(), "Pago", guardado.getIdPago(),
                    "Pago rechazado", null,
                    guardado.getMetodo() + " - " + guardado.getReferencia()
                            + " - " + motivoRechazo);

            return guardado;
        }

        // ---------- Pago aprobado: se registra la contratación ----------
        // (misma transacción; si algo falla, se revierte también el pago)
        ContratacionRequest contratacionRequest = new ContratacionRequest();
        contratacionRequest.setIdCotizacion(cotizacion.getIdCotizacion());
        contratacionRequest.setIdUsuario(usuario.getIdUsuario());
        contratacionRequest.setAceptaCondiciones(true);

        Contratacion contratacion =
                contratacionService.crear(contratacionRequest);

        pago.setEstado("Aprobado");
        pago.setContratacion(contratacion);

        Pago guardado = pagoRepository.save(pago);

        historialService.registrar(
                usuario.getIdUsuario(), "Pago", guardado.getIdPago(),
                "Pago aprobado", null,
                guardado.getMetodo() + " - " + guardado.getReferencia()
                        + " - S/ " + monto
                        + " - contratación " + contratacion.getIdContratacion());

        return guardado;
    }

    // =====================================================
    // TARJETA
    // =====================================================

    /** Valida y "cobra" la tarjeta. Devuelve el motivo del rechazo o null. */
    private String procesarTarjeta(PagoRequest request, Pago pago) {

        String numero = soloDigitos(request.getNumeroTarjeta());
        String titular = request.getTitular() == null
                ? "" : request.getTitular().trim();

        if (titular.length() < 3 || titular.length() > 120) {
            throw new IllegalArgumentException(
                    "Ingrese el nombre del titular de la tarjeta.");
        }

        if (numero.length() < 13 || numero.length() > 19
                || !cumpleLuhn(numero)) {
            throw new IllegalArgumentException(
                    "El número de tarjeta no es válido.");
        }

        validarVencimiento(request.getVencimiento());

        String cvv = soloDigitos(request.getCvv());
        if (cvv.length() < 3 || cvv.length() > 4) {
            throw new IllegalArgumentException("El CVV no es válido.");
        }

        pago.setMetodo(METODO_TARJETA);
        // Nunca se guarda el número completo ni el CVV.
        pago.setTarjetaUltimos4(numero.substring(numero.length() - 4));
        pago.setTitular(titular);

        if (numero.endsWith("0002")) {
            return "Fondos insuficientes.";
        }

        if (numero.endsWith("0069")) {
            return "Tarjeta vencida según el banco emisor.";
        }

        return null;
    }

    // =====================================================
    // BILLETERA DIGITAL (YAPE / PLIN)
    // =====================================================

    /** Simula el cobro por QR. Devuelve el motivo del rechazo o null. */
    private String procesarBilletera(
            String billetera, PagoRequest request, Pago pago) {

        String celular = soloDigitos(request.getCelular());

        if (!celular.matches("9\\d{8}")) {
            throw new IllegalArgumentException(
                    "El celular debe tener 9 dígitos y empezar con 9.");
        }

        pago.setMetodo(("YAPE".equals(billetera) ? "Yape" : "Plin")
                + " (simulado)");

        // Solo se guarda el celular enmascarado: 9** *** 789
        pago.setCelularOrigen(
                celular.charAt(0) + "** *** " + celular.substring(6));

        pago.setDestino(CELULAR_COMERCIO);

        if (celular.endsWith("000")) {
            return "Saldo insuficiente en la billetera.";
        }

        pago.setNumeroOperacion(
                String.format("%08d", RANDOM.nextInt(100_000_000)));

        return null;
    }

    // =====================================================
    // VALIDACIONES
    // =====================================================

    private String soloDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private boolean cumpleLuhn(String numero) {

        int suma = 0;
        boolean duplicar = false;

        for (int i = numero.length() - 1; i >= 0; i--) {

            int d = numero.charAt(i) - '0';

            if (duplicar) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }

            suma += d;
            duplicar = !duplicar;
        }

        return suma % 10 == 0;
    }

    private void validarVencimiento(String vencimiento) {

        if (vencimiento == null
                || !vencimiento.trim().matches("(0[1-9]|1[0-2])/\\d{2}")) {
            throw new IllegalArgumentException(
                    "La fecha de vencimiento debe tener el formato MM/AA.");
        }

        String[] partes = vencimiento.trim().split("/");

        YearMonth vence = YearMonth.of(
                2000 + Integer.parseInt(partes[1]),
                Integer.parseInt(partes[0]));

        if (vence.isBefore(YearMonth.now())) {
            throw new IllegalArgumentException("La tarjeta está vencida.");
        }
    }
}

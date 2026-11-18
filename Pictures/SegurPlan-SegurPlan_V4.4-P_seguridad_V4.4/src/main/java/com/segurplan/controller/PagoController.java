package com.segurplan.controller;

import com.segurplan.dto.PagoRequest;
import com.segurplan.model.Pago;
import com.segurplan.service.PagoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /**
     * Datos FICTICIOS del comercio que se muestran en el código QR
     * de la billetera digital (Yape / Plin).
     */
    @GetMapping("/datos-qr")
    public ResponseEntity<?> datosQr() {

        Map<String, String> datos = new LinkedHashMap<>();

        String n = PagoService.CELULAR_COMERCIO;

        datos.put("numero", n);
        datos.put("numeroFormateado",
                n.substring(0, 3) + " " + n.substring(3, 6) + " " + n.substring(6));
        datos.put("nombre", PagoService.NOMBRE_COMERCIO);

        return ResponseEntity.ok(datos);
    }

    /**
     * Paga (de forma simulada) una cotización con tarjeta, Yape o Plin.
     * Si el pago se aprueba, también se registra la contratación.
     */
    @PostMapping
    public ResponseEntity<?> pagar(
            @RequestBody PagoRequest request,
            Authentication authentication) {

        try {

            Pago pago = pagoService.pagar(
                    authentication.getName(), request);

            Map<String, Object> respuesta = new LinkedHashMap<>();

            boolean aprobado = "Aprobado".equals(pago.getEstado());

            respuesta.put("aprobado", aprobado);
            respuesta.put("estado", pago.getEstado());
            respuesta.put("idPago", pago.getIdPago());
            respuesta.put("referencia", pago.getReferencia());
            respuesta.put("monto", pago.getMonto());
            respuesta.put("moneda", pago.getMoneda());
            respuesta.put("metodo", pago.getMetodo());
            respuesta.put("detalle", detalle(pago));
            respuesta.put("numeroOperacion", pago.getNumeroOperacion());
            respuesta.put("fechaPago", pago.getFechaPago());

            if (aprobado) {
                respuesta.put("mensaje",
                        "Pago simulado aprobado. Contratación registrada.");
                respuesta.put("idContratacion",
                        pago.getContratacion().getIdContratacion());
                return ResponseEntity.ok(respuesta);
            }

            respuesta.put("mensaje",
                    "Pago rechazado: " + pago.getMotivoRechazo());

            return ResponseEntity
                    .status(HttpStatus.PAYMENT_REQUIRED)
                    .body(respuesta);

        } catch (RuntimeException e) {

            Map<String, String> error = new LinkedHashMap<>();
            error.put("mensaje", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }
    }

    private String detalle(Pago pago) {

        if (pago.getTarjetaUltimos4() != null) {
            return "Tarjeta **** **** **** " + pago.getTarjetaUltimos4();
        }

        if (pago.getCelularOrigen() != null) {
            return "Celular " + pago.getCelularOrigen()
                    + " → " + pago.getDestino();
        }

        return "";
    }
}

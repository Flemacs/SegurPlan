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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de la simulación de pagos (PagoService).
 * Cubre el caso de prueba CP003 (Requisitos RF007 y RF034).
 *
 * Se simulan con Mockito los repositorios y servicios externos
 * para garantizar la ejecución en memoria sin conexión a base de datos.
 */
@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    private static final String CORREO_USUARIO = "cliente@segurplan.pe";
    private static final Long ID_COTIZACION = 100L;
    private static final Long ID_USUARIO = 1L;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private CotizacionRepository cotizacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ContratacionService contratacionService;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private PagoService pagoService;

    // =====================================================
    // DATOS DE PRUEBA AUXILIARES
    // =====================================================

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(ID_USUARIO);
        usuario.setCorreo(CORREO_USUARIO);
        return usuario;
    }

    private Cotizacion crearCotizacion(Usuario usuario) {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setIdCotizacion(ID_COTIZACION);
        cotizacion.setUsuario(usuario);
        cotizacion.setEstado("Disponible");
        cotizacion.setPrecio(new BigDecimal("350.00"));
        return cotizacion;
    }

    private PagoRequest crearRequestTarjetaBase() {
        PagoRequest request = new PagoRequest();
        request.setIdCotizacion(ID_COTIZACION);
        request.setAceptaCondiciones(true);
        request.setMetodo("TARJETA");
        request.setTitular("Juan Pérez García");
        request.setNumeroTarjeta("4111 1111 1111 1111");
        request.setVencimiento("12/30");
        request.setCvv("123");
        return request;
    }

    /**
     * Configura los repositorios para que el usuario y la cotización existan
     * y cumplan las precondiciones de pago.
     */
    private void configurarEntornoValido() {
        Usuario usuario = crearUsuario();
        Cotizacion cotizacion = crearCotizacion(usuario);

        when(usuarioRepository.findByCorreo(CORREO_USUARIO))
                .thenReturn(Optional.of(usuario));
        when(cotizacionRepository.findById(ID_COTIZACION))
                .thenReturn(Optional.of(cotizacion));
        when(pagoRepository.findByCotizacion_IdCotizacionAndEstado(ID_COTIZACION, "Aprobado"))
                .thenReturn(Optional.empty());
    }

    // =====================================================
    // CASOS DE PRUEBA - CP003 (RF034)
    // =====================================================

    /**
     * Prueba 1: Rechazo de tarjeta con número inválido según algoritmo de Luhn.
     * Comprueba que si la tarjeta no cumple la suma de verificación, se lance IllegalArgumentException.
     */
    @Test
    void pagar_tarjetaInvalidaLuhn_lanzaExcepcion() {
        // Arrange (Preparar datos)
        configurarEntornoValido();
        PagoRequest request = crearRequestTarjetaBase();
        request.setNumeroTarjeta("4111 1111 1111 1112"); // Checksum de Luhn inválido

        // Act & Assert (Ejecutar y comprobar)
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.pagar(CORREO_USUARIO, request));

        assertEquals("El número de tarjeta no es válido.", ex.getMessage());
        verify(pagoRepository, never()).save(any(Pago.class));
        verifyNoInteractions(contratacionService);
    }

    /**
     * Prueba 2: Rechazo de tarjeta con fecha de vencimiento expirada.
     * Comprueba que si el vencimiento es anterior al mes actual, se lance IllegalArgumentException.
     */
    @Test
    void pagar_tarjetaVencida_lanzaExcepcion() {
        // Arrange
        configurarEntornoValido();
        PagoRequest request = crearRequestTarjetaBase();
        request.setVencimiento("01/20"); // Tarjeta vencida en enero de 2020

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.pagar(CORREO_USUARIO, request));

        assertEquals("La tarjeta está vencida.", ex.getMessage());
        verify(pagoRepository, never()).save(any(Pago.class));
        verifyNoInteractions(contratacionService);
    }

    /**
     * Prueba 3: Simulación de pago con tarjeta rechazada por fondos insuficientes.
     * La tarjeta cumple Luhn pero termina en '0002'. El sistema debe registrar el pago como
     * Rechazado sin crear la contratación.
     */
    @Test
    void pagar_tarjetaFondosInsuficientes_registraPagoRechazado() {
        // Arrange
        configurarEntornoValido();
        PagoRequest request = crearRequestTarjetaBase();
        request.setNumeroTarjeta("4000 0000 0000 0002"); // Tarjeta válida que simula falta de fondos

        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setIdPago(15L);
            return p;
        });

        // Act
        Pago resultado = pagoService.pagar(CORREO_USUARIO, request);

        // Assert
        assertNotNull(resultado);
        assertEquals("Rechazado", resultado.getEstado());
        assertEquals("Fondos insuficientes.", resultado.getMotivoRechazo());
        assertNull(resultado.getNumeroOperacion());
        verify(pagoRepository).save(any(Pago.class));
        verifyNoInteractions(contratacionService); // No debe contratarse
    }

    /**
     * Prueba 4: Validación de formato incorrecto de teléfono para billetera digital (Yape/Plin).
     * Comprueba que si el celular no tiene 9 dígitos o no empieza con 9, se lance IllegalArgumentException.
     */
    @Test
    void pagar_billeteraTelefonoInvalido_lanzaExcepcion() {
        // Arrange
        configurarEntornoValido();
        PagoRequest request = new PagoRequest();
        request.setIdCotizacion(ID_COTIZACION);
        request.setAceptaCondiciones(true);
        request.setMetodo("YAPE");
        request.setCelular("812345678"); // Inválido: no empieza con 9

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.pagar(CORREO_USUARIO, request));

        assertEquals("El celular debe tener 9 dígitos y empezar con 9.", ex.getMessage());
        verify(pagoRepository, never()).save(any(Pago.class));
        verifyNoInteractions(contratacionService);
    }

    /**
     * Prueba 5: Simulación de billetera digital con saldo insuficiente.
     * El celular tiene formato válido pero termina en '000'. El sistema debe registrar el pago
     * como Rechazado sin crear la contratación.
     */
    @Test
    void pagar_billeteraSaldoInsuficiente_registraPagoRechazado() {
        // Arrange
        configurarEntornoValido();
        PagoRequest request = new PagoRequest();
        request.setIdCotizacion(ID_COTIZACION);
        request.setAceptaCondiciones(true);
        request.setMetodo("PLIN");
        request.setCelular("987654000"); // Termina en 000: simula saldo insuficiente

        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setIdPago(16L);
            return p;
        });

        // Act
        Pago resultado = pagoService.pagar(CORREO_USUARIO, request);

        // Assert
        assertNotNull(resultado);
        assertEquals("Rechazado", resultado.getEstado());
        assertEquals("Saldo insuficiente en la billetera.", resultado.getMotivoRechazo());
        assertNull(resultado.getNumeroOperacion());
        verify(pagoRepository).save(any(Pago.class));
        verifyNoInteractions(contratacionService);
    }

    /**
     * Prueba 6: Procesamiento correcto de un pago válido con tarjeta.
     * Comprueba que ante datos válidos el pago quede en estado 'Aprobado', se invoque
     * el servicio de contratación y se registre en la auditoría.
     */
    @Test
    void pagar_tarjetaValida_apruebaPagoYRegistraContratacion() {
        // Arrange
        configurarEntornoValido();
        PagoRequest request = crearRequestTarjetaBase();

        Contratacion contratacion = new Contratacion();
        contratacion.setIdContratacion(50L);
        when(contratacionService.crear(any(ContratacionRequest.class))).thenReturn(contratacion);

        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setIdPago(17L);
            return p;
        });

        // Act
        Pago resultado = pagoService.pagar(CORREO_USUARIO, request);

        // Assert
        assertNotNull(resultado);
        assertEquals("Aprobado", resultado.getEstado());
        assertEquals("1111", resultado.getTarjetaUltimos4());
        assertEquals("Juan Pérez García", resultado.getTitular());
        assertNotNull(resultado.getContratacion());
        assertEquals(50L, resultado.getContratacion().getIdContratacion());

        verify(contratacionService).crear(any(ContratacionRequest.class));
        verify(pagoRepository).save(any(Pago.class));
        verify(historialService).registrar(
                eq(ID_USUARIO), eq("Pago"), eq(17L), eq("Pago aprobado"),
                isNull(), contains("contratación 50"));
    }
}

package com.segurplan.service;

import com.segurplan.model.Documento;
import com.segurplan.model.Poliza;
import com.segurplan.model.Prevision;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.segurplan.model.Historial;
import com.segurplan.repository.HistorialRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReporteService {

    @PersistenceContext
    private EntityManager entityManager;

    private final HistorialRepository historialRepository;

    public ReporteService(HistorialRepository historialRepository) {
        this.historialRepository = historialRepository;
    }

    // =========================================================
    // REPORTE GENERAL
    // =========================================================
    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteGeneral(
            Integer anio,
            Integer mes) {

        validarPeriodo(anio, mes);

        YearMonth periodo
                = YearMonth.of(anio, mes);

        LocalDateTime inicio
                = periodo.atDay(1)
                        .atStartOfDay();

        LocalDateTime fin
                = periodo.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        // =====================================================
        // COTIZACIONES
        // =====================================================
        long totalCotizaciones
                = contarCotizaciones(
                        inicio,
                        fin,
                        null
                );

        /*
         * Consideramos atendida una cotización cuando ya
         * avanzó más allá de "Solicitada" o "En revisión".
         */
        long cotizacionesAtendidas
                = contarCotizacionesAtendidas(
                        inicio,
                        fin
                );

        /*
         * Adquirida = cotización que llegó al estado Aceptada.
         */
        long cotizacionesAdquiridas
                = contarCotizaciones(
                        inicio,
                        fin,
                        "Aceptada"
                );

        // =====================================================
        // SINIESTROS
        // =====================================================
        long totalSiniestros
                = contarSiniestros(
                        inicio,
                        fin,
                        null
                );

        long siniestrosRevision
                = contarSiniestros(
                        inicio,
                        fin,
                        "En revisión"
                );

        long siniestrosDerivados
                = contarSiniestros(
                        inicio,
                        fin,
                        "Derivado"
                );

        long siniestrosConResultado
                = contarSiniestrosConResultado(
                        inicio,
                        fin
                );

        // =====================================================
        // AFP
        // =====================================================
        long totalAFP
                = contarPrevisiones(
                        inicio,
                        fin,
                        null
                );

        long afpSimuladas
                = contarPrevisionesSimuladas(
                        inicio,
                        fin
                );

        long afpOrientaciones
                = contarPrevisionesOrientadas(
                        inicio,
                        fin
                );

        // =====================================================
        // RESPUESTA
        // =====================================================
        Map<String, Object> reporte
                = new LinkedHashMap<>();

        reporte.put(
                "anio",
                anio
        );

        reporte.put(
                "mes",
                mes
        );

        reporte.put(
                "totalCotizaciones",
                totalCotizaciones
        );

        reporte.put(
                "totalSiniestros",
                totalSiniestros
        );

        reporte.put(
                "totalAFP",
                totalAFP
        );

        reporte.put(
                "cotizacionesAtendidas",
                cotizacionesAtendidas
        );

        reporte.put(
                "cotizacionesAdquiridas",
                cotizacionesAdquiridas
        );

        reporte.put(
                "porcentajeCotizacionesAtendidas",
                porcentaje(
                        cotizacionesAtendidas,
                        totalCotizaciones
                )
        );

        reporte.put(
                "porcentajeCotizacionesAdquiridas",
                porcentaje(
                        cotizacionesAdquiridas,
                        totalCotizaciones
                )
        );

        reporte.put(
                "siniestrosRevision",
                siniestrosRevision
        );

        reporte.put(
                "siniestrosDerivados",
                siniestrosDerivados
        );

        reporte.put(
                "siniestrosConResultado",
                siniestrosConResultado
        );

        reporte.put(
                "porcentajeSiniestrosRevision",
                porcentaje(
                        siniestrosRevision,
                        totalSiniestros
                )
        );

        reporte.put(
                "porcentajeSiniestrosDerivados",
                porcentaje(
                        siniestrosDerivados,
                        totalSiniestros
                )
        );

        reporte.put(
                "porcentajeSiniestrosConResultado",
                porcentaje(
                        siniestrosConResultado,
                        totalSiniestros
                )
        );

        reporte.put(
                "afpSimulaciones",
                afpSimuladas
        );

        reporte.put(
                "afpOrientaciones",
                afpOrientaciones
        );

        /*
         * IMPORTANTE:
         *
         * La tabla prevision NO guarda el perfil seleccionado
         * en s06 ("nuevo" / "tercera").
         *
         * Por lo tanto NO inventamos los contadores
         * "Nuevos inversores" y "Personas de tercera edad".
         */
        return reporte;
    }

    // =========================================================
    // COTIZACIONES
    // =========================================================
    private long contarCotizaciones(
            LocalDateTime inicio,
            LocalDateTime fin,
            String estado) {

        String jpql
                = """
                SELECT COUNT(c)
                FROM Cotizacion c
                WHERE c.fechaSolicitud >= :inicio
                  AND c.fechaSolicitud < :fin
                """;

        if (estado != null) {
            jpql += " AND c.estado = :estado";
        }

        Query query
                = entityManager.createQuery(
                        jpql
                );

        query.setParameter(
                "inicio",
                inicio
        );

        query.setParameter(
                "fin",
                fin
        );

        if (estado != null) {
            query.setParameter(
                    "estado",
                    estado
            );
        }

        return ((Number) query.getSingleResult())
                .longValue();
    }

    private long contarCotizacionesAtendidas(
            LocalDateTime inicio,
            LocalDateTime fin) {

        return ((Number) entityManager
                .createQuery(
                        """
                                SELECT COUNT(c)
                                FROM Cotizacion c
                                WHERE c.fechaSolicitud >= :inicio
                                  AND c.fechaSolicitud < :fin
                                  AND c.estado NOT IN (
                                      'Solicitada',
                                      'En revisión'
                                  )
                                """
                )
                .setParameter(
                        "inicio",
                        inicio
                )
                .setParameter(
                        "fin",
                        fin
                )
                .getSingleResult())
                .longValue();
    }

    // =========================================================
    // SINIESTROS
    // =========================================================
    private long contarSiniestros(
            LocalDateTime inicio,
            LocalDateTime fin,
            String estado) {

        String jpql
                = """
                SELECT COUNT(s)
                FROM Siniestro s
                WHERE s.fechaRegistro >= :inicio
                  AND s.fechaRegistro < :fin
                """;

        if (estado != null) {
            jpql += " AND s.estado = :estado";
        }

        Query query
                = entityManager.createQuery(
                        jpql
                );

        query.setParameter(
                "inicio",
                inicio
        );

        query.setParameter(
                "fin",
                fin
        );

        if (estado != null) {
            query.setParameter(
                    "estado",
                    estado
            );
        }

        return ((Number) query.getSingleResult())
                .longValue();
    }

    private long contarSiniestrosConResultado(
            LocalDateTime inicio,
            LocalDateTime fin) {

        return ((Number) entityManager
                .createQuery(
                        """
                                SELECT COUNT(s)
                                FROM Siniestro s
                                WHERE s.fechaRegistro >= :inicio
                                  AND s.fechaRegistro < :fin
                                  AND s.resultadoEvaluacion IS NOT NULL
                                  AND TRIM(s.resultadoEvaluacion) <> ''
                                """
                )
                .setParameter(
                        "inicio",
                        inicio
                )
                .setParameter(
                        "fin",
                        fin
                )
                .getSingleResult())
                .longValue();
    }

    // =========================================================
    // AFP
    // =========================================================
    private long contarPrevisiones(
            LocalDateTime inicio,
            LocalDateTime fin,
            String estado) {

        String jpql
                = """
                SELECT COUNT(p)
                FROM Prevision p
                WHERE p.fechaRegistro >= :inicio
                  AND p.fechaRegistro < :fin
                """;

        if (estado != null) {
            jpql += " AND p.estado = :estado";
        }

        Query query
                = entityManager.createQuery(
                        jpql
                );

        query.setParameter(
                "inicio",
                inicio
        );

        query.setParameter(
                "fin",
                fin
        );

        if (estado != null) {
            query.setParameter(
                    "estado",
                    estado
            );
        }

        return ((Number) query.getSingleResult())
                .longValue();
    }

    private long contarPrevisionesSimuladas(
            LocalDateTime inicio,
            LocalDateTime fin) {

        /*
         * Cuenta registros que alcanzaron una etapa
         * de simulación u orientación.
         */
        return ((Number) entityManager
                .createQuery(
                        """
                                SELECT COUNT(p)
                                FROM Prevision p
                                WHERE p.fechaRegistro >= :inicio
                                  AND p.fechaRegistro < :fin
                                  AND p.montoPensionEstimado IS NOT NULL
                                """
                )
                .setParameter(
                        "inicio",
                        inicio
                )
                .setParameter(
                        "fin",
                        fin
                )
                .getSingleResult())
                .longValue();
    }

    private long contarPrevisionesOrientadas(
            LocalDateTime inicio,
            LocalDateTime fin) {

        return ((Number) entityManager
                .createQuery(
                        """
                                SELECT COUNT(p)
                                FROM Prevision p
                                WHERE p.fechaRegistro >= :inicio
                                  AND p.fechaRegistro < :fin
                                  AND p.estado IN (
                                      'En revisión',
                                      'Respondida',
                                      'Cerrada'
                                  )
                                """
                )
                .setParameter(
                        "inicio",
                        inicio
                )
                .setParameter(
                        "fin",
                        fin
                )
                .getSingleResult())
                .longValue();
    }

    // =========================================================
    // PORCENTAJE
    // =========================================================
    private BigDecimal porcentaje(
            long cantidad,
            long total) {

        if (total <= 0) {

            return BigDecimal.ZERO
                    .setScale(
                            1,
                            RoundingMode.HALF_UP
                    );
        }

        return BigDecimal
                .valueOf(cantidad)
                .multiply(
                        BigDecimal.valueOf(100)
                )
                .divide(
                        BigDecimal.valueOf(total),
                        1,
                        RoundingMode.HALF_UP
                );
    }

    // =========================================================
    // VALIDAR PERIODO
    // =========================================================
    private void validarPeriodo(
            Integer anio,
            Integer mes) {

        if (anio == null
                || anio < 2000
                || anio > 2100) {

            throw new IllegalArgumentException(
                    "El año indicado no es válido."
            );
        }

        if (mes == null
                || mes < 1
                || mes > 12) {

            throw new IllegalArgumentException(
                    "El mes indicado no es válido."
            );
        }
    }
    // =========================================================
// REPORTE DE SINIESTROS
// =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteSiniestros(
            Integer anio,
            Integer mes) {

        validarPeriodo(anio, mes);

        YearMonth periodo
                = YearMonth.of(anio, mes);

        LocalDateTime inicio
                = periodo.atDay(1)
                        .atStartOfDay();

        LocalDateTime fin
                = periodo.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        // =====================================================
        // TOTALES
        // =====================================================
        long total
                = contarSiniestros(
                        inicio,
                        fin,
                        null
                );

        long registrados
                = contarSiniestros(
                        inicio,
                        fin,
                        "Registrado"
                );

        long revision
                = contarSiniestros(
                        inicio,
                        fin,
                        "En revisión"
                );

        long derivados
                = contarSiniestros(
                        inicio,
                        fin,
                        "Derivado"
                );

        long conResultado
                = contarSiniestrosConResultado(
                        inicio,
                        fin
                );

        // =====================================================
        // LISTA DE SINIESTROS
        // =====================================================
        @SuppressWarnings("unchecked")
        java.util.List<com.segurplan.model.Siniestro> siniestros
                = entityManager
                        .createQuery(
                                """
                            SELECT s
                            FROM Siniestro s
                            WHERE s.fechaRegistro >= :inicio
                              AND s.fechaRegistro < :fin
                            ORDER BY s.fechaRegistro DESC
                            """
                        )
                        .setParameter(
                                "inicio",
                                inicio
                        )
                        .setParameter(
                                "fin",
                                fin
                        )
                        .getResultList();

        java.util.List<Map<String, Object>> detalle
                = siniestros
                        .stream()
                        .map(this::convertirSiniestro)
                        .toList();

        // =====================================================
        // RESPUESTA
        // =====================================================
        Map<String, Object> reporte
                = new LinkedHashMap<>();

        reporte.put(
                "anio",
                anio
        );

        reporte.put(
                "mes",
                mes
        );

        reporte.put(
                "total",
                total
        );

        reporte.put(
                "registrados",
                registrados
        );

        reporte.put(
                "enRevision",
                revision
        );

        reporte.put(
                "derivados",
                derivados
        );

        reporte.put(
                "conResultado",
                conResultado
        );

        reporte.put(
                "porcentajeRegistrados",
                porcentaje(
                        registrados,
                        total
                )
        );

        reporte.put(
                "porcentajeRevision",
                porcentaje(
                        revision,
                        total
                )
        );

        reporte.put(
                "porcentajeDerivados",
                porcentaje(
                        derivados,
                        total
                )
        );

        reporte.put(
                "porcentajeConResultado",
                porcentaje(
                        conResultado,
                        total
                )
        );

        reporte.put(
                "siniestros",
                detalle
        );

        return reporte;
    }

// =========================================================
// CONVERTIR SINIESTRO
// =========================================================
    private Map<String, Object> convertirSiniestro(
            com.segurplan.model.Siniestro s) {

        Map<String, Object> item
                = new LinkedHashMap<>();

        // =====================================================
        // IDENTIFICACIÓN
        // =====================================================
        item.put(
                "idSiniestro",
                s.getIdSiniestro()
        );

        item.put(
                "numeroSiniestro",
                s.getNumeroSiniestro()
        );

        // =====================================================
        // FECHAS
        // =====================================================
        item.put(
                "fechaRegistro",
                s.getFechaRegistro()
        );

        item.put(
                "fechaAccidente",
                s.getFechaAccidente()
        );

        // =====================================================
        // VEHÍCULO
        // =====================================================
        item.put(
                "placa",
                s.getPlaca()
        );

        item.put(
                "marca",
                s.getMarca()
        );

        item.put(
                "modelo",
                s.getModelo()
        );

        item.put(
                "anio",
                s.getAnio()
        );

        // =====================================================
        // ACCIDENTE
        // =====================================================
        item.put(
                "lugar",
                s.getLugar()
        );

        item.put(
                "descripcion",
                s.getDescripcion()
        );

        // =====================================================
        // EVALUACIÓN
        // =====================================================
        item.put(
                "resultadoEvaluacion",
                s.getResultadoEvaluacion()
        );

        item.put(
                "observaciones",
                s.getObservaciones()
        );

        item.put(
                "estado",
                s.getEstado()
        );

        // =====================================================
        // CLIENTE
        // =====================================================
        if (s.getUsuario() != null) {

            item.put(
                    "idUsuario",
                    s.getUsuario()
                            .getIdUsuario()
            );

            item.put(
                    "cliente",
                    s.getUsuario()
                            .getNombres()
                    + " "
                    + s.getUsuario()
                            .getApellidos()
            );

        } else {

            item.put(
                    "idUsuario",
                    null
            );

            item.put(
                    "cliente",
                    "—"
            );
        }

        // =====================================================
        // ASEGURADORA
        // =====================================================
        if (s.getAseguradora() != null) {

            item.put(
                    "idAseguradora",
                    s.getAseguradora()
                            .getIdAseguradora()
            );

        } else {

            item.put(
                    "idAseguradora",
                    null
            );
        }

        return item;
    }
    // =========================================================
// REPORTE AFP
// =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteAfp(
            Integer anio,
            Integer mes) {

        validarPeriodo(anio, mes);

        YearMonth periodo
                = YearMonth.of(anio, mes);

        LocalDateTime inicio
                = periodo.atDay(1)
                        .atStartOfDay();

        LocalDateTime fin
                = periodo.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        // =====================================================
        // REGISTROS AFP DEL PERÍODO
        // =====================================================
        @SuppressWarnings("unchecked")
        java.util.List<com.segurplan.model.Prevision> previsiones
                = entityManager
                        .createQuery(
                                """
                            SELECT p
                            FROM Prevision p
                            WHERE p.tipoSistema = 'AFP'
                              AND p.fechaRegistro >= :inicio
                              AND p.fechaRegistro < :fin
                            ORDER BY p.fechaRegistro DESC
                            """
                        )
                        .setParameter("inicio", inicio)
                        .setParameter("fin", fin)
                        .getResultList();

        long totalSimulaciones
                = previsiones.size();

        // =====================================================
        // USUARIOS DIFERENTES
        // =====================================================
        long usuarios
                = previsiones
                        .stream()
                        .map(p
                                -> p.getUsuario()
                                .getIdUsuario()
                        )
                        .distinct()
                        .count();

        // =====================================================
        // ORIENTACIONES SOLICITADAS
        // Una consulta escrita por el usuario cuenta como
        // solicitud de orientación.
        // =====================================================
        long orientacionesSolicitadas
                = previsiones
                        .stream()
                        .filter(p
                                -> p.getConsulta() != null
                        && !p.getConsulta()
                                .isBlank()
                        )
                        .count();

        // =====================================================
        // ORIENTACIONES RESPONDIDAS
        // =====================================================
        long orientacionesRespondidas
                = previsiones
                        .stream()
                        .filter(p
                                -> p.getRespuesta() != null
                        && !p.getRespuesta()
                                .isBlank()
                        )
                        .count();

        long orientacionesPendientes
                = Math.max(
                        0,
                        orientacionesSolicitadas
                        - orientacionesRespondidas
                );

        // =====================================================
        // PENSIÓN PROMEDIO
        // =====================================================
        java.math.BigDecimal sumaPensiones
                = previsiones
                        .stream()
                        .map(
                                com.segurplan.model.Prevision::getMontoPensionEstimado
                        )
                        .filter(
                                java.util.Objects::nonNull
                        )
                        .reduce(
                                java.math.BigDecimal.ZERO,
                                java.math.BigDecimal::add
                        );

        long cantidadPensiones
                = previsiones
                        .stream()
                        .map(
                                com.segurplan.model.Prevision::getMontoPensionEstimado
                        )
                        .filter(
                                java.util.Objects::nonNull
                        )
                        .count();

        java.math.BigDecimal pensionPromedio
                = cantidadPensiones == 0
                        ? java.math.BigDecimal.ZERO
                        : sumaPensiones.divide(
                                java.math.BigDecimal.valueOf(
                                        cantidadPensiones
                                ),
                                2,
                                java.math.RoundingMode.HALF_UP
                        );

        // =====================================================
        // INGRESO PROMEDIO
        // =====================================================
        java.math.BigDecimal sumaIngresos
                = previsiones
                        .stream()
                        .map(
                                com.segurplan.model.Prevision::getIngresoMensual
                        )
                        .filter(
                                java.util.Objects::nonNull
                        )
                        .reduce(
                                java.math.BigDecimal.ZERO,
                                java.math.BigDecimal::add
                        );

        long cantidadIngresos
                = previsiones
                        .stream()
                        .map(
                                com.segurplan.model.Prevision::getIngresoMensual
                        )
                        .filter(
                                java.util.Objects::nonNull
                        )
                        .count();

        java.math.BigDecimal ingresoPromedio
                = cantidadIngresos == 0
                        ? java.math.BigDecimal.ZERO
                        : sumaIngresos.divide(
                                java.math.BigDecimal.valueOf(
                                        cantidadIngresos
                                ),
                                2,
                                java.math.RoundingMode.HALF_UP
                        );

        // =====================================================
        // DETALLE
        // =====================================================
        java.util.List<Map<String, Object>> detalle
                = previsiones
                        .stream()
                        .map(this::convertirPrevisionReporte)
                        .toList();

        // =====================================================
        // RESPUESTA
        // =====================================================
        Map<String, Object> reporte
                = new LinkedHashMap<>();

        reporte.put("anio", anio);
        reporte.put("mes", mes);

        reporte.put(
                "totalSimulaciones",
                totalSimulaciones
        );

        reporte.put(
                "usuarios",
                usuarios
        );

        reporte.put(
                "orientacionesSolicitadas",
                orientacionesSolicitadas
        );

        reporte.put(
                "orientacionesRespondidas",
                orientacionesRespondidas
        );

        reporte.put(
                "orientacionesPendientes",
                orientacionesPendientes
        );

        reporte.put(
                "pensionPromedio",
                pensionPromedio
        );

        reporte.put(
                "ingresoPromedio",
                ingresoPromedio
        );

        reporte.put(
                "simulaciones",
                detalle
        );

        return reporte;
    }

// =========================================================
// CONVERTIR PREVISIÓN PARA REPORTE
// =========================================================
    private Map<String, Object> convertirPrevisionReporte(
            com.segurplan.model.Prevision p) {

        Map<String, Object> item
                = new LinkedHashMap<>();

        item.put(
                "idPrevision",
                p.getIdPrevision()
        );

        item.put(
                "fechaRegistro",
                p.getFechaRegistro()
        );

        item.put(
                "tipoSistema",
                p.getTipoSistema()
        );

        item.put(
                "edad",
                p.getEdad()
        );

        item.put(
                "ingresoMensual",
                p.getIngresoMensual()
        );

        item.put(
                "aniosAporte",
                p.getAniosAporte()
        );

        item.put(
                "montoPensionEstimado",
                p.getMontoPensionEstimado()
        );

        item.put(
                "consulta",
                p.getConsulta()
        );

        item.put(
                "respuesta",
                p.getRespuesta()
        );

        item.put(
                "estado",
                p.getEstado()
        );

        if (p.getUsuario() != null) {

            item.put(
                    "idUsuario",
                    p.getUsuario()
                            .getIdUsuario()
            );

            item.put(
                    "usuario",
                    p.getUsuario()
                            .getNombres()
                    + " "
                    + p.getUsuario()
                            .getApellidos()
            );

        } else {

            item.put(
                    "idUsuario",
                    null
            );

            item.put(
                    "usuario",
                    "—"
            );
        }

        return item;
    }
    // =========================================================
// REP-MV-01 - COTIZACIONES POR ESTADO
// =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> generarCotizacionesPorEstado(
            Integer anio,
            Integer mes) {

        validarPeriodo(anio, mes);

        YearMonth periodo = YearMonth.of(anio, mes);

        LocalDateTime inicio = periodo
                .atDay(1)
                .atStartOfDay();

        LocalDateTime fin = periodo
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay();

        long solicitadas = contarCotizaciones(inicio, fin, "Solicitada");
        long enRevision = contarCotizaciones(inicio, fin, "En revisión");
        long disponibles = contarCotizaciones(inicio, fin, "Disponible");
        long aceptadas = contarCotizaciones(inicio, fin, "Aceptada");
        long rechazadas = contarCotizaciones(inicio, fin, "Rechazada");
        long vencidas = contarCotizaciones(inicio, fin, "Vencida");

        long total = contarCotizaciones(inicio, fin, null);

        Map<String, Object> reporte = new LinkedHashMap<>();

        reporte.put("anio", anio);
        reporte.put("mes", mes);
        reporte.put("total", total);

        Map<String, Long> estados = new LinkedHashMap<>();

        estados.put("Solicitada", solicitadas);
        estados.put("En revisión", enRevision);
        estados.put("Disponible", disponibles);
        estados.put("Aceptada", aceptadas);
        estados.put("Rechazada", rechazadas);
        estados.put("Vencida", vencidas);

        reporte.put("estados", estados);

        return reporte;
    }
// =========================================================
// REP-MV-02 - CONVERSIÓN DE VENTAS
// =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> generarConversionVentas(
            Integer anio,
            Integer mes) {

        validarPeriodo(anio, mes);

        YearMonth periodo = YearMonth.of(anio, mes);

        LocalDateTime inicio
                = periodo.atDay(1).atStartOfDay();

        LocalDateTime fin
                = periodo.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        // ============================================
        // 1. TOTAL DE SOLICITUDES
        // ============================================
        Long solicitudes = entityManager.createQuery("""
        SELECT COUNT(c)
        FROM Cotizacion c
        WHERE c.fechaSolicitud >= :inicio
          AND c.fechaSolicitud < :fin
        """, Long.class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getSingleResult();

        // ============================================
        // 2. COTIZACIONES RESPONDIDAS
        //
        // Consideramos respondida cuando ya existe
        // fechaRespuesta.
        // ============================================
        Long respondidas = entityManager.createQuery("""
        SELECT COUNT(c)
        FROM Cotizacion c
        WHERE c.fechaSolicitud >= :inicio
          AND c.fechaSolicitud < :fin
          AND c.fechaRespuesta IS NOT NULL
        """, Long.class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getSingleResult();

        // ============================================
        // 3. COMPRAS / CONTRATACIONES
        //
        // Contamos contrataciones correspondientes
        // a cotizaciones solicitadas en el período.
        // ============================================
        Long compras = entityManager.createQuery("""
        SELECT COUNT(ct)
        FROM Contratacion ct
        WHERE ct.cotizacion.fechaSolicitud >= :inicio
          AND ct.cotizacion.fechaSolicitud < :fin
        """, Long.class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getSingleResult();

        // ============================================
        // 4. PÓLIZAS EMITIDAS
        // ============================================
        Long polizas = entityManager.createQuery("""
        SELECT COUNT(p)
        FROM Poliza p
        WHERE p.contratacion.cotizacion.fechaSolicitud >= :inicio
          AND p.contratacion.cotizacion.fechaSolicitud < :fin
        """, Long.class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getSingleResult();

        // ============================================
        // TASA DE CONVERSIÓN
        // ============================================
        double tasaConversion = 0.0;

        if (solicitudes != null && solicitudes > 0) {

            tasaConversion
                    = (polizas.doubleValue()
                    / solicitudes.doubleValue())
                    * 100.0;
        }

        tasaConversion
                = Math.round(tasaConversion * 100.0)
                / 100.0;

        // ============================================
        // RESPUESTA
        // ============================================
        Map<String, Object> reporte
                = new LinkedHashMap<>();

        reporte.put("anio", anio);
        reporte.put("mes", mes);

        reporte.put(
                "solicitudes",
                solicitudes
        );

        reporte.put(
                "respondidas",
                respondidas
        );

        reporte.put(
                "compras",
                compras
        );

        reporte.put(
                "polizasEmitidas",
                polizas
        );

        reporte.put(
                "tasaConversion",
                tasaConversion
        );

        return reporte;
    }
// =========================================================
// REP-MV-05 - CARTERA DE PÓLIZAS
// =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> generarCarteraPolizas(
            Integer anio,
            Integer mes) {

        validarPeriodo(anio, mes);

        YearMonth periodo = YearMonth.of(anio, mes);

        LocalDate inicio = periodo.atDay(1);
        LocalDate fin = periodo.plusMonths(1).atDay(1);

        // -----------------------------------------------------
        // CONSULTAR PÓLIZAS DEL PERÍODO
        // -----------------------------------------------------
        List<Poliza> polizas = entityManager.createQuery("""
        SELECT p
        FROM Poliza p
        WHERE p.fechaInicio >= :inicio
          AND p.fechaInicio < :fin
        ORDER BY p.fechaInicio DESC
        """, Poliza.class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getResultList();

        // -----------------------------------------------------
        // INDICADORES
        // -----------------------------------------------------
        long total = polizas.size();

        long vigentes = polizas.stream()
                .filter(p
                        -> "Vigente".equalsIgnoreCase(
                        p.getEstado()
                )
                )
                .count();

        long vencidas = polizas.stream()
                .filter(p
                        -> "Vencida".equalsIgnoreCase(
                        p.getEstado()
                )
                )
                .count();

        BigDecimal primaTotal = polizas.stream()
                .map(Poliza::getPrima)
                .filter(Objects::nonNull)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        // -----------------------------------------------------
        // DETALLE
        // -----------------------------------------------------
        List<Map<String, Object>> detalle
                = new ArrayList<>();

        for (Poliza p : polizas) {

            Map<String, Object> fila
                    = new LinkedHashMap<>();

            fila.put(
                    "idPoliza",
                    p.getIdPoliza()
            );

            fila.put(
                    "numeroPoliza",
                    p.getNumeroPoliza()
            );

            fila.put(
                    "fechaInicio",
                    p.getFechaInicio()
            );

            fila.put(
                    "fechaFin",
                    p.getFechaFin()
            );

            fila.put(
                    "prima",
                    p.getPrima()
            );

            fila.put(
                    "cobertura",
                    p.getCobertura()
            );

            fila.put(
                    "deducible",
                    p.getDeducible()
            );

            fila.put(
                    "estado",
                    p.getEstado()
            );

            detalle.add(fila);
        }

        // -----------------------------------------------------
        // RESPUESTA
        // -----------------------------------------------------
        Map<String, Object> reporte
                = new LinkedHashMap<>();

        reporte.put("anio", anio);
        reporte.put("mes", mes);

        reporte.put(
                "total",
                total
        );

        reporte.put(
                "vigentes",
                vigentes
        );

        reporte.put(
                "vencidas",
                vencidas
        );

        reporte.put(
                "primaTotal",
                primaTotal
        );

        reporte.put(
                "polizas",
                detalle
        );

        return reporte;
    }
// =========================================================
// REP-LA-01 - DOCUMENTOS POR TIPO Y ESTADO
// =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> generarDocumentosPorTipoEstado(
            Integer anio,
            Integer mes) {

        validarPeriodo(anio, mes);

        YearMonth periodo = YearMonth.of(anio, mes);

        LocalDateTime inicio
                = periodo.atDay(1)
                        .atStartOfDay();

        LocalDateTime fin
                = periodo.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        // =====================================================
        // DOCUMENTOS DEL PERÍODO
        // =====================================================
        List<Documento> documentos
                = entityManager.createQuery("""
                SELECT d
                FROM Documento d
                WHERE d.fechaCarga >= :inicio
                  AND d.fechaCarga < :fin
                ORDER BY d.fechaCarga DESC
                """, Documento.class)
                        .setParameter("inicio", inicio)
                        .setParameter("fin", fin)
                        .getResultList();

        // =====================================================
        // KPI POR ESTADO
        // =====================================================
        long total
                = documentos.size();

        long pendientes
                = documentos.stream()
                        .filter(d
                                -> "Pendiente".equalsIgnoreCase(
                                d.getEstado()
                        )
                        )
                        .count();

        long validados
                = documentos.stream()
                        .filter(d
                                -> "Validado".equalsIgnoreCase(
                                d.getEstado()
                        )
                        )
                        .count();

        long observados
                = documentos.stream()
                        .filter(d
                                -> "Observado".equalsIgnoreCase(
                                d.getEstado()
                        )
                        )
                        .count();

        long rechazados
                = documentos.stream()
                        .filter(d
                                -> "Rechazado".equalsIgnoreCase(
                                d.getEstado()
                        )
                        )
                        .count();

        // =====================================================
        // AGRUPACIÓN POR TIPO
        // =====================================================
        Map<String, Long> porTipo
                = documentos.stream()
                        .collect(
                                java.util.stream.Collectors.groupingBy(
                                        d -> {

                                            if (d.getTipoDocumento() == null
                                            || d.getTipoDocumento().isBlank()) {

                                                return "Sin especificar";
                                            }

                                            return d.getTipoDocumento();
                                        },
                                        LinkedHashMap::new,
                                        java.util.stream.Collectors.counting()
                                )
                        );

        // =====================================================
        // DETALLE DE DOCUMENTOS
        // =====================================================
        List<Map<String, Object>> detalle
                = new ArrayList<>();

        for (Documento documento : documentos) {

            Map<String, Object> fila
                    = new LinkedHashMap<>();

            fila.put(
                    "idDocumento",
                    documento.getIdDocumento()
            );

            fila.put(
                    "tipoDocumento",
                    documento.getTipoDocumento()
            );

            fila.put(
                    "nombreArchivo",
                    documento.getNombreArchivo()
            );

            fila.put(
                    "version",
                    documento.getVersion()
            );

            fila.put(
                    "fechaCarga",
                    documento.getFechaCarga()
            );

            fila.put(
                    "estado",
                    documento.getEstado()
            );

            detalle.add(
                    fila
            );
        }

        // =====================================================
        // RESPUESTA
        // =====================================================
        Map<String, Object> reporte
                = new LinkedHashMap<>();

        reporte.put(
                "anio",
                anio
        );

        reporte.put(
                "mes",
                mes
        );

        reporte.put(
                "total",
                total
        );

        reporte.put(
                "pendientes",
                pendientes
        );

        reporte.put(
                "validados",
                validados
        );

        reporte.put(
                "observados",
                observados
        );

        reporte.put(
                "rechazados",
                rechazados
        );

        reporte.put(
                "porTipo",
                porTipo
        );

        reporte.put(
                "documentos",
                detalle
        );

        return reporte;
    }
// =========================================================
// REP-PO-01 - REPORTE AFP
// =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteAfpPorPeriodo(
            Integer anio,
            Integer mes) {

        validarPeriodo(anio, mes);

        YearMonth periodo
                = YearMonth.of(anio, mes);

        LocalDateTime inicio
                = periodo.atDay(1)
                        .atStartOfDay();

        LocalDateTime fin
                = periodo.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        // =====================================================
        // CONSULTAS AFP DEL PERÍODO
        // =====================================================
        List<Prevision> registros
                = entityManager.createQuery("""
                SELECT p
                FROM Prevision p
                WHERE p.fechaRegistro >= :inicio
                  AND p.fechaRegistro < :fin
                  AND UPPER(p.tipoSistema) = 'AFP'
                ORDER BY p.fechaRegistro DESC
                """, Prevision.class)
                        .setParameter("inicio", inicio)
                        .setParameter("fin", fin)
                        .getResultList();

        // =====================================================
        // KPI
        // =====================================================
        long total
                = registros.size();

        long registradas
                = registros.stream()
                        .filter(p
                                -> "Registrada".equalsIgnoreCase(
                                p.getEstado()
                        )
                        )
                        .count();

        long enRevision
                = registros.stream()
                        .filter(p
                                -> "En revisión".equalsIgnoreCase(
                                p.getEstado()
                        )
                        )
                        .count();

        long simuladas
                = registros.stream()
                        .filter(p
                                -> "Simulada".equalsIgnoreCase(
                                p.getEstado()
                        )
                        )
                        .count();

        long respondidas
                = registros.stream()
                        .filter(p
                                -> "Respondida".equalsIgnoreCase(
                                p.getEstado()
                        )
                        )
                        .count();

        long cerradas
                = registros.stream()
                        .filter(p
                                -> "Cerrada".equalsIgnoreCase(
                                p.getEstado()
                        )
                        )
                        .count();

        // =====================================================
        // DETALLE
        // =====================================================
        List<Map<String, Object>> detalle
                = new ArrayList<>();

        for (Prevision prevision : registros) {

            Map<String, Object> fila
                    = new LinkedHashMap<>();

            fila.put(
                    "idPrevision",
                    prevision.getIdPrevision()
            );

            fila.put(
                    "tipoSistema",
                    prevision.getTipoSistema()
            );

            fila.put(
                    "edad",
                    prevision.getEdad()
            );

            fila.put(
                    "ingresoMensual",
                    prevision.getIngresoMensual()
            );

            fila.put(
                    "aniosAporte",
                    prevision.getAniosAporte()
            );

            fila.put(
                    "fechaRegistro",
                    prevision.getFechaRegistro()
            );

            fila.put(
                    "montoPensionEstimado",
                    prevision.getMontoPensionEstimado()
            );

            fila.put(
                    "resultadoSimulacion",
                    prevision.getResultadoSimulacion()
            );

            fila.put(
                    "consulta",
                    prevision.getConsulta()
            );

            fila.put(
                    "respuesta",
                    prevision.getRespuesta()
            );

            fila.put(
                    "estado",
                    prevision.getEstado()
            );

            detalle.add(
                    fila
            );
        }

        // =====================================================
        // RESPUESTA
        // =====================================================
        Map<String, Object> reporte
                = new LinkedHashMap<>();

        reporte.put(
                "anio",
                anio
        );

        reporte.put(
                "mes",
                mes
        );

        reporte.put(
                "total",
                total
        );

        reporte.put(
                "registradas",
                registradas
        );

        reporte.put(
                "enRevision",
                enRevision
        );

        reporte.put(
                "simuladas",
                simuladas
        );

        reporte.put(
                "respondidas",
                respondidas
        );

        reporte.put(
                "cerradas",
                cerradas
        );

        reporte.put(
                "registros",
                detalle
        );

        return reporte;
    }
    // =========================================================
// REP-PO-03 - SINIESTROS POR ASEGURADORA
// =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> generarSiniestrosPorAseguradora(
            Integer anio,
            Integer mes) {

        validarPeriodo(anio, mes);

        YearMonth periodo
                = YearMonth.of(anio, mes);

        LocalDateTime inicio
                = periodo.atDay(1)
                        .atStartOfDay();

        LocalDateTime fin
                = periodo.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        // =====================================================
        // CONSULTA AGRUPADA POR ASEGURADORA
        // =====================================================
        List<Object[]> resultados
                = entityManager.createQuery("""
            SELECT
                a.idAseguradora,
                a.nombre,
                COUNT(s),

                SUM(
                    CASE
                        WHEN LOWER(s.estado) = 'aprobado'
                        THEN 1
                        ELSE 0
                    END
                ),

                SUM(
                    CASE
                        WHEN LOWER(s.estado) = 'rechazado'
                        THEN 1
                        ELSE 0
                    END
                ),

                SUM(
                    CASE
                        WHEN LOWER(s.estado) = 'observado'
                        THEN 1
                        ELSE 0
                    END
                ),

                SUM(
                    CASE
                        WHEN s.resultadoEvaluacion IS NOT NULL
                        THEN 1
                        ELSE 0
                    END
                )

            FROM Siniestro s

            JOIN s.aseguradora a

            WHERE s.fechaRegistro >= :inicio
              AND s.fechaRegistro < :fin

            GROUP BY
                a.idAseguradora,
                a.nombre

            ORDER BY COUNT(s) DESC
            """, Object[].class)
                        .setParameter(
                                "inicio",
                                inicio
                        )
                        .setParameter(
                                "fin",
                                fin
                        )
                        .getResultList();

        // =====================================================
        // DETALLE
        // =====================================================
        List<Map<String, Object>> aseguradoras
                = new ArrayList<>();

        long totalSiniestros = 0;
        long totalAprobados = 0;
        long totalRechazados = 0;
        long totalObservados = 0;
        long totalConResultado = 0;

        for (Object[] resultado : resultados) {

            Long idAseguradora
                    = ((Number) resultado[0])
                            .longValue();

            String nombre
                    = String.valueOf(
                            resultado[1]
                    );

            long total
                    = resultado[2] == null
                            ? 0
                            : ((Number) resultado[2])
                                    .longValue();

            long aprobados
                    = resultado[3] == null
                            ? 0
                            : ((Number) resultado[3])
                                    .longValue();

            long rechazados
                    = resultado[4] == null
                            ? 0
                            : ((Number) resultado[4])
                                    .longValue();

            long observados
                    = resultado[5] == null
                            ? 0
                            : ((Number) resultado[5])
                                    .longValue();

            long conResultado
                    = resultado[6] == null
                            ? 0
                            : ((Number) resultado[6])
                                    .longValue();

            totalSiniestros += total;
            totalAprobados += aprobados;
            totalRechazados += rechazados;
            totalObservados += observados;
            totalConResultado += conResultado;

            Map<String, Object> fila
                    = new LinkedHashMap<>();

            fila.put(
                    "idAseguradora",
                    idAseguradora
            );

            fila.put(
                    "aseguradora",
                    nombre
            );

            fila.put(
                    "total",
                    total
            );

            fila.put(
                    "aprobados",
                    aprobados
            );

            fila.put(
                    "rechazados",
                    rechazados
            );

            fila.put(
                    "observados",
                    observados
            );

            fila.put(
                    "conResultado",
                    conResultado
            );

            aseguradoras.add(
                    fila
            );
        }

        // =====================================================
        // RESPUESTA
        // =====================================================
        Map<String, Object> reporte
                = new LinkedHashMap<>();

        reporte.put(
                "anio",
                anio
        );

        reporte.put(
                "mes",
                mes
        );

        reporte.put(
                "totalSiniestros",
                totalSiniestros
        );

        reporte.put(
                "totalAseguradoras",
                aseguradoras.size()
        );

        reporte.put(
                "aprobados",
                totalAprobados
        );

        reporte.put(
                "rechazados",
                totalRechazados
        );

        reporte.put(
                "observados",
                totalObservados
        );

        reporte.put(
                "conResultado",
                totalConResultado
        );

        reporte.put(
                "aseguradoras",
                aseguradoras
        );

        return reporte;
    }
    // =========================================================
// REP-DG-02 - INDICADORES GERENCIALES
// =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> generarIndicadoresGerenciales(
            Integer anio,
            Integer mes) {

        validarPeriodo(anio, mes);

        YearMonth periodo = YearMonth.of(anio, mes);

        LocalDateTime inicio = periodo
                .atDay(1)
                .atStartOfDay();

        LocalDateTime fin = periodo
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay();

        LocalDate inicioFecha = periodo.atDay(1);
        LocalDate finFecha = periodo
                .plusMonths(1)
                .atDay(1);

        // =====================================================
        // 1. COTIZACIONES DEL PERÍODO
        // =====================================================
        long totalCotizaciones = contarCotizaciones(
                inicio,
                fin,
                null
        );

        long cotizacionesAceptadas = contarCotizaciones(
                inicio,
                fin,
                "Aceptada"
        );

        BigDecimal tasaConversion = porcentaje(
                cotizacionesAceptadas,
                totalCotizaciones
        );

        // =====================================================
        // 2. PÓLIZAS DEL PERÍODO
        // =====================================================
        List<Poliza> polizas = entityManager.createQuery("""
        SELECT p
        FROM Poliza p
        WHERE p.fechaInicio >= :inicio
          AND p.fechaInicio < :fin
        """, Poliza.class)
                .setParameter("inicio", inicioFecha)
                .setParameter("fin", finFecha)
                .getResultList();

        long totalPolizas = polizas.size();

        long polizasVigentes = polizas.stream()
                .filter(p
                        -> "Vigente".equalsIgnoreCase(
                        p.getEstado()
                )
                )
                .count();

        BigDecimal primaTotal = polizas.stream()
                .map(Poliza::getPrima)
                .filter(Objects::nonNull)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        // =====================================================
        // 3. SINIESTROS DEL PERÍODO
        // =====================================================
        long totalSiniestros = contarSiniestros(
                inicio,
                fin,
                null
        );

        /*
     * Consideramos abiertos todos los siniestros que todavía
     * no están Cerrados, Aprobados o Rechazados.
         */
        Long siniestrosAbiertos = entityManager.createQuery("""
        SELECT COUNT(s)
        FROM Siniestro s
        WHERE s.fechaRegistro >= :inicio
          AND s.fechaRegistro < :fin
          AND s.estado NOT IN (
              'Cerrado',
              'Aprobado',
              'Rechazado'
          )
        """, Long.class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getSingleResult();

        long siniestrosAprobados = contarSiniestros(
                inicio,
                fin,
                "Aprobado"
        );

        long siniestrosRechazados = contarSiniestros(
                inicio,
                fin,
                "Rechazado"
        );

        // =====================================================
        // 4. AFP
        // =====================================================
        long totalAFP = contarPrevisiones(
                inicio,
                fin,
                null
        );

        long afpSimulaciones = contarPrevisionesSimuladas(
                inicio,
                fin
        );

        // =====================================================
        // 5. RESPUESTA
        // =====================================================
        Map<String, Object> reporte = new LinkedHashMap<>();

        reporte.put("anio", anio);
        reporte.put("mes", mes);

        // Indicadores principales
        reporte.put("tasaConversion", tasaConversion);
        reporte.put("primaTotal", primaTotal);
        reporte.put("siniestrosAbiertos", siniestrosAbiertos);
        reporte.put("polizasVigentes", polizasVigentes);

        // Cotizaciones
        reporte.put("totalCotizaciones", totalCotizaciones);
        reporte.put(
                "cotizacionesAceptadas",
                cotizacionesAceptadas
        );

        // Pólizas
        reporte.put("totalPolizas", totalPolizas);

        // Siniestros
        reporte.put("totalSiniestros", totalSiniestros);
        reporte.put(
                "siniestrosAprobados",
                siniestrosAprobados
        );
        reporte.put(
                "siniestrosRechazados",
                siniestrosRechazados
        );

        // AFP
        reporte.put("totalAFP", totalAFP);
        reporte.put("afpSimulaciones", afpSimulaciones);

        return reporte;
    }
// =========================================================
// REP-OT-02 - HISTORIAL Y TRAZABILIDAD
// =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> generarTrazabilidad(
            Integer anio,
            Integer mes) {

        validarPeriodo(anio, mes);

        YearMonth periodo
                = YearMonth.of(anio, mes);

        LocalDateTime inicio
                = periodo
                        .atDay(1)
                        .atStartOfDay();

        LocalDateTime fin
                = periodo
                        .plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        List<Historial> registros
                = historialRepository
                        .findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(
                                inicio,
                                fin
                        );

        long creaciones
                = registros.stream()
                        .filter(h
                                -> "CREAR".equalsIgnoreCase(h.getAccion())
                        || "REGISTRAR".equalsIgnoreCase(h.getAccion()))
                        .count();

        long actualizaciones
                = registros.stream()
                        .filter(h
                                -> "ACTUALIZAR".equalsIgnoreCase(h.getAccion()))
                        .count();

        long eliminaciones
                = registros.stream()
                        .filter(h
                                -> "ELIMINAR".equalsIgnoreCase(h.getAccion()))
                        .count();

        Map<String, Long> entidades
                = new LinkedHashMap<>();

        for (Historial historial : registros) {

            String entidad
                    = historial.getEntidad() != null
                    ? historial.getEntidad()
                    : "SIN ENTIDAD";

            entidades.put(
                    entidad,
                    entidades.getOrDefault(entidad, 0L) + 1
            );
        }

        List<Map<String, Object>> detalle
                = new ArrayList<>();

        for (Historial historial : registros) {

            Map<String, Object> fila
                    = new LinkedHashMap<>();

            fila.put(
                    "idHistorial",
                    historial.getIdHistorial()
            );

            fila.put(
                    "entidad",
                    historial.getEntidad()
            );

            fila.put(
                    "idRegistro",
                    historial.getIdRegistro()
            );

            fila.put(
                    "accion",
                    historial.getAccion()
            );

            fila.put(
                    "valorAnterior",
                    historial.getValorAnterior()
            );

            fila.put(
                    "valorNuevo",
                    historial.getValorNuevo()
            );

            fila.put(
                    "fechaHora",
                    historial.getFechaHora()
            );

            if (historial.getUsuario() != null) {

                fila.put(
                        "idUsuario",
                        historial.getUsuario().getIdUsuario()
                );

            } else {

                fila.put(
                        "idUsuario",
                        null
                );
            }

            detalle.add(fila);
        }

        Map<String, Object> reporte
                = new LinkedHashMap<>();

        reporte.put("anio", anio);
        reporte.put("mes", mes);

        reporte.put(
                "totalRegistros",
                registros.size()
        );

        reporte.put(
                "creaciones",
                creaciones
        );

        reporte.put(
                "actualizaciones",
                actualizaciones
        );

        reporte.put(
                "eliminaciones",
                eliminaciones
        );

        reporte.put(
                "entidades",
                entidades
        );

        reporte.put(
                "registros",
                detalle
        );

        return reporte;
    }
}

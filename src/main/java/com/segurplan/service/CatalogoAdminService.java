package com.segurplan.service;

import com.segurplan.dto.AseguradoraRequest;
import com.segurplan.dto.TipoSeguroRequest;
import com.segurplan.model.Aseguradora;
import com.segurplan.model.TipoSeguro;
import com.segurplan.repository.AseguradoraRepository;
import com.segurplan.repository.TipoSeguroRepository;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Mantenimiento (CRUD) de los catálogos Aseguradora y Tipo de seguro.
 * No se eliminan filas: se desactivan (baja lógica), porque las
 * cotizaciones y siniestros las referencian por clave foránea.
 */
@Service
public class CatalogoAdminService {

    private static final String ACTIVO = "Activo";
    private static final String INACTIVO = "Inactivo";

    private final AseguradoraRepository aseguradoraRepository;
    private final TipoSeguroRepository tipoSeguroRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialService historialService;

    public CatalogoAdminService(
            AseguradoraRepository aseguradoraRepository,
            TipoSeguroRepository tipoSeguroRepository,
            UsuarioRepository usuarioRepository,
            HistorialService historialService) {

        this.aseguradoraRepository = aseguradoraRepository;
        this.tipoSeguroRepository = tipoSeguroRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialService = historialService;
    }

    // =====================================================
    // ASEGURADORAS
    // =====================================================

    public List<Aseguradora> listarAseguradoras() {
        return aseguradoraRepository.findAll();
    }

    @Transactional
    public Aseguradora crearAseguradora(AseguradoraRequest r, String correoAdmin) {

        String nombre = limpiar(r.getNombre());
        String ruc = limpiar(r.getRuc());

        validarAseguradora(nombre, ruc, r.getCorreo());

        if (ruc != null && aseguradoraRepository.existsByRuc(ruc)) {
            throw new RuntimeException("Ya existe una aseguradora con ese RUC.");
        }

        Aseguradora a = new Aseguradora();
        a.setNombre(nombre);
        a.setRuc(ruc);
        a.setTelefono(limpiar(r.getTelefono()));
        a.setCorreo(limpiar(r.getCorreo()));
        a.setEstado(ACTIVO);

        Aseguradora guardada = aseguradoraRepository.save(a);

        historialService.registrar(idActor(correoAdmin), "Aseguradora",
                Long.valueOf(guardada.getIdAseguradora()), "CREAR", null, nombre);

        return guardada;
    }

    @Transactional
    public Aseguradora actualizarAseguradora(
            Integer id, AseguradoraRequest r, String correoAdmin) {

        Aseguradora a = aseguradoraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aseguradora no encontrada."));

        String nombre = limpiar(r.getNombre());
        String ruc = limpiar(r.getRuc());

        validarAseguradora(nombre, ruc, r.getCorreo());

        if (ruc != null && aseguradoraRepository
                .existsByRucAndIdAseguradoraNot(ruc, id)) {
            throw new RuntimeException("Ya existe otra aseguradora con ese RUC.");
        }

        String anterior = a.getNombre();

        a.setNombre(nombre);
        a.setRuc(ruc);
        a.setTelefono(limpiar(r.getTelefono()));
        a.setCorreo(limpiar(r.getCorreo()));

        Aseguradora guardada = aseguradoraRepository.save(a);

        historialService.registrar(idActor(correoAdmin), "Aseguradora",
                Long.valueOf(id), "ACTUALIZAR", anterior, nombre);

        return guardada;
    }

    @Transactional
    public Aseguradora cambiarEstadoAseguradora(
            Integer id, String estado, String correoAdmin) {

        validarEstado(estado);

        Aseguradora a = aseguradoraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aseguradora no encontrada."));

        String anterior = a.getEstado();
        a.setEstado(estado);

        Aseguradora guardada = aseguradoraRepository.save(a);

        historialService.registrar(idActor(correoAdmin), "Aseguradora",
                Long.valueOf(id), "CAMBIO_ESTADO", anterior, estado);

        return guardada;
    }

    // =====================================================
    // TIPOS DE SEGURO
    // =====================================================

    public List<TipoSeguro> listarTiposSeguro() {
        return tipoSeguroRepository.findAll();
    }

    @Transactional
    public TipoSeguro crearTipoSeguro(TipoSeguroRequest r, String correoAdmin) {

        String nombre = limpiar(r.getNombre());

        validarTipoSeguro(nombre, r.getDescripcion());

        if (tipoSeguroRepository.existsByNombreIgnoreCase(nombre)) {
            throw new RuntimeException("Ya existe un tipo de seguro con ese nombre.");
        }

        TipoSeguro t = new TipoSeguro();
        t.setNombre(nombre);
        t.setDescripcion(limpiar(r.getDescripcion()));
        t.setEstado(ACTIVO);

        TipoSeguro guardado = tipoSeguroRepository.save(t);

        historialService.registrar(idActor(correoAdmin), "TipoSeguro",
                Long.valueOf(guardado.getIdTipoSeguro()), "CREAR", null, nombre);

        return guardado;
    }

    @Transactional
    public TipoSeguro actualizarTipoSeguro(
            Integer id, TipoSeguroRequest r, String correoAdmin) {

        TipoSeguro t = tipoSeguroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de seguro no encontrado."));

        String nombre = limpiar(r.getNombre());

        validarTipoSeguro(nombre, r.getDescripcion());

        if (tipoSeguroRepository
                .existsByNombreIgnoreCaseAndIdTipoSeguroNot(nombre, id)) {
            throw new RuntimeException("Ya existe otro tipo de seguro con ese nombre.");
        }

        String anterior = t.getNombre();

        t.setNombre(nombre);
        t.setDescripcion(limpiar(r.getDescripcion()));

        TipoSeguro guardado = tipoSeguroRepository.save(t);

        historialService.registrar(idActor(correoAdmin), "TipoSeguro",
                Long.valueOf(id), "ACTUALIZAR", anterior, nombre);

        return guardado;
    }

    @Transactional
    public TipoSeguro cambiarEstadoTipoSeguro(
            Integer id, String estado, String correoAdmin) {

        validarEstado(estado);

        TipoSeguro t = tipoSeguroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de seguro no encontrado."));

        String anterior = t.getEstado();
        t.setEstado(estado);

        TipoSeguro guardado = tipoSeguroRepository.save(t);

        historialService.registrar(idActor(correoAdmin), "TipoSeguro",
                Long.valueOf(id), "CAMBIO_ESTADO", anterior, estado);

        return guardado;
    }

    // =====================================================
    // VALIDACIONES Y UTILIDADES
    // =====================================================

    private void validarAseguradora(String nombre, String ruc, String correo) {

        if (nombre == null) {
            throw new RuntimeException("El nombre de la aseguradora es obligatorio.");
        }

        if (nombre.length() > 150) {
            throw new RuntimeException("El nombre no puede superar 150 caracteres.");
        }

        if (ruc != null && !ruc.matches("\\d{11}")) {
            throw new RuntimeException("El RUC debe tener 11 dígitos numéricos.");
        }

        if (limpiar(correo) != null
                && !limpiar(correo).matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new RuntimeException("El correo no tiene un formato válido.");
        }
    }

    private void validarTipoSeguro(String nombre, String descripcion) {

        if (nombre == null) {
            throw new RuntimeException("El nombre del tipo de seguro es obligatorio.");
        }

        if (nombre.length() > 80) {
            throw new RuntimeException("El nombre no puede superar 80 caracteres.");
        }

        if (descripcion != null && descripcion.length() > 200) {
            throw new RuntimeException("La descripción no puede superar 200 caracteres.");
        }
    }

    private void validarEstado(String estado) {

        if (!ACTIVO.equals(estado) && !INACTIVO.equals(estado)) {
            throw new RuntimeException("Estado inválido. Use Activo o Inactivo.");
        }
    }

    private String limpiar(String valor) {

        if (valor == null) {
            return null;
        }

        String t = valor.trim();

        return t.isEmpty() ? null : t;
    }

    private Long idActor(String correo) {

        if (correo == null) {
            return null;
        }

        return usuarioRepository.findByCorreo(correo)
                .map(u -> u.getIdUsuario())
                .orElse(null);
    }
}

package com.segurplan.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Matriz de permisos por rol y módulo, editable por el administrador (RF030). */
@Service
public class PermisoAdminService {

    public static final List<String> MODULOS = List.of(
            "Cotizaciones", "Pólizas", "AFP", "Siniestros", "Documentos",
            "Usuarios", "Asesores", "Roles", "Reportes", "Historial", "Respaldo", "Catálogos");

    private final JdbcTemplate jdbc;
    private final HistorialService historialService;

    public PermisoAdminService(JdbcTemplate jdbc, HistorialService historialService) {
        this.jdbc = jdbc;
        this.historialService = historialService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtener(Integer idRol) {
        List<Map<String, Object>> filas = jdbc.query(
                "SELECT modulo, ver, crear, editar, eliminar FROM rol_permiso WHERE id_rol = ?",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("modulo", rs.getString("modulo"));
                    m.put("ver", rs.getBoolean("ver"));
                    m.put("crear", rs.getBoolean("crear"));
                    m.put("editar", rs.getBoolean("editar"));
                    m.put("eliminar", rs.getBoolean("eliminar"));
                    return m;
                }, idRol);
        // devuelve todos los módulos, aunque el rol no tenga fila
        Map<String, Map<String, Object>> porModulo = new LinkedHashMap<>();
        for (String modulo : MODULOS) {
            Map<String, Object> vacio = new LinkedHashMap<>();
            vacio.put("modulo", modulo);
            vacio.put("ver", false);
            vacio.put("crear", false);
            vacio.put("editar", false);
            vacio.put("eliminar", false);
            porModulo.put(modulo, vacio);
        }
        for (Map<String, Object> f : filas) {
            porModulo.put((String) f.get("modulo"), f);
        }
        return List.copyOf(porModulo.values());
    }

    @Transactional
    public void guardar(Long idUsuarioActor, Integer idRol, List<Map<String, Object>> permisos) {
        Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM rol WHERE id_rol = ?", Integer.class, idRol);
        if (existe == null || existe == 0) {
            throw new IllegalArgumentException("Rol no encontrado.");
        }
        Set<String> validos = Set.copyOf(MODULOS);
        for (Map<String, Object> p : permisos) {
            String modulo = String.valueOf(p.get("modulo"));
            if (!validos.contains(modulo)) {
                throw new IllegalArgumentException("Módulo no válido: " + modulo);
            }
            boolean ver = bool(p.get("ver"));
            boolean crear = bool(p.get("crear"));
            boolean editar = bool(p.get("editar"));
            boolean eliminar = bool(p.get("eliminar"));
            // crear/editar/eliminar sin "ver" no tiene sentido
            if (!ver && (crear || editar || eliminar)) {
                throw new IllegalArgumentException("El módulo " + modulo
                        + " no puede tener permisos de escritura sin permiso de lectura.");
            }
            jdbc.update("INSERT INTO rol_permiso (id_rol, modulo, ver, crear, editar, eliminar) "
                    + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (id_rol, modulo) DO UPDATE SET "
                    + "ver = EXCLUDED.ver, crear = EXCLUDED.crear, editar = EXCLUDED.editar, "
                    + "eliminar = EXCLUDED.eliminar",
                    idRol, modulo, ver, crear, editar, eliminar);
        }
        historialService.registrar(idUsuarioActor, "ROL", idRol.longValue(),
                "ACTUALIZAR_PERMISOS", null, "Matriz de permisos actualizada (" + permisos.size() + " módulos)");
    }

    private boolean bool(Object o) {
        return o instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(o));
    }
}

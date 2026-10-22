package com.segurplan.service;

import com.segurplan.dto.RolAdminDTO;
import com.segurplan.model.Rol;
import com.segurplan.repository.RolRepository;
import com.segurplan.repository.UsuarioRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RolAdminService {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    public RolAdminService(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository) {

        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // =====================================================
    // LISTAR ROLES
    // =====================================================

    public List<RolAdminDTO> listarRoles() {

        List<Rol> roles =
                rolRepository.findAll();

        List<RolAdminDTO> resultado =
                new ArrayList<>();

        for (Rol rol : roles) {

            long cantidadUsuarios =
                    usuarioRepository
                            .countByRol_IdRol(
                                    rol.getIdRol()
                            );

            List<String> permisos =
                    obtenerPermisos(
                            rol.getNombre()
                    );

            RolAdminDTO dto =
                    new RolAdminDTO(
                            rol.getIdRol(),
                            rol.getNombre(),
                            rol.getDescripcion(),
                            cantidadUsuarios,
                            permisos
                    );

            resultado.add(dto);
        }

        return resultado;
    }

    // =====================================================
    // OBTENER ROL
    // =====================================================

    public RolAdminDTO obtenerRol(
            Integer idRol) {

        Rol rol =
                rolRepository
                        .findById(idRol)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Rol no encontrado."
                                )
                        );

        long cantidadUsuarios =
                usuarioRepository
                        .countByRol_IdRol(
                                rol.getIdRol()
                        );

        return new RolAdminDTO(
                rol.getIdRol(),
                rol.getNombre(),
                rol.getDescripcion(),
                cantidadUsuarios,
                obtenerPermisos(
                        rol.getNombre()
                )
        );
    }

    // =====================================================
    // PERMISOS SEGÚN SECURITYCONFIG
    // =====================================================

    private List<String> obtenerPermisos(
            String nombreRol) {

        List<String> permisos =
                new ArrayList<>();

        if (nombreRol == null) {
            return permisos;
        }

        switch (
                nombreRol.trim().toLowerCase()
        ) {

            case "cliente" -> {

                permisos.add(
                        "Acceder al panel del cliente"
                );

                permisos.add(
                        "Solicitar cotizaciones"
                );

                permisos.add(
                        "Gestionar contrataciones propias"
                );

                permisos.add(
                        "Consultar sus pólizas"
                );

                permisos.add(
                        "Registrar y consultar sus siniestros"
                );

                permisos.add(
                        "Cargar documentos de sus siniestros"
                );

                permisos.add(
                        "Realizar simulaciones AFP"
                );

                permisos.add(
                        "Solicitar orientación AFP"
                );
            }

            case "asesor" -> {

                permisos.add(
                        "Acceder al panel del asesor"
                );

                permisos.add(
                        "Consultar siniestros para revisión"
                );

                permisos.add(
                        "Consultar documentos de siniestros"
                );

                permisos.add(
                        "Derivar siniestros"
                );

                permisos.add(
                        "Consultar solicitudes AFP"
                );

                permisos.add(
                        "Responder solicitudes de orientación AFP"
                );
            }

            case "administrador" -> {

                permisos.add(
                        "Acceder al panel administrativo"
                );

                permisos.add(
                        "Gestionar usuarios"
                );

                permisos.add(
                        "Gestionar asesores"
                );

                permisos.add(
                        "Consultar roles y permisos"
                );

                permisos.add(
                        "Gestionar cotizaciones"
                );

                permisos.add(
                        "Generar pólizas"
                );

                permisos.add(
                        "Consultar reportes"
                );

                permisos.add(
                        "Realizar funciones autorizadas del asesor"
                );
            }

            default -> {
                // Rol sin permisos administrativos definidos.
            }
        }

        return permisos;
    }
}
package com.segurplan.service;

import com.segurplan.dto.ActualizarUsuarioRequest;
import com.segurplan.dto.LoginRequest;
import com.segurplan.dto.LoginResponse;
import com.segurplan.dto.RegistroAsesorRequest;
import com.segurplan.dto.RegistroRequest;
import com.segurplan.dto.UsuarioAdminDTO;

import com.segurplan.model.Rol;
import com.segurplan.model.Usuario;

import com.segurplan.repository.RolRepository;
import com.segurplan.repository.UsuarioRepository;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final HistorialService historialService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            HistorialService historialService) {

        this.usuarioRepository =
                usuarioRepository;

        this.rolRepository =
                rolRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.historialService =
                historialService;
    }


    // =====================================================
    // REGISTRO DE CLIENTE
    // =====================================================

    @Transactional
    public Usuario registrar(
            RegistroRequest request) {

        if (request.getCorreo() == null
                || request.getCorreo().isBlank()) {

            throw new IllegalArgumentException(
                    "El correo es obligatorio"
            );
        }


        String correo =
                request.getCorreo()
                        .trim()
                        .toLowerCase();


        if (usuarioRepository.existsByCorreo(
                correo)) {

            throw new IllegalArgumentException(
                    "El correo ya está registrado"
            );
        }


        Rol rolCliente =
                rolRepository
                        .findByNombre("Cliente")
                        .orElseThrow(
                                () ->
                                    new IllegalArgumentException(
                                            "No existe el rol Cliente"
                                    )
                        );


        Usuario usuario =
                new Usuario();


        usuario.setNombres(
                request.getNombres()
        );

        usuario.setApellidos(
                request.getApellidos()
        );

        usuario.setCorreo(
                correo
        );

        usuario.setTelefono(
                request.getTelefono()
        );


        usuario.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );


        usuario.setRol(
                rolCliente
        );


        return usuarioRepository.save(
                usuario
        );
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @Transactional(readOnly = true)
    public LoginResponse login(
            LoginRequest request) {

        if (request.getCorreo() == null
                || request.getCorreo().isBlank()) {

            throw new IllegalArgumentException(
                    "Correo o contraseña incorrectos"
            );
        }


        String correo =
                request.getCorreo()
                        .trim()
                        .toLowerCase();


        Usuario usuario =
                usuarioRepository
                        .findByCorreo(correo)
                        .orElseThrow(
                                () ->
                                    new IllegalArgumentException(
                                            "Correo o contraseña incorrectos"
                                    )
                        );


        if (!"Activo".equalsIgnoreCase(
                usuario.getEstado())) {

            throw new IllegalArgumentException(
                    "El usuario no se encuentra activo"
            );
        }


        boolean passwordCorrecto =
                passwordEncoder.matches(
                        request.getPassword(),
                        usuario.getPassword()
                );


        if (!passwordCorrecto) {

            throw new IllegalArgumentException(
                    "Correo o contraseña incorrectos"
            );
        }


        return new LoginResponse(
                true,
                "Inicio de sesión correcto",
                usuario.getIdUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getRol().getNombre()
        );
    }


    // =====================================================
    // ADMINISTRACIÓN - LISTAR TODOS LOS USUARIOS
    // =====================================================

    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO> listarUsuarios() {

        return usuarioRepository
                .findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                Usuario::getIdUsuario
                        )
                )
                .map(
                        this::convertirDTO
                )
                .toList();
    }


    // =====================================================
    // ADMINISTRACIÓN - OBTENER USUARIO
    // =====================================================

    @Transactional(readOnly = true)
    public UsuarioAdminDTO obtenerUsuario(
            Long idUsuario) {

        Usuario usuario =
                buscarUsuario(
                        idUsuario
                );


        return convertirDTO(
                usuario
        );
    }


    // =====================================================
    // ADMINISTRACIÓN - ACTUALIZAR USUARIO
    // =====================================================

    @Transactional
    public UsuarioAdminDTO actualizarUsuario(
            Long idUsuario,
            ActualizarUsuarioRequest request) {

        Usuario usuario =
                buscarUsuario(
                        idUsuario
                );


        validarDatosActualizacion(
                request
        );


        usuario.setNombres(
                request.getNombres()
                        .trim()
        );


        usuario.setApellidos(
                request.getApellidos()
                        .trim()
        );


        asignarTelefono(
                usuario,
                request.getTelefono()
        );


        Usuario actualizado =
                usuarioRepository.save(
                        usuario
                );


        return convertirDTO(
                actualizado
        );
    }


    // =====================================================
    // ADMINISTRACIÓN - CAMBIAR ESTADO USUARIO
    // =====================================================

    @Transactional
    public UsuarioAdminDTO cambiarEstado(
            Long idUsuario,
            String estado) {

        Usuario usuario =
                buscarUsuario(
                        idUsuario
                );


        asignarEstado(
                usuario,
                estado
        );


        Usuario actualizado =
                usuarioRepository.save(
                        usuario
                );


        return convertirDTO(
                actualizado
        );
    }


    // =====================================================
    // ASESORES - LISTAR
    // =====================================================

    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO> listarAsesores() {

        return usuarioRepository
                .findByRol_NombreOrderByIdUsuarioAsc(
                        "Asesor"
                )
                .stream()
                .map(
                        this::convertirDTO
                )
                .toList();
    }


    // =====================================================
    // ASESORES - REGISTRAR
    // =====================================================

    @Transactional
    public UsuarioAdminDTO registrarAsesor(
            RegistroAsesorRequest request) {

        validarRegistroAsesor(
                request
        );


        String correo =
                request.getCorreo()
                        .trim()
                        .toLowerCase();


        if (usuarioRepository.existsByCorreo(
                correo)) {

            throw new IllegalArgumentException(
                    "El correo ya está registrado"
            );
        }


        Rol rolAsesor =
                rolRepository
                        .findByNombre("Asesor")
                        .orElseThrow(
                                () ->
                                    new IllegalArgumentException(
                                            "No existe el rol Asesor"
                                    )
                        );


        Usuario asesor =
                new Usuario();


        asesor.setNombres(
                request.getNombres()
                        .trim()
        );


        asesor.setApellidos(
                request.getApellidos()
                        .trim()
        );


        asesor.setCorreo(
                correo
        );


        asignarTelefono(
                asesor,
                request.getTelefono()
        );


        asesor.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );


        asesor.setRol(
                rolAsesor
        );


        Usuario guardado =
                usuarioRepository.save(
                        asesor
                );


        return convertirDTO(
                guardado
        );
    }


    // =====================================================
    // ASESORES - OBTENER POR ID
    // =====================================================

    @Transactional(readOnly = true)
    public UsuarioAdminDTO obtenerAsesor(
            Long idUsuario) {

        Usuario asesor =
                buscarAsesor(
                        idUsuario
                );


        return convertirDTO(
                asesor
        );
    }


    // =====================================================
    // ASESORES - ACTUALIZAR
    // =====================================================

    @Transactional
    public UsuarioAdminDTO actualizarAsesor(
            Long idUsuario,
            ActualizarUsuarioRequest request) {

        Usuario asesor =
                buscarAsesor(
                        idUsuario
                );


        validarDatosActualizacion(
                request
        );


        asesor.setNombres(
                request.getNombres()
                        .trim()
        );


        asesor.setApellidos(
                request.getApellidos()
                        .trim()
        );


        asignarTelefono(
                asesor,
                request.getTelefono()
        );


        Usuario actualizado =
                usuarioRepository.save(
                        asesor
                );


        return convertirDTO(
                actualizado
        );
    }


    // =====================================================
    // ASESORES - CAMBIAR ESTADO
    // =====================================================

    @Transactional
    public UsuarioAdminDTO cambiarEstadoAsesor(
            Long idUsuario,
            String estado) {

        Usuario asesor =
                buscarAsesor(
                        idUsuario
                );


        asignarEstado(
                asesor,
                estado
        );


        Usuario actualizado =
                usuarioRepository.save(
                        asesor
                );


        return convertirDTO(
                actualizado
        );
    }


    // =====================================================
    // ADMINISTRACIÓN - CAMBIAR ROL
    // =====================================================

    @Transactional
    public UsuarioAdminDTO cambiarRol(
            Long idUsuario,
            Integer idRol) {

        // =============================================
        // VALIDAR ID USUARIO
        // =============================================

        if (idUsuario == null) {

            throw new IllegalArgumentException(
                    "ID de usuario inválido"
            );
        }


        // =============================================
        // VALIDAR ID ROL
        // =============================================

        if (idRol == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un rol"
            );
        }


        // =============================================
        // BUSCAR USUARIO
        // =============================================

        Usuario usuario =
                buscarUsuario(
                        idUsuario
                );


        // =============================================
        // BUSCAR NUEVO ROL
        // =============================================

        Rol rolNuevo =
                rolRepository
                        .findById(idRol)
                        .orElseThrow(
                                () ->
                                    new IllegalArgumentException(
                                            "Rol no encontrado"
                                    )
                        );


        // =============================================
        // ROL ANTERIOR
        // =============================================

        String rolAnterior =
                usuario.getRol() != null
                        ? usuario.getRol()
                                .getNombre()
                        : "Sin rol";


        String nombreRolNuevo =
                rolNuevo.getNombre();


        // =============================================
        // SI EL ROL NO CAMBIÓ
        // =============================================

        if (
            rolAnterior.equalsIgnoreCase(
                    nombreRolNuevo
            )
        ) {

            return convertirDTO(
                    usuario
            );
        }


        // =============================================
        // CAMBIAR ROL
        // =============================================

        usuario.setRol(
                rolNuevo
        );


        Usuario actualizado =
                usuarioRepository.save(
                        usuario
                );


        // =============================================
        // USUARIO QUE REALIZÓ LA ACCIÓN
        // =============================================

        Long idAdministrador =
                obtenerIdUsuarioAutenticado();


        // =============================================
        // REGISTRAR HISTORIAL
        // =============================================

        historialService.registrar(
                idAdministrador,
                "Usuario",
                actualizado.getIdUsuario(),
                "CAMBIO_ROL",
                rolAnterior,
                nombreRolNuevo
        );


        return convertirDTO(
                actualizado
        );
    }


    // =====================================================
    // AUXILIAR - BUSCAR USUARIO
    // =====================================================

    private Usuario buscarUsuario(
            Long idUsuario) {

        if (idUsuario == null) {

            throw new IllegalArgumentException(
                    "ID de usuario inválido"
            );
        }


        return usuarioRepository
                .findById(
                        idUsuario
                )
                .orElseThrow(
                        () ->
                            new IllegalArgumentException(
                                    "Usuario no encontrado"
                            )
                );
    }


    // =====================================================
    // AUXILIAR - BUSCAR Y VALIDAR ASESOR
    // =====================================================

    private Usuario buscarAsesor(
            Long idUsuario) {

        Usuario usuario =
                buscarUsuario(
                        idUsuario
                );


        if (usuario.getRol() == null
                || !"Asesor".equalsIgnoreCase(
                        usuario.getRol()
                                .getNombre()
                )) {

            throw new IllegalArgumentException(
                    "El usuario indicado no es un asesor"
            );
        }


        return usuario;
    }


    // =====================================================
    // AUXILIAR - VALIDAR REGISTRO DE ASESOR
    // =====================================================

    private void validarRegistroAsesor(
            RegistroAsesorRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Los datos del asesor son obligatorios"
            );
        }


        if (request.getNombres() == null
                || request.getNombres().isBlank()) {

            throw new IllegalArgumentException(
                    "Los nombres son obligatorios"
            );
        }


        if (request.getApellidos() == null
                || request.getApellidos().isBlank()) {

            throw new IllegalArgumentException(
                    "Los apellidos son obligatorios"
            );
        }


        if (request.getCorreo() == null
                || request.getCorreo().isBlank()) {

            throw new IllegalArgumentException(
                    "El correo es obligatorio"
            );
        }


        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "La contraseña es obligatoria"
            );
        }


        if (request.getPassword().length() < 6) {

            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 6 caracteres"
            );
        }
    }


    // =====================================================
    // AUXILIAR - VALIDAR ACTUALIZACIÓN
    // =====================================================

    private void validarDatosActualizacion(
            ActualizarUsuarioRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Los datos son obligatorios"
            );
        }


        if (request.getNombres() == null
                || request.getNombres().isBlank()) {

            throw new IllegalArgumentException(
                    "Los nombres son obligatorios"
            );
        }


        if (request.getApellidos() == null
                || request.getApellidos().isBlank()) {

            throw new IllegalArgumentException(
                    "Los apellidos son obligatorios"
            );
        }
    }


    // =====================================================
    // AUXILIAR - ASIGNAR TELÉFONO
    // =====================================================

    private void asignarTelefono(
            Usuario usuario,
            String telefono) {

        if (telefono == null
                || telefono.isBlank()) {

            usuario.setTelefono(
                    null
            );

        } else {

            usuario.setTelefono(
                    telefono.trim()
            );
        }
    }


    // =====================================================
    // AUXILIAR - ASIGNAR ESTADO
    // =====================================================

    private void asignarEstado(
            Usuario usuario,
            String estado) {

        if (estado == null
                || estado.isBlank()) {

            throw new IllegalArgumentException(
                    "Debe indicar el estado"
            );
        }


        if ("Activo".equalsIgnoreCase(
                estado)) {

            usuario.setEstado(
                    "Activo"
            );

        } else if (
            "Inactivo".equalsIgnoreCase(
                    estado
            )
        ) {

            usuario.setEstado(
                    "Inactivo"
            );

        } else {

            throw new IllegalArgumentException(
                    "El estado debe ser Activo o Inactivo"
            );
        }
    }


    // =====================================================
    // AUXILIAR - CONVERTIR A DTO
    // =====================================================

    private UsuarioAdminDTO convertirDTO(
            Usuario usuario) {

        String nombreRol =
                "";


        if (usuario.getRol() != null) {

            nombreRol =
                    usuario.getRol()
                            .getNombre();
        }


        return new UsuarioAdminDTO(
                usuario.getIdUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getEstado(),
                usuario.getFechaRegistro(),
                nombreRol
        );
    }


    // =====================================================
    // AUXILIAR - OBTENER USUARIO AUTENTICADO
    // =====================================================

    private Long obtenerIdUsuarioAutenticado() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                        authentication.getName()
                )) {

            return null;
        }


        return usuarioRepository
                .findByCorreo(
                        authentication.getName()
                )
                .map(
                        Usuario::getIdUsuario
                )
                .orElse(
                        null
                );
    }
}
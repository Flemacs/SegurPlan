package com.segurplan.service;

import com.segurplan.dto.LoginRequest;
import com.segurplan.dto.LoginResponse;
import com.segurplan.dto.RegistroRequest;
import com.segurplan.model.Rol;
import com.segurplan.model.Usuario;
import com.segurplan.repository.RolRepository;
import com.segurplan.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de las reglas de negocio de UsuarioService.
 * Cubre el caso de prueba CP001 y los requisitos funcionales RF001, RF002, RF028 y RNF001.
 *
 * Utiliza Mockito para simular la persistencia y el encoder de contraseñas (BCrypt)
 * de forma aislada en memoria sin requerir conexión a base de datos.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String CORREO_VALIDO = "cliente@segurplan.pe";
    private static final String CLAVE_PLANA = "SegurPlan2026!";
    private static final String CLAVE_CIFRADA = "$2a$10$hashedBCryptStringExample123456789";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private UsuarioService usuarioService;

    // =====================================================
    // 1. REGISTRO DE USUARIOS (RF001, RNF001 / CP001)
    // =====================================================

    /**
     * Prueba 1: Registro correcto de usuario con contraseña cifrada (BCrypt) y rol Cliente.
     * Comprueba que la contraseña nunca se guarde en texto plano y se asigne el rol predeterminado.
     */
    @Test
    void registrar_datosValidos_cifraPasswordConBCryptYGuardaUsuario() {
        // Arrange (Preparar datos)
        RegistroRequest request = new RegistroRequest();
        request.setNombres("Carlos");
        request.setApellidos("Mendoza");
        request.setCorreo(CORREO_VALIDO);
        request.setTelefono("987654321");
        request.setPassword(CLAVE_PLANA);

        Rol rolCliente = new Rol(3, "Cliente", "Rol para clientes del sistema");

        when(usuarioRepository.existsByCorreo(CORREO_VALIDO)).thenReturn(false);
        when(rolRepository.findByNombre("Cliente")).thenReturn(Optional.of(rolCliente));
        when(passwordEncoder.encode(CLAVE_PLANA)).thenReturn(CLAVE_CIFRADA);

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(10L);
            return u;
        });

        // Act (Ejecutar)
        Usuario resultado = usuarioService.registrar(request);

        // Assert (Comprobar)
        assertNotNull(resultado);
        assertEquals(10L, resultado.getIdUsuario());
        assertEquals(CORREO_VALIDO, resultado.getCorreo());
        assertEquals(CLAVE_CIFRADA, resultado.getPassword()); // Verificación de cifrado BCrypt
        assertEquals("Cliente", resultado.getRol().getNombre());

        verify(passwordEncoder).encode(CLAVE_PLANA);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    /**
     * Prueba 2: Rechazo de registro cuando el correo electrónico ya se encuentra registrado.
     * Comprueba que no se permitan cuentas duplicadas y se cancele la persistencia.
     */
    @Test
    void registrar_correoDuplicado_lanzaExcepcion() {
        // Arrange
        RegistroRequest request = new RegistroRequest();
        request.setCorreo(CORREO_VALIDO);
        request.setPassword(CLAVE_PLANA);

        when(usuarioRepository.existsByCorreo(CORREO_VALIDO)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.registrar(request));

        assertEquals("El correo ya está registrado", ex.getMessage());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // =====================================================
    // 2. INICIO DE SESIÓN Y CONTROL DE ACCESO (RF002, RF028 / CP001)
    // =====================================================

    /**
     * Prueba 3: Rechazo de inicio de sesión cuando el correo no existe en el sistema.
     * Comprueba el mensaje de seguridad estándar para evitar enumeración de usuarios.
     */
    @Test
    void login_usuarioInexistente_lanzaExcepcion() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setCorreo("noexiste@segurplan.pe");
        request.setPassword("Cualquiera123");

        when(usuarioRepository.findByCorreo("noexiste@segurplan.pe"))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.login(request));

        assertEquals("Correo o contraseña incorrectos", ex.getMessage());
    }

    /**
     * Prueba 4: Rechazo de inicio de sesión cuando el usuario se encuentra inactivo.
     * Comprueba que los usuarios desactivados por administración no puedan acceder a la plataforma.
     */
    @Test
    void login_usuarioInactivo_lanzaExcepcion() {
        // Arrange
        Usuario usuarioInactivo = new Usuario();
        usuarioInactivo.setIdUsuario(15L);
        usuarioInactivo.setCorreo(CORREO_VALIDO);
        usuarioInactivo.setEstado("Inactivo"); // Usuario bloqueado o suspendido

        when(usuarioRepository.findByCorreo(CORREO_VALIDO))
                .thenReturn(Optional.of(usuarioInactivo));

        LoginRequest request = new LoginRequest();
        request.setCorreo(CORREO_VALIDO);
        request.setPassword(CLAVE_PLANA);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.login(request));

        assertEquals("El usuario no se encuentra activo", ex.getMessage());
    }

    /**
     * Prueba 5: Rechazo de inicio de sesión ante contraseña incorrecta.
     * Comprueba que el comparador BCrypt (matches) detecte la discrepancia y deniegue el acceso.
     */
    @Test
    void login_passwordIncorrecto_lanzaExcepcion() {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(10L);
        usuario.setCorreo(CORREO_VALIDO);
        usuario.setPassword(CLAVE_CIFRADA);
        usuario.setEstado("Activo");

        when(usuarioRepository.findByCorreo(CORREO_VALIDO))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("ClaveErronea", CLAVE_CIFRADA))
                .thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setCorreo(CORREO_VALIDO);
        request.setPassword("ClaveErronea");

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.login(request));

        assertEquals("Correo o contraseña incorrectos", ex.getMessage());
    }

    /**
     * Prueba 6: Inicio de sesión exitoso con credenciales correctas.
     * Comprueba que se retorne LoginResponse con los datos del usuario autenticado y su rol.
     */
    @Test
    void login_credencialesValidas_retornaLoginResponseExitoso() {
        // Arrange
        Rol rolAsesor = new Rol(2, "Asesor", "Rol de asesor comercial");

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(5L);
        usuario.setNombres("René");
        usuario.setApellidos("Valencia");
        usuario.setCorreo("asesor@segurplan.pe");
        usuario.setPassword(CLAVE_CIFRADA);
        usuario.setEstado("Activo");
        usuario.setRol(rolAsesor);

        when(usuarioRepository.findByCorreo("asesor@segurplan.pe"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE_PLANA, CLAVE_CIFRADA))
                .thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setCorreo("asesor@segurplan.pe");
        request.setPassword(CLAVE_PLANA);

        // Act
        LoginResponse respuesta = usuarioService.login(request);

        // Assert
        assertNotNull(respuesta);
        assertTrue(respuesta.isExito());
        assertEquals("Inicio de sesión correcto", respuesta.getMensaje());
        assertEquals(5L, respuesta.getIdUsuario());
        assertEquals("René", respuesta.getNombres());
        assertEquals("asesor@segurplan.pe", respuesta.getCorreo());
        assertEquals("Asesor", respuesta.getRol());
    }
}

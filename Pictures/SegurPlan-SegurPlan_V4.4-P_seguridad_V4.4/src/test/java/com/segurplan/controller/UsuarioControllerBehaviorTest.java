
package com.segurplan.controller;

import com.segurplan.dto.ActualizarUsuarioRequest;
import com.segurplan.dto.LoginRequest;
import com.segurplan.dto.LoginResponse;
import com.segurplan.dto.RegistroRequest;
import com.segurplan.dto.UsuarioAdminDTO;
import com.segurplan.model.Rol;
import com.segurplan.model.Usuario;
import com.segurplan.service.SegurplanUserDetailsService;
import com.segurplan.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioControllerBehaviorTest {

    private UsuarioService service;
    private SegurplanUserDetailsService detailsService;
    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        service = mock(UsuarioService.class);
        detailsService = mock(SegurplanUserDetailsService.class);

        controller = new UsuarioController(
                service,
                detailsService,
                mock(com.segurplan.service.NotificacionService.class),
                new com.segurplan.service.LoginAttemptService());

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();

        usuario.setIdUsuario(1L);
        usuario.setNombres("Juan");
        usuario.setApellidos("Perez");
        usuario.setCorreo("juan@test.com");
        usuario.setTelefono("999888777");
        usuario.setEstado("Activo");
        usuario.setFechaRegistro(
                LocalDateTime.of(2026, 11, 10, 10, 0)
        );
        usuario.setRol(
                new Rol(1, "Cliente", "Usuario cliente")
        );

        return usuario;
    }

    private UsuarioAdminDTO crearDTO() {
        return new UsuarioAdminDTO(
                1L,
                "Juan",
                "Perez",
                "juan@test.com",
                "999888777",
                "Activo",
                LocalDateTime.of(2026, 11, 10, 10, 0),
                "Cliente"
        );
    }

    @Test
    void registrarCorrectamente() {
        RegistroRequest request = new RegistroRequest();

        request.setNombres("Juan");
        request.setApellidos("Perez");
        request.setCorreo("juan@test.com");
        request.setTelefono("999888777");
        request.setPassword("123456");

        when(service.registrar(request))
                .thenReturn(crearUsuario());

        ResponseEntity<?> respuesta =
                controller.registrar(request);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "Usuario registrado correctamente",
                body.get("mensaje")
        );
        assertEquals(1L, body.get("idUsuario"));
        assertEquals("Juan", body.get("nombres"));
        assertEquals("Perez", body.get("apellidos"));
        assertEquals("juan@test.com", body.get("correo"));
        assertEquals("Cliente", body.get("rol"));

        verify(service).registrar(request);
    }

    @Test
    void registrarConError() {
        RegistroRequest request = new RegistroRequest();

        when(service.registrar(request))
                .thenThrow(
                        new IllegalArgumentException(
                                "El correo ya está registrado"
                        )
                );

        ResponseEntity<?> respuesta =
                controller.registrar(request);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "El correo ya está registrado",
                body.get("mensaje")
        );
    }

    @Test
    void loginCorrectamenteCreaSesion() {
        LoginRequest request = new LoginRequest();

        request.setCorreo("juan@test.com");
        request.setPassword("123456");

        LoginResponse loginResponse = new LoginResponse(
                true,
                "Inicio de sesión correcto",
                1L,
                "Juan",
                "Perez",
                "juan@test.com",
                "Cliente"
        );

        UserDetails userDetails = User
                .withUsername("juan@test.com")
                .password("encoded")
                .roles("Cliente")
                .build();

        HttpServletRequest httpRequest =
                mock(HttpServletRequest.class);

        HttpSession session = mock(HttpSession.class);

        when(service.login(request))
                .thenReturn(loginResponse);

        when(detailsService.loadUserByUsername("juan@test.com"))
                .thenReturn(userDetails);

        when(httpRequest.getSession(false))
                .thenReturn(null);

        when(httpRequest.getSession(true))
                .thenReturn(session);

        ResponseEntity<?> respuesta =
                controller.login(request, httpRequest);

        assertEquals(200, respuesta.getStatusCode().value());
        assertSame(loginResponse, respuesta.getBody());

        verify(httpRequest).getSession(true);

        verify(session).setAttribute(
                eq(
                        HttpSessionSecurityContextRepository
                                .SPRING_SECURITY_CONTEXT_KEY
                ),
                any()
        );

        assertNotNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        assertEquals(
                "juan@test.com",
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getName()
        );
    }

    @Test
    void loginConSesionExistenteCambiaId() {
        LoginRequest request = new LoginRequest();

        request.setCorreo("juan@test.com");
        request.setPassword("123456");

        LoginResponse loginResponse = new LoginResponse(
                true,
                "Correcto",
                1L,
                "Juan",
                "Perez",
                "juan@test.com",
                "Cliente"
        );

        UserDetails userDetails = User
                .withUsername("juan@test.com")
                .password("encoded")
                .roles("Cliente")
                .build();

        HttpServletRequest httpRequest =
                mock(HttpServletRequest.class);

        HttpSession session = mock(HttpSession.class);

        when(service.login(request))
                .thenReturn(loginResponse);

        when(detailsService.loadUserByUsername("juan@test.com"))
                .thenReturn(userDetails);

        when(httpRequest.getSession(false))
                .thenReturn(session);

        ResponseEntity<?> respuesta =
                controller.login(request, httpRequest);

        assertEquals(200, respuesta.getStatusCode().value());

        verify(httpRequest).changeSessionId();

        verify(session).setAttribute(
                eq(
                        HttpSessionSecurityContextRepository
                                .SPRING_SECURITY_CONTEXT_KEY
                ),
                any()
        );
    }

    @Test
    void loginConCredencialesInvalidas() {
        LoginRequest request = new LoginRequest();

        request.setCorreo("juan@test.com");
        request.setPassword("incorrecta");

        HttpServletRequest httpRequest =
                mock(HttpServletRequest.class);

        when(service.login(request))
                .thenThrow(
                        new RuntimeException(
                                "Credenciales incorrectas"
                        )
                );

        ResponseEntity<?> respuesta =
                controller.login(request, httpRequest);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "Credenciales incorrectas",
                body.get("mensaje")
        );

        verifyNoInteractions(detailsService);
    }

    @Test
    void loginCuandoFallaCargaDeUsuario() {
        LoginRequest request = new LoginRequest();

        request.setCorreo("juan@test.com");
        request.setPassword("123456");

        HttpServletRequest httpRequest =
                mock(HttpServletRequest.class);

        LoginResponse loginResponse = new LoginResponse(
                true,
                "Correcto",
                1L,
                "Juan",
                "Perez",
                "juan@test.com",
                "Cliente"
        );

        when(service.login(request))
                .thenReturn(loginResponse);

        when(detailsService.loadUserByUsername("juan@test.com"))
                .thenThrow(
                        new RuntimeException("Usuario no encontrado")
                );

        ResponseEntity<?> respuesta =
                controller.login(request, httpRequest);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "Usuario no encontrado",
                body.get("mensaje")
        );
    }

    @Test
    void listarUsuariosCorrectamente() {
        List<UsuarioAdminDTO> usuarios =
                List.of(crearDTO());

        when(service.listarUsuarios())
                .thenReturn(usuarios);

        ResponseEntity<?> respuesta =
                controller.listarUsuarios();

        assertEquals(200, respuesta.getStatusCode().value());
        assertSame(usuarios, respuesta.getBody());
        assertEquals(1, ((List<?>) respuesta.getBody()).size());

        verify(service).listarUsuarios();
    }

    @Test
    void listarUsuariosConError() {
        when(service.listarUsuarios())
                .thenThrow(
                        new RuntimeException(
                                "No se pudieron listar los usuarios"
                        )
                );

        ResponseEntity<?> respuesta =
                controller.listarUsuarios();

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "No se pudieron listar los usuarios",
                body.get("mensaje")
        );
    }

    @Test
    void obtenerUsuarioCorrectamente() {
        UsuarioAdminDTO usuario = crearDTO();

        when(service.obtenerUsuario(1L))
                .thenReturn(usuario);

        ResponseEntity<?> respuesta =
                controller.obtenerUsuario(1L);

        assertEquals(200, respuesta.getStatusCode().value());
        assertSame(usuario, respuesta.getBody());

        verify(service).obtenerUsuario(1L);
    }

    @Test
    void obtenerUsuarioConError() {
        when(service.obtenerUsuario(99L))
                .thenThrow(
                        new RuntimeException("Usuario no encontrado")
                );

        ResponseEntity<?> respuesta =
                controller.obtenerUsuario(99L);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "Usuario no encontrado",
                body.get("mensaje")
        );
    }

    @Test
    void actualizarUsuarioCorrectamente() {
        ActualizarUsuarioRequest request =
                new ActualizarUsuarioRequest();

        request.setNombres("Carlos");
        request.setApellidos("Lopez");
        request.setTelefono("987654321");

        UsuarioAdminDTO usuario = crearDTO();

        usuario.setNombres("Carlos");
        usuario.setApellidos("Lopez");
        usuario.setTelefono("987654321");

        when(service.actualizarUsuario(1L, request))
                .thenReturn(usuario);

        ResponseEntity<?> respuesta =
                controller.actualizarUsuario(1L, request);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "Usuario actualizado correctamente",
                body.get("mensaje")
        );

        assertSame(usuario, body.get("usuario"));

        verify(service).actualizarUsuario(1L, request);
    }

    @Test
    void actualizarUsuarioConError() {
        ActualizarUsuarioRequest request =
                new ActualizarUsuarioRequest();

        when(service.actualizarUsuario(1L, request))
                .thenThrow(
                        new RuntimeException("Datos inválidos")
                );

        ResponseEntity<?> respuesta =
                controller.actualizarUsuario(1L, request);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "Datos inválidos",
                body.get("mensaje")
        );
    }

    @Test
    void cambiarEstadoCorrectamente() {
        Map<String, String> request =
                Map.of("estado", "Inactivo");

        UsuarioAdminDTO usuario = crearDTO();
        usuario.setEstado("Inactivo");

        when(service.cambiarEstado(1L, "Inactivo"))
                .thenReturn(usuario);

        ResponseEntity<?> respuesta =
                controller.cambiarEstado(1L, request);

        assertEquals(200, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "Estado actualizado correctamente",
                body.get("mensaje")
        );

        assertSame(usuario, body.get("usuario"));

        verify(service).cambiarEstado(1L, "Inactivo");
    }

    @Test
    void cambiarEstadoConError() {
        Map<String, String> request =
                Map.of("estado", "Desconocido");

        when(service.cambiarEstado(1L, "Desconocido"))
                .thenThrow(
                        new RuntimeException(
                                "El estado debe ser Activo o Inactivo"
                        )
                );

        ResponseEntity<?> respuesta =
                controller.cambiarEstado(1L, request);

        assertEquals(400, respuesta.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) respuesta.getBody();

        assertEquals(
                "El estado debe ser Activo o Inactivo",
                body.get("mensaje")
        );
    }
}

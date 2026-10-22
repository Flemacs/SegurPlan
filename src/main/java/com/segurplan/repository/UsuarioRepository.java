package com.segurplan.repository;

import com.segurplan.model.Usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    // BUSCAR USUARIO POR CORREO

    Optional<Usuario> findByCorreo(
            String correo
    );

    // VALIDAR SI EL CORREO YA EXISTE

    boolean existsByCorreo(
            String correo
    );

    // BUSCAR USUARIOS POR ROL
    // Ejemplo:

    List<Usuario> findByRol_NombreOrderByIdUsuarioAsc(
            String nombreRol
    );

    // CONTAR USUARIOS POR ID DEL ROL
    // Usado en Roles y Permisos

    long countByRol_IdRol(
            Integer idRol
    );
}
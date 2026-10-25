package com.segurplan.repository;

import com.segurplan.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    // Documentos/evidencias pertenecientes a un siniestro
    List<Documento> findBySiniestro_IdSiniestroOrderByFechaCargaDesc(
            Long idSiniestro
    );

    // Documentos de un usuario
    List<Documento> findByUsuario_IdUsuarioOrderByFechaCargaDesc(
            Long idUsuario
    );

    // Documentos de un siniestro perteneciente a un usuario concreto
    List<Documento> findBySiniestro_IdSiniestroAndUsuario_IdUsuarioOrderByFechaCargaDesc(
            Long idSiniestro,
            Long idUsuario
    );
}
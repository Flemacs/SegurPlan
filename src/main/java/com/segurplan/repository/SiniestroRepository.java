package com.segurplan.repository;

import com.segurplan.model.Siniestro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiniestroRepository
        extends JpaRepository<Siniestro, Long> {

    Optional<Siniestro> findByNumeroSiniestro(
            String numeroSiniestro
    );

    List<Siniestro>
    findByUsuario_IdUsuarioOrderByFechaRegistroDesc(
            Long idUsuario
    );
}
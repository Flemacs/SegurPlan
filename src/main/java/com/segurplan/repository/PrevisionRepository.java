package com.segurplan.repository;

import com.segurplan.model.Prevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrevisionRepository
        extends JpaRepository<Prevision, Long> {

    List<Prevision> findByUsuario_IdUsuarioOrderByFechaRegistroDesc(
            Long idUsuario
    );

    List<Prevision> findByEstadoOrderByFechaRegistroDesc(
            String estado
    );

    List<Prevision> findAllByOrderByFechaRegistroDesc();
}


package com.segurplan.repository;

import com.segurplan.model.Poliza;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolizaRepository extends JpaRepository<Poliza, Long> {

    Optional<Poliza> findByContratacion_IdContratacion(
            Long idContratacion
    );

    @Query("""
        SELECT p
        FROM Poliza p
        JOIN FETCH p.contratacion c
        WHERE c.usuario.idUsuario = :idUsuario
        ORDER BY p.idPoliza DESC
        """)
    List<Poliza> findByUsuario(
            @Param("idUsuario") Long idUsuario
    );
}
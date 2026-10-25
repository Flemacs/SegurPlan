package com.segurplan.repository;

import com.segurplan.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findTop30ByUsuario_IdUsuarioOrderByFechaDesc(Long idUsuario);

    long countByUsuario_IdUsuarioAndLeidaFalse(Long idUsuario);

    @Modifying
    @Query("update Notificacion n set n.leida = true "
            + "where n.usuario.idUsuario = :id and n.leida = false")
    int marcarTodasLeidas(@Param("id") Long idUsuario);
}

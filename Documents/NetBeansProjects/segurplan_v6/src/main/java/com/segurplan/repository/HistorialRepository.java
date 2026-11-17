package com.segurplan.repository;

import com.segurplan.model.Historial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistorialRepository
        extends JpaRepository<Historial, Long> {

    // TODO EL HISTORIAL

    List<Historial>
    findAllByOrderByFechaHoraDesc();


    // POR RANGO DE FECHAS

    List<Historial>
    findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(
            LocalDateTime inicio,
            LocalDateTime fin
    );


    // POR ENTIDAD

    List<Historial>
    findByEntidadOrderByFechaHoraDesc(
            String entidad
    );


    // POR USUARIO

    List<Historial>
    findByUsuario_IdUsuarioOrderByFechaHoraDesc(
            Long idUsuario
    );
}
package com.segurplan.repository;

import com.segurplan.model.ComunicacionAseguradora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComunicacionAseguradoraRepository
        extends JpaRepository<ComunicacionAseguradora, Long> {

    List<ComunicacionAseguradora> findByCotizacion_IdCotizacionOrderByFechaDesc(
            Long idCotizacion);
}

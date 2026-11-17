package com.segurplan.repository;

import com.segurplan.model.Aseguradora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AseguradoraRepository
        extends JpaRepository<Aseguradora, Integer> {

    // Para los formularios públicos: solo las aseguradoras activas
    java.util.List<Aseguradora> findByEstadoOrderByNombreAsc(String estado);

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdAseguradoraNot(String ruc, Integer idAseguradora);
}

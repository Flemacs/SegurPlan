package com.segurplan.repository;

import com.segurplan.model.TipoSeguro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoSeguroRepository
        extends JpaRepository<TipoSeguro, Integer> {

    // Para los formularios públicos: solo los tipos de seguro activos
    java.util.List<TipoSeguro> findByEstadoOrderByNombreAsc(String estado);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdTipoSeguroNot(String nombre, Integer idTipoSeguro);
}

package com.segurplan.repository;

import com.segurplan.model.TipoSeguro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoSeguroRepository
        extends JpaRepository<TipoSeguro, Integer> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdTipoSeguroNot(String nombre, Integer idTipoSeguro);
}

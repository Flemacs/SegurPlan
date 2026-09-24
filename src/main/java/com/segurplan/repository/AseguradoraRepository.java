package com.segurplan.repository;

import com.segurplan.model.Aseguradora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AseguradoraRepository
        extends JpaRepository<Aseguradora, Integer> {
}
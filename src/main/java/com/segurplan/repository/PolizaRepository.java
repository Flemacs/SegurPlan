/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.segurplan.repository;

import com.segurplan.model.Poliza;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PolizaRepository
        extends JpaRepository<Poliza, Long> {

    Optional<Poliza>
        findByContratacion_IdContratacion(Long idContratacion);
}
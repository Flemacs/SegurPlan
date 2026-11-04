/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.segurplan.repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;
import com.segurplan.model.Contratacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContratacionRepository
        extends JpaRepository<Contratacion, Long> {

    Optional<Contratacion>
        findByCotizacion_IdCotizacion(Long idCotizacion);
    @Query("""
    SELECT c FROM Contratacion c
    WHERE c.condicionesAceptadas = true
    AND c.cotizacion.estado = 'Aceptada'
    AND NOT EXISTS (
        SELECT p FROM Poliza p
        WHERE p.contratacion = c
    )
    ORDER BY c.fechaContratacion DESC
""")
List<Contratacion> findPendientesDePoliza();

}
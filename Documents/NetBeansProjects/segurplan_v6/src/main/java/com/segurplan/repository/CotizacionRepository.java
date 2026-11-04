/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.segurplan.repository;

import com.segurplan.model.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CotizacionRepository
        extends JpaRepository<Cotizacion, Long> {

    List<Cotizacion> findByUsuario_IdUsuario(Long idUsuario);
}
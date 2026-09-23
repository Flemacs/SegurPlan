/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.segurplan.repository;
import com.segurplan.model.TipoSeguro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoSeguroRepository
        extends JpaRepository<TipoSeguro, Integer> {
}
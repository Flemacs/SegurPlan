package com.segurplan.controller;

import com.segurplan.model.Aseguradora;
import com.segurplan.model.TipoSeguro;
import com.segurplan.repository.AseguradoraRepository;
import com.segurplan.repository.TipoSeguroRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Catálogos", description = "Tipos de seguro y aseguradoras activos, para los formularios.")
@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private final TipoSeguroRepository tipoSeguroRepository;
    private final AseguradoraRepository aseguradoraRepository;

    public CatalogoController(
            TipoSeguroRepository tipoSeguroRepository,
            AseguradoraRepository aseguradoraRepository) {

        this.tipoSeguroRepository = tipoSeguroRepository;
        this.aseguradoraRepository = aseguradoraRepository;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Listar tipos de seguro activos", description = "**Roles:** Cualquier usuario autenticado. Solo devuelve los tipos en estado Activo.")
    @GetMapping("/tipos-seguro")
    public ResponseEntity<List<TipoSeguro>> listarTiposSeguro() {
        // Solo los activos: lo que se desactiva en "Mantenimiento de catálogos"
        // deja de ofrecerse en los formularios.
        return ResponseEntity.ok(
                tipoSeguroRepository.findByEstadoOrderByNombreAsc("Activo"));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Listar aseguradoras activas", description = "**Roles:** Cualquier usuario autenticado. Solo devuelve las aseguradoras en estado Activo.")
    @GetMapping("/aseguradoras")
    public ResponseEntity<List<Aseguradora>> listarAseguradoras() {
        return ResponseEntity.ok(
                aseguradoraRepository.findByEstadoOrderByNombreAsc("Activo"));
    }
}
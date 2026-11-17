package com.segurplan.controller;

import com.segurplan.model.Aseguradora;
import com.segurplan.model.TipoSeguro;
import com.segurplan.repository.AseguradoraRepository;
import com.segurplan.repository.TipoSeguroRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/tipos-seguro")
    public ResponseEntity<List<TipoSeguro>> listarTiposSeguro() {
        return ResponseEntity.ok(tipoSeguroRepository.findAll());
    }

    @GetMapping("/aseguradoras")
    public ResponseEntity<List<Aseguradora>> listarAseguradoras() {
        return ResponseEntity.ok(aseguradoraRepository.findAll());
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.segurplan.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Autenticación", description = "Registro, inicio de sesión y rutas heredadas de administración de usuarios.")
@RestController
public class CsrfController {

    @io.swagger.v3.oas.annotations.Operation(summary = "Obtener el token CSRF", description = "**Roles:** Público (sin sesión). Devuelve headerName, parameterName y token. Envíe el token en la cabecera X-CSRF-TOKEN en POST, PUT y DELETE.")
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @GetMapping("/api/csrf")
    public CsrfToken obtenerToken(CsrfToken csrfToken) {
        return csrfToken;
    }
}
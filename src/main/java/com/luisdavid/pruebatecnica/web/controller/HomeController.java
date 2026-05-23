package com.luisdavid.pruebatecnica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirige la raíz "/" a Swagger UI.
 *
 * <p>Sin esto, una visita a la URL base devolvía 500 porque no había handler
 * registrado para "/" y el handler global lo trataba como error inesperado.
 * Para un evaluador que abre la URL pública, lo más útil es aterrizar en
 * la documentación interactiva.</p>
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/swagger-ui/index.html";
    }
}

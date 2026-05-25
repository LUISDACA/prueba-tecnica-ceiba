package com.luisdavid.pruebatecnica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirige la raiz "/" a Swagger UI.
 *
 * <p>Sin esto, una visita a la URL base devolvia 500 porque no habia handler
 * registrado para "/" y el handler global lo trataba como error inesperado.
 * Para un evaluador que abre la URL publica, lo mas util es aterrizar en
 * la documentacion interactiva.</p>
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/swagger-ui/index.html";
    }
}

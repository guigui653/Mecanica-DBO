package com.mecanicadbo.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * No PC da oficina (perfil prod) o backend também entrega as telas do Angular,
 * que ficam na pasta do APP_SITE. Ao abrir ou recarregar (F5) um endereço do
 * Angular, como /clientes ou /ordens/12, devolve o index.html e o Angular
 * mostra a tela certa. A raiz "/" o Spring já resolve sozinho.
 */
@Controller
@Profile("prod")
public class TelasController {

    @GetMapping({"/login", "/dashboard", "/clientes", "/veiculos", "/ordens", "/ordens/{*resto}"})
    public String tela() {
        return "forward:/index.html";
    }
}

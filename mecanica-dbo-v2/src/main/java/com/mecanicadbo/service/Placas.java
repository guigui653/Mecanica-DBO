package com.mecanicadbo.service;

import java.util.Locale;

/**
 * Regra única de placa.
 *
 * No banco a placa fica só com letras e números, em maiúsculas (JEH3030,
 * ABC1D23) — o CHECK veiculo_placa_formato_check garante isso. Assim
 * "jeh-3030", "JEH 3030" e "JEH3030" são o mesmo carro.
 *
 * O hífen do padrão antigo é só exibição (formatar), para o recibo continuar
 * igual ao que o cliente conhece.
 */
public final class Placas {

    private Placas() {}

    /** " jeh-3030 " → "JEH3030". null continua null. */
    public static String normalizar(String placa) {
        if (placa == null) return null;
        return placa.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Termo digitado na busca, pronto para comparar com a placa gravada: "jeh-30" → "JEH30".
     * Se não sobrar nada (ex.: "-"), devolve o termo original — um LIKE '%%' acharia tudo.
     */
    public static String paraBusca(String termo) {
        String p = normalizar(termo);
        return (p == null || p.isEmpty()) ? termo : p;
    }

    /** Padrão antigo (AAA9999) ganha hífen: "JEH3030" → "JEH-3030". Mercosul e outros ficam como estão. */
    public static String formatar(String placa) {
        String p = normalizar(placa);
        if (p != null && p.matches("[A-Z]{3}[0-9]{4}")) {
            return p.substring(0, 3) + "-" + p.substring(3);
        }
        return placa;
    }
}

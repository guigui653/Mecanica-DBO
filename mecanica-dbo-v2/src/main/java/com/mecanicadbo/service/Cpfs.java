package com.mecanicadbo.service;

/**
 * Regra única de formato do CPF (a criptografia fica no CpfCripto).
 *
 * O CPF é tratado sempre só com os 11 números: "123.456.789-00" e
 * "12345678900" são o mesmo CPF. Para mostrar, ele sai mascarado
 * ("***.456.789-**"), como nos sites do governo.
 */
public final class Cpfs {

    private Cpfs() {}

    /** " 123.456.789-00 " → "12345678900". null continua null. */
    public static String soDigitos(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    /** "12345678900" → "***.456.789-**". Formato fora do padrão vira "***"; vazio vira null. */
    public static String mascarar(String cpf) {
        String d = soDigitos(cpf);
        if (d == null || d.isEmpty()) return null;
        if (d.length() != 11) return "***";
        return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
    }
}

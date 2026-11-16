package com.mecanicadbo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Troca da chave do CPF: tranca de novo, com a CPF_CHAVE atual, os CPFs que
 * foram gravados com a CPF_CHAVE_ANTIGA.
 *
 * Roda ao subir o sistema, logo depois do Flyway e antes de qualquer tela
 * carregar um cliente (ver FlywayConfig). Só faz algo se CPF_CHAVE_ANTIGA
 * estiver definida. Tudo numa transação: se algum CPF não abrir com nenhuma
 * das duas chaves, nada é alterado e o sistema não sobe.
 *
 * Pode rodar mais de uma vez sem problema: CPF que já abre com a chave atual
 * fica como está. Depois da troca, apague a CPF_CHAVE_ANTIGA.
 */
@Component
public class TrocaChaveCpf {

    private static final Logger log = LoggerFactory.getLogger(TrocaChaveCpf.class);

    private final CpfCripto atual;
    private final String chaveAntiga;
    private final DataSource dataSource;

    public TrocaChaveCpf(CpfCripto atual,
                         @Value("${app.cpf.chave-antiga:}") String chaveAntiga,
                         DataSource dataSource) {
        this.atual = atual;
        this.chaveAntiga = chaveAntiga;
        this.dataSource = dataSource;
    }

    public void executarSePedido() {
        if (chaveAntiga == null || chaveAntiga.isBlank()) return;

        CpfCripto antiga = CpfCripto.daChaveAntiga(chaveAntiga);
        if (antiga.mesmaChaveQue(atual)) {
            log.warn("CPF_CHAVE_ANTIGA é igual à CPF_CHAVE: nada a trocar. Apague a CPF_CHAVE_ANTIGA.");
            return;
        }

        try (Connection con = dataSource.getConnection()) {
            boolean autoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                Resultado r = trocar(con, antiga);
                con.commit();
                log.warn("Troca da chave do CPF concluída: {} CPF(s) trancado(s) com a chave nova, {} já estava(m) com ela. "
                       + "Agora APAGUE a variável CPF_CHAVE_ANTIGA e guarde só a CPF_CHAVE nova.", r.trocados, r.jaNaChaveNova);
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha na troca da chave do CPF. Nada foi alterado.", e);
        }
    }

    private Resultado trocar(Connection con, CpfCripto antiga) throws SQLException {
        Map<Long, String> cifrados = new LinkedHashMap<>();
        try (PreparedStatement st = con.prepareStatement(
                "SELECT id, cpf_cifrado FROM cliente WHERE cpf_cifrado IS NOT NULL ORDER BY id FOR UPDATE");
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) cifrados.put(rs.getLong("id"), rs.getString("cpf_cifrado"));
        }

        Resultado r = new Resultado();
        try (PreparedStatement up = con.prepareStatement(
                "UPDATE cliente SET cpf_cifrado = ?, cpf_hash = ? WHERE id = ?")) {
            for (Map.Entry<Long, String> e : cifrados.entrySet()) {
                String cpf = abrir(e.getKey(), e.getValue(), antiga, r);
                if (cpf == null) continue;
                up.setString(1, atual.cifrar(cpf));
                up.setString(2, atual.hash(cpf));
                up.setLong(3, e.getKey());
                up.addBatch();
            }
            up.executeBatch();
        }
        return r;
    }

    /** CPF em números se precisa trocar; null se já está na chave atual. */
    String abrir(Long id, String cifrado, CpfCripto antiga, Resultado r) {
        try {
            atual.decifrar(cifrado);
            r.jaNaChaveNova++;
            return null;
        } catch (IllegalStateException naoEDaAtual) {
            try {
                String cpf = antiga.decifrar(cifrado);
                r.trocados++;
                return cpf;
            } catch (IllegalStateException naoEDaAntiga) {
                throw new IllegalStateException("Troca da chave do CPF cancelada: o CPF do cliente id " + id
                        + " não abre nem com a CPF_CHAVE nem com a CPF_CHAVE_ANTIGA. Confira as duas chaves. Nada foi alterado.");
            }
        }
    }

    static class Resultado {
        int trocados;
        int jaNaChaveNova;
    }
}

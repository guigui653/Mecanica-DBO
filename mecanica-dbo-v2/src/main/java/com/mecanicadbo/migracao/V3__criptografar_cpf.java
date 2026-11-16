package com.mecanicadbo.migracao;

import com.mecanicadbo.service.CpfCripto;
import com.mecanicadbo.service.Cpfs;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V3 — CPF criptografado no banco.
 *
 * Em Java (e não em .sql) porque só a aplicação tem a chave (CPF_CHAVE).
 * O Spring Boot entrega este bean ao Flyway, que roda tudo numa transação:
 * se algo falhar, nada muda.
 *
 *  1. Cria cliente.cpf_cifrado (texto cifrado) e cliente.cpf_hash (busca/unicidade).
 *  2. Converte cada CPF existente para só números; se dois clientes ficarem
 *     com o mesmo CPF, PARA sem mudar nada. CPF sem nenhum número vira vazio.
 *  3. Grava cifrado + hash, cria o índice único no hash e APAGA a coluna cpf
 *     em claro.
 *
 * Backups feitos antes desta versão ainda têm o CPF legível.
 */
@Component
public class V3__criptografar_cpf extends BaseJavaMigration {

    private final CpfCripto cripto;

    public V3__criptografar_cpf(CpfCripto cripto) {
        this.cripto = cripto;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection con = context.getConnection();

        try (Statement st = con.createStatement()) {
            st.execute("ALTER TABLE cliente ADD COLUMN cpf_cifrado text, ADD COLUMN cpf_hash varchar(64)");
        }

        // Lê os CPFs atuais e confere colisões antes de gravar qualquer coisa.
        Map<Long, String> digitosPorId = new HashMap<>();
        Map<String, List<String>> clientesPorCpf = new HashMap<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nome, cpf FROM cliente WHERE cpf IS NOT NULL")) {
            while (rs.next()) {
                String digitos = Cpfs.soDigitos(rs.getString("cpf"));
                if (digitos == null || digitos.isEmpty()) continue;   // ex.: "***.***.***-**"
                long id = rs.getLong("id");
                digitosPorId.put(id, digitos);
                clientesPorCpf.computeIfAbsent(digitos, k -> new ArrayList<>())
                        .add("id " + id + " (" + rs.getString("nome") + ")");
            }
        }
        List<String> colisoes = new ArrayList<>();
        clientesPorCpf.forEach((cpf, clientes) -> {
            if (clientes.size() > 1) colisoes.add(Cpfs.mascarar(cpf) + ": " + String.join(", ", clientes));
        });
        if (!colisoes.isEmpty()) {
            throw new IllegalStateException(
                    "Criptografia do CPF cancelada: estes clientes têm o mesmo CPF — " + String.join("; ", colisoes)
                  + ". Corrija ou junte esses cadastros e suba o sistema de novo.");
        }

        try (PreparedStatement up = con.prepareStatement(
                "UPDATE cliente SET cpf_cifrado = ?, cpf_hash = ? WHERE id = ?")) {
            for (Map.Entry<Long, String> e : digitosPorId.entrySet()) {
                up.setString(1, cripto.cifrar(e.getValue()));
                up.setString(2, cripto.hash(e.getValue()));
                up.setLong(3, e.getKey());
                up.addBatch();
            }
            up.executeBatch();
        }

        try (Statement st = con.createStatement()) {
            st.execute("ALTER TABLE cliente ADD CONSTRAINT cliente_cpf_hash_key UNIQUE (cpf_hash)");
            st.execute("ALTER TABLE cliente DROP COLUMN cpf");   // leva junto o índice único antigo
            st.execute("COMMENT ON COLUMN cliente.cpf_cifrado IS 'CPF criptografado (AES-256-GCM, chave CPF_CHAVE). Só a aplicação lê.'");
            st.execute("COMMENT ON COLUMN cliente.cpf_hash IS 'HMAC-SHA256 do CPF: índice único e busca pelo CPF completo.'");
        }
    }
}

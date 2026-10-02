package com.aclg.apecan.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;
import java.util.function.Function;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Atualizacao atomica com bloqueio de linha; transacao independente da regra de negocio. */
@Component
public class ControleAcessoStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final String segredo;
    private final Clock clock;
    private final boolean h2;

    public ControleAcessoStore(JdbcTemplate jdbc, PlatformTransactionManager manager, Clock clock,
            @Value("${apecan.acesso.chave-hmac:apecan-desenvolvimento-nao-usar-em-producao}") String segredo) {
        this.jdbc = jdbc; this.clock = clock; this.segredo = segredo;
        this.tx = new TransactionTemplate(manager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.h2 = Boolean.TRUE.equals(jdbc.execute((java.sql.Connection con) ->
            con.getMetaData().getDatabaseProductName().equals("H2")));
    }
    public <T> T alterar(String finalidade, String identificador, Function<Estado, T> operacao) {
        String chave = chave(finalidade, identificador);
        for (int tentativa = 0; ; tentativa++) {
        try {
        return tx.execute(status -> {
            long agora = clock.millis();
            jdbc.update("DELETE FROM controles_acesso WHERE expira_em < ?", agora);
            if (h2) {
                jdbc.update("""
                    MERGE INTO controles_acesso t USING (VALUES (CAST(? AS VARCHAR(80)))) s(chave)
                    ON t.chave=s.chave WHEN NOT MATCHED THEN
                    INSERT (chave,falhas,inicio_janela,bloqueado_ate,ultimo_evento,expira_em)
                    VALUES (s.chave,0,0,0,0,?)
                    """, chave, agora + 3600000);
            } else {
                jdbc.update("""
                    INSERT INTO controles_acesso (chave,falhas,inicio_janela,bloqueado_ate,ultimo_evento,expira_em)
                    VALUES (?,0,0,0,0,?) ON CONFLICT (chave) DO NOTHING
                    """, chave, agora + 3600000);
            }
            Estado estado = jdbc.queryForObject("""
                SELECT falhas,inicio_janela,bloqueado_ate,ultimo_evento,expira_em
                FROM controles_acesso WHERE chave=? FOR UPDATE
                """, (rs, i) -> new Estado(rs.getInt(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)), chave);
            T resultado = operacao.apply(estado);
            jdbc.update("""
                UPDATE controles_acesso SET falhas=?,inicio_janela=?,bloqueado_ate=?,ultimo_evento=?,expira_em=?
                WHERE chave=?
                """, estado.falhas, estado.inicio, estado.bloqueadoAte, estado.ultimo, estado.expira, chave);
            return resultado;
        });
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            // H2 MERGE pode disputar a primeira insercao. Repetir em NOVA transacao.
            // PostgreSQL usa ON CONFLICT e nao precisa desse caminho.
            if (!h2 || tentativa >= 2) throw exception;
        }
        }
    }
    private String chave(String finalidade, String identificador) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String normalizado = identificador == null ? "" : identificador.trim().toLowerCase(Locale.ROOT);
            return finalidade + ":" + HexFormat.of().formatHex(
                    mac.doFinal((finalidade + ":" + normalizado).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Controle de acesso indisponivel.");
        }
    }
    public static final class Estado {
        int falhas;
        long inicio, bloqueadoAte, ultimo, expira;
        Estado(int falhas, long inicio, long bloqueadoAte, long ultimo, long expira) {
            this.falhas= falhas; this.inicio=inicio; this.bloqueadoAte=bloqueadoAte;
            this.ultimo=ultimo; this.expira=expira;
        }
    }
}

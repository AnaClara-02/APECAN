package com.aclg.apecan.administracao.teste.service;

import com.aclg.apecan.administracao.teste.dto.ExclusaoTestePreview;
import com.aclg.apecan.administracao.teste.entity.AuditoriaExclusaoTeste;
import com.aclg.apecan.administracao.teste.entity.TipoRegistroExclusaoTeste;
import com.aclg.apecan.administracao.teste.repository.AuditoriaExclusaoTesteRepository;
import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.equipamento.entity.StatusEquipamento;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.shared.exception.RecursoNaoEncontradoException;
import com.aclg.apecan.usuario.entity.StatusUsuario;
import com.aclg.apecan.usuario.entity.TipoPerfil;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
public class ExclusaoTesteService {

    private static final String JUSTIFICATIVA_AUDITORIA = "Exclusão confirmada pela interface administrativa.";

    private final JdbcTemplate jdbc;
    private final UsuarioRepository usuarios;
    private final AuditoriaExclusaoTesteRepository auditoria;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public ExclusaoTesteService(JdbcTemplate jdbc, UsuarioRepository usuarios,
            AuditoriaExclusaoTesteRepository auditoria, UsuarioAtual usuarioAtual, Clock clock) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ExclusaoTestePreview analisar(TipoRegistroExclusaoTeste tipo, Long id) {
        exigirAdmDev(false);
        return analisarDados(tipo, id);
    }

    @Transactional
    public void excluir(TipoRegistroExclusaoTeste tipo, Long id) {
        Usuario responsavel = exigirAdmDev(true);
        try {
            ExclusaoTestePreview previa;
            int removidos;
            switch (tipo) {
                case PACIENTE -> {
                    bloquear("pacientes", "id_paciente", id);
                    previa = analisarDados(tipo, id);
                    exigirPermitida(previa);
                    removidos = jdbc.update("DELETE FROM historico_status_paciente WHERE id_paciente = ?", id);
                    removidos += jdbc.update("DELETE FROM pacientes WHERE id_paciente = ?", id);
                }
                case VOLUNTARIO -> {
                    bloquear("voluntarios", "id_voluntario", id);
                    previa = analisarDados(tipo, id);
                    exigirPermitida(previa);
                    removidos = jdbc.update("DELETE FROM voluntarios WHERE id_voluntario = ?", id);
                }
                case EQUIPAMENTO -> {
                    bloquear("equipamentos", "id_equipamento", id);
                    previa = analisarDados(tipo, id);
                    exigirPermitida(previa);
                    removidos = jdbc.update("DELETE FROM equipamentos WHERE id_equipamento = ?", id);
                }
                case CATEGORIA -> {
                    bloquear("categorias_equipamentos", "id_categoria", id);
                    previa = analisarDados(tipo, id);
                    exigirPermitida(previa);
                    removidos = jdbc.update("DELETE FROM categorias_equipamentos WHERE id_categoria = ?", id);
                }
                case EMPRESTIMO -> {
                    Long equipamentoId = ((Number) obter(
                        "SELECT id_equipamento FROM emprestimos_equipamentos WHERE id_emprestimo = ?",
                        id, "EMPRESTIMO_NAO_ENCONTRADO").get("id_equipamento")).longValue();
                    bloquear("equipamentos", "id_equipamento", equipamentoId);
                    bloquear("emprestimos_equipamentos", "id_emprestimo", id);
                    previa = analisarDados(tipo, id);
                    exigirPermitida(previa);
                    if (jdbc.queryForObject("SELECT data_devolucao IS NULL FROM emprestimos_equipamentos WHERE id_emprestimo = ?",
                            Boolean.class, id)) {
                        int atualizado = jdbc.update("""
                            UPDATE equipamentos SET status = ?, atualizado_por_usuario_id = ?,
                                atualizado_em = ?, versao = versao + 1
                            WHERE id_equipamento = ? AND status = ?
                            """, StatusEquipamento.ATIVO.name(), responsavel.getId(),
                            LocalDateTime.now(clock), equipamentoId, StatusEquipamento.EMPRESTADO.name());
                        if (atualizado != 1) throw dependenciaConcorrente();
                    }
                    removidos = jdbc.update("DELETE FROM emprestimos_equipamentos WHERE id_emprestimo = ?", id);
                }
                case DOACAO -> {
                    bloquear("doacoes", "id_doacao", id);
                    previa = analisarDados(tipo, id);
                    exigirPermitida(previa);
                    String tipoDoacao = jdbc.queryForObject(
                        "SELECT tipo_doacao FROM doacoes WHERE id_doacao = ?", String.class, id);
                    removidos = jdbc.update("DELETE FROM doacao_voluntarios WHERE id_doacao = ?", id);
                    if ("MONETARIA".equals(tipoDoacao)) {
                        int movimentacoes = jdbc.update("DELETE FROM movimentacoes_financeiras WHERE id_doacao = ?", id);
                        if (movimentacoes != 1) throw dependenciaConcorrente();
                        removidos += movimentacoes;
                    }
                    else if (contar("SELECT count(*) FROM movimentacoes_financeiras WHERE id_doacao = ?", id) > 0) {
                        throw new OperacaoInvalidaException("DOACAO_NAO_MONETARIA_COM_MOVIMENTACAO",
                            "A doação não monetária possui uma movimentação financeira vinculada. Revise o vínculo antes da exclusão.");
                    }
                    removidos += jdbc.update("DELETE FROM doacoes WHERE id_doacao = ?", id);
                }
                case DESPESA -> {
                    bloquear("despesas", "id_despesa", id);
                    previa = analisarDados(tipo, id);
                    exigirPermitida(previa);
                    Long movimentoId = jdbc.queryForObject(
                        "SELECT id_movimentacao_financeira FROM despesas WHERE id_despesa = ?", Long.class, id);
                    removidos = jdbc.update("DELETE FROM despesas WHERE id_despesa = ?", id);
                    removidos += jdbc.update("DELETE FROM movimentacoes_financeiras WHERE id_movimentacao = ?", movimentoId);
                }
                case MOVIMENTACAO -> {
                    bloquear("movimentacoes_financeiras", "id_movimentacao", id);
                    previa = analisarDados(tipo, id);
                    exigirPermitida(previa);
                    removidos = jdbc.update("DELETE FROM movimentacoes_financeiras WHERE id_movimentacao = ?", id);
                }
                default -> throw new OperacaoInvalidaException("TIPO_EXCLUSAO_INVALIDO", "Tipo de registro inválido.");
            }
            if (removidos < 1) throw dependenciaConcorrente();
            auditoria.saveAndFlush(new AuditoriaExclusaoTeste(responsavel, tipo.name(), id,
                JUSTIFICATIVA_AUDITORIA, removidos, LocalDateTime.now(clock)));
        }
        catch (DataIntegrityViolationException exception) {
            throw dependenciaConcorrente();
        }
    }

    private ExclusaoTestePreview analisarDados(TipoRegistroExclusaoTeste tipo, Long id) {
        return switch (tipo) {
            case PACIENTE -> {
                Map<String, Object> row = obter("SELECT nome, status FROM pacientes WHERE id_paciente = ?", id, "PACIENTE_NAO_ENCONTRADO");
                long emprestimos = contar("SELECT count(*) FROM emprestimos_equipamentos WHERE id_paciente = ?", id);
                yield new ExclusaoTestePreview(tipo, id, texto(row, "nome"), "Status atual: " + texto(row, "status"),
                    emprestimos == 0, emprestimos == 0 ? null : "Existem empréstimos vinculados ao paciente.",
                    "Serão removidos o paciente e " + contar("SELECT count(*) FROM historico_status_paciente WHERE id_paciente = ?", id)
                        + " registro(s) do histórico de status.");
            }
            case VOLUNTARIO -> {
                Map<String, Object> row = obter("SELECT nome, status FROM voluntarios WHERE id_voluntario = ?", id, "VOLUNTARIO_NAO_ENCONTRADO");
                long doacoes = contar("SELECT count(*) FROM doacao_voluntarios WHERE id_voluntario = ?", id);
                yield new ExclusaoTestePreview(tipo, id, texto(row, "nome"), "Status atual: " + texto(row, "status"),
                    doacoes == 0, doacoes == 0 ? null : "O voluntário participa de doação(ões); essas relações precisam ser preservadas.",
                    "Será removido somente o cadastro do voluntário.");
            }
            case EQUIPAMENTO -> {
                Map<String, Object> row = obter("""
                    SELECT e.status, e.id_doacao, c.nome AS categoria FROM equipamentos e
                    JOIN categorias_equipamentos c ON c.id_categoria = e.id_categoria WHERE e.id_equipamento = ?
                    """, id, "EQUIPAMENTO_NAO_ENCONTRADO");
                long emprestimos = contar("SELECT count(*) FROM emprestimos_equipamentos WHERE id_equipamento = ?", id);
                boolean doado = row.get("id_doacao") != null;
                yield new ExclusaoTestePreview(tipo, id, "Equipamento #" + id,
                    "Categoria: " + texto(row, "categoria") + " · Situação: " + texto(row, "status"),
                    emprestimos == 0, emprestimos == 0 ? null : "Há histórico de empréstimos vinculado à unidade.",
                    doado ? "Será removida a unidade; a doação original continuará registrada." : "Será removida somente a unidade do equipamento.");
            }
            case CATEGORIA -> {
                Map<String, Object> row = obter("SELECT nome FROM categorias_equipamentos WHERE id_categoria = ?", id, "CATEGORIA_NAO_ENCONTRADA");
                long unidades = contar("SELECT count(*) FROM equipamentos WHERE id_categoria = ?", id);
                yield new ExclusaoTestePreview(tipo, id, texto(row, "nome"), "Categoria de equipamentos",
                    unidades == 0, unidades == 0 ? null : "A categoria possui equipamentos cadastrados.",
                    "Será removida somente a categoria vazia.");
            }
            case EMPRESTIMO -> {
                Map<String, Object> row = obter("""
                    SELECT e.id_equipamento, e.data_emprestimo, e.data_devolucao, p.nome AS paciente
                    FROM emprestimos_equipamentos e JOIN pacientes p ON p.id_paciente = e.id_paciente
                    WHERE e.id_emprestimo = ?
                    """, id, "EMPRESTIMO_NAO_ENCONTRADO");
                boolean aberto = row.get("data_devolucao") == null;
                yield new ExclusaoTestePreview(tipo, id, "Empréstimo #" + id,
                    "Paciente: " + texto(row, "paciente") + " · Data: " + texto(row, "data_emprestimo"),
                    true, null, aberto ? "O registro será removido e o equipamento #" + texto(row, "id_equipamento")
                        + " voltará a ATIVO; nenhuma devolução será inventada."
                        : "Será removido somente o registro do empréstimo concluído.");
            }
            case DOACAO -> {
                Map<String, Object> row = obter("SELECT tipo_doacao, fonte_doacao, data_doacao FROM doacoes WHERE id_doacao = ?", id, "DOACAO_NAO_ENCONTRADA");
                String tipoDoacao = texto(row, "tipo_doacao");
                long unidades = contar("SELECT count(*) FROM equipamentos WHERE id_doacao = ?", id);
                String detalhes = "Tipo: " + tipoDoacao + " · Data: " + texto(row, "data_doacao")
                    + " · Origem: " + texto(row, "fonte_doacao");
                String impacto = "A doação e " + contar("SELECT count(*) FROM doacao_voluntarios WHERE id_doacao = ?", id)
                    + " vínculo(s) de voluntário serão removidos.";
                if ("MONETARIA".equals(tipoDoacao)) {
                    Map<String, Object> movimentacao = obter(
                        "SELECT valor FROM movimentacoes_financeiras WHERE id_doacao = ?", id,
                        "MOVIMENTACAO_DA_DOACAO_NAO_ENCONTRADA");
                    impacto += " A entrada de " + dinheiro((BigDecimal) movimentacao.get("valor"))
                        + " também será removida do saldo e dos relatórios.";
                }
                if ("EQUIPAMENTO".equals(tipoDoacao) && unidades > 0)
                    impacto = "Há " + unidades + " unidade(s) de equipamento vinculada(s); exclua essas unidades antes.";
                yield new ExclusaoTestePreview(tipo, id, "Doação #" + id, detalhes,
                    !("EQUIPAMENTO".equals(tipoDoacao) && unidades > 0),
                    "EQUIPAMENTO".equals(tipoDoacao) && unidades > 0 ? impacto : null,
                    impacto);
            }
            case DESPESA -> {
                Map<String, Object> row = obter("""
                    SELECT d.descricao, d.tipo_despesa, m.valor, m.data_movimentacao
                    FROM despesas d JOIN movimentacoes_financeiras m ON m.id_movimentacao = d.id_movimentacao_financeira
                    WHERE d.id_despesa = ?
                    """, id, "DESPESA_NAO_ENCONTRADA");
                yield new ExclusaoTestePreview(tipo, id, "Despesa #" + id,
                    texto(row, "tipo_despesa") + " · " + texto(row, "descricao") + " · " + texto(row, "data_movimentacao"),
                    true, null, "A despesa e sua saída de " + dinheiro((BigDecimal) row.get("valor"))
                        + " serão removidas do saldo e dos relatórios.");
            }
            case MOVIMENTACAO -> {
                Map<String, Object> row = obter("""
                    SELECT m.tipo_movimentacao, m.valor, m.data_movimentacao, m.origem_descricao,
                           m.id_doacao, d.id_despesa
                    FROM movimentacoes_financeiras m LEFT JOIN despesas d
                      ON d.id_movimentacao_financeira = m.id_movimentacao
                    WHERE m.id_movimentacao = ?
                    """, id, "MOVIMENTACAO_NAO_ENCONTRADA");
                boolean vinculada = row.get("id_doacao") != null || row.get("id_despesa") != null;
                String motivo = row.get("id_doacao") != null ? "Movimentação vinculada a uma doação; exclua a doação pela tela de doações."
                    : row.get("id_despesa") != null ? "Movimentação vinculada a uma despesa; exclua a despesa pela tela de despesas."
                    : null;
                yield new ExclusaoTestePreview(tipo, id, "Movimentação #" + id,
                    texto(row, "tipo_movimentacao") + " · " + dinheiro((BigDecimal) row.get("valor"))
                        + " · " + texto(row, "data_movimentacao") + " · " + texto(row, "origem_descricao"),
                    !vinculada, motivo, "A movimentação será removida do saldo e dos relatórios financeiros.");
            }
        };
    }

    private Usuario exigirAdmDev(boolean bloquear) {
        Long id = usuarioAtual.exigirId();
        Usuario usuario = (bloquear ? usuarios.findByIdForUpdate(id) : usuarios.findById(id))
            .orElseThrow(() -> new OperacaoInvalidaException("ADM_DEV_NAO_AUTORIZADO", "A operação não está autorizada."));
        if (usuario.getTipoPerfil() != TipoPerfil.ADM_DEV || usuario.getStatus() != StatusUsuario.ATIVO || !usuario.estaAtivado())
            throw new OperacaoInvalidaException("ADM_DEV_NAO_AUTORIZADO", "Somente Adm. Dev. ativo pode excluir registros.");
        return usuario;
    }

    private void bloquear(String tabela, String colunaId, Long id) {
        try {
            jdbc.queryForObject("SELECT 1 FROM " + tabela + " WHERE " + colunaId + " = ? FOR UPDATE", Integer.class, id);
        }
        catch (EmptyResultDataAccessException exception) {
            throw new RecursoNaoEncontradoException("REGISTRO_NAO_ENCONTRADO", "O registro não foi encontrado.");
        }
    }

    private Map<String, Object> obter(String sql, Long id, String codigo) {
        try {
            return jdbc.queryForMap(sql, id);
        }
        catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            throw new RecursoNaoEncontradoException(codigo, "O registro não foi encontrado.");
        }
    }

    private long contar(String sql, Long id) {
        Long total = jdbc.queryForObject(sql, Long.class, id);
        return total == null ? 0 : total;
    }

    private String texto(Map<String, Object> row, String coluna) {
        return Objects.toString(row.get(coluna), "");
    }

    private String dinheiro(BigDecimal valor) {
        return "R$ " + valor.setScale(2).toPlainString().replace('.', ',');
    }

    private void exigirPermitida(ExclusaoTestePreview previa) {
        if (!previa.podeExcluir())
            throw new OperacaoInvalidaException("REGISTRO_COM_DEPENDENCIAS", previa.bloqueio());
    }

    private OperacaoInvalidaException dependenciaConcorrente() {
        return new OperacaoInvalidaException("DEPENDENCIA_CRIADA_CONCORRENTE",
            "O registro passou a ter dependências durante a operação. Atualize a página e confira os vínculos.");
    }
}

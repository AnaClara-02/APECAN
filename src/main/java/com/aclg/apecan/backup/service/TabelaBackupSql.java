package com.aclg.apecan.backup.service;

import java.util.List;

record TabelaBackupSql(String nome, String colunaId, List<String> colunas) {

	static List<TabelaBackupSql> todas() {
		return List.of(
			tabela("pacientes", "id_paciente", "id_paciente,nome,cpf,data_nascimento,telefone,endereco,local_tratamento,status,criado_por_nome_historico,atualizado_por_nome_historico,criado_em,atualizado_em,desativado_em"),
			tabela("historico_status_paciente", "id_historico", "id_historico,id_paciente,status,alterado_em,alterado_por_nome_historico,observacao"),
			tabela("voluntarios", "id_voluntario", "id_voluntario,nome,cpf,data_nascimento,telefone,endereco,status,criado_por_nome_historico,atualizado_por_nome_historico,criado_em,atualizado_em,desativado_em"),
			tabela("categorias_equipamentos", "id_categoria", "id_categoria,nome,descricao,criado_por_nome_historico,criado_em,atualizado_em"),
			tabela("doacoes", "id_doacao", "id_doacao,tipo_doacao,quantidade,unidade,data_doacao,fonte_doacao,registrado_por_nome_historico,criado_em"),
			tabela("equipamentos", "id_equipamento", "id_equipamento,id_categoria,id_doacao,estado_conservacao,status,criado_por_nome_historico,atualizado_por_nome_historico,criado_em,atualizado_em,versao"),
			tabela("doacao_voluntarios", null, "id_doacao,id_voluntario,papel"),
			tabela("emprestimos_equipamentos", "id_emprestimo", "id_emprestimo,id_equipamento,id_paciente,data_emprestimo,data_devolucao,registrado_por_nome_historico,observacao,devolvido_por_nome_historico,estado_conservacao_devolucao,versao,data_prevista_devolucao"),
			tabela("movimentacoes_financeiras", "id_movimentacao", "id_movimentacao,tipo_movimentacao,valor,data_movimentacao,destino,origem_tipo,origem_descricao,id_doacao,registrado_por_nome_historico,criado_em"),
			tabela("despesas", "id_despesa", "id_despesa,tipo_despesa,descricao,numero_nota_fiscal,id_movimentacao_financeira,registrado_por_nome_historico,criado_em")
		);
	}

	private static TabelaBackupSql tabela(String nome, String id, String colunas) {
		return new TabelaBackupSql(nome, id, List.of(colunas.split(",")));
	}
}

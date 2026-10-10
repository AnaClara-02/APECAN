package com.aclg.apecan.administracao.teste.entity;

public enum TipoRegistroExclusaoTeste {
    PACIENTE("Paciente"),
    VOLUNTARIO("Voluntário"),
    EQUIPAMENTO("Equipamento"),
    CATEGORIA("Categoria de equipamento"),
    EMPRESTIMO("Empréstimo"),
    DOACAO("Doação"),
    DESPESA("Despesa"),
    MOVIMENTACAO("Movimentação financeira");

    private final String rotulo;

    TipoRegistroExclusaoTeste(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}

package com.aclg.apecan.emprestimo.entity;

import com.aclg.apecan.equipamento.entity.Equipamento;
import com.aclg.apecan.paciente.entity.Paciente;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "emprestimos_equipamentos")
public class EmprestimoEquipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_emprestimo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_equipamento", nullable = false,
        foreignKey = @ForeignKey(name = "fk_emprestimos_equipamento"))
    private Equipamento equipamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false,
        foreignKey = @ForeignKey(name = "fk_emprestimos_paciente"))
    private Paciente paciente;

    @Column(name = "data_emprestimo", nullable = false)
    private LocalDate dataEmprestimo;

    @Column(name = "data_devolucao")
    private LocalDate dataDevolucao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_usuario_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_emprestimos_registrado_por"))
    private Usuario registradoPor;

    @Column(length = 255)
    private String observacao;

    protected EmprestimoEquipamento() {
    }

    public EmprestimoEquipamento(Equipamento equipamento, Paciente paciente,
                                 LocalDate dataEmprestimo,
                                 Usuario registradoPor, String observacao) {
        this.equipamento = Objects.requireNonNull(equipamento);
        this.paciente = Objects.requireNonNull(paciente);
        this.dataEmprestimo = Objects.requireNonNull(dataEmprestimo);
        this.registradoPor = Objects.requireNonNull(registradoPor);
        this.observacao = observacao;
    }

    public void registrarDevolucao(LocalDate dataDevolucao) {
        Objects.requireNonNull(dataDevolucao);
        if (this.dataDevolucao != null) {
            throw new OperacaoInvalidaException(
                "DEVOLUCAO_JA_REGISTRADA",
                "A devolução já foi registrada."
            );
        }
        if (dataDevolucao.isBefore(dataEmprestimo)) {
            throw new OperacaoInvalidaException(
                "DATA_DEVOLUCAO_INVALIDA",
                "A devolução não pode ocorrer antes do empréstimo."
            );
        }
        this.dataDevolucao = dataDevolucao;
    }

    public boolean estaAberto() {
        return dataDevolucao == null;
    }

    public Long getId() { return id; }
    public Equipamento getEquipamento() { return equipamento; }
    public Paciente getPaciente() { return paciente; }
    public LocalDate getDataEmprestimo() { return dataEmprestimo; }
    public LocalDate getDataDevolucao() { return dataDevolucao; }
    public Usuario getRegistradoPor() { return registradoPor; }
    public String getObservacao() { return observacao; }
}

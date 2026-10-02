package com.aclg.apecan.auth.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "controles_acesso")
public class ControleAcesso {
    @Id @Column(length = 80) private String chave;
    @Column(nullable = false) private int falhas;
    @Column(name = "inicio_janela", nullable = false) private long inicioJanela;
    @Column(name = "bloqueado_ate", nullable = false) private long bloqueadoAte;
    @Column(name = "ultimo_evento", nullable = false) private long ultimoEvento;
    @Column(name = "expira_em", nullable = false) private long expiraEm;
    protected ControleAcesso() {}
}

package com.aclg.apecan.regressao;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.aclg.apecan.equipamento.service.EquipamentoService;
import com.aclg.apecan.emprestimo.service.EmprestimoService;
import com.aclg.apecan.financeiro.service.FinanceiroService;
import com.aclg.apecan.voluntario.service.VoluntarioService;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ValidacaoServicosTests {
    @Autowired EquipamentoService equipamentos;
    @Autowired EmprestimoService emprestimos;
    @Autowired FinanceiroService financeiro;
    @Autowired VoluntarioService voluntarios;

    @Test void chamadasInternasTambemRejeitamFormulariosInvalidos() {
        assertThatThrownBy(() -> equipamentos.cadastrar(new com.aclg.apecan.equipamento.dto.EquipamentoForm()))
            .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> emprestimos.emprestar(new com.aclg.apecan.emprestimo.dto.EmprestimoForm()))
            .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> financeiro.registrar(new com.aclg.apecan.financeiro.dto.MovimentacaoForm()))
            .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> voluntarios.cadastrar(new com.aclg.apecan.voluntario.dto.VoluntarioForm()))
            .isInstanceOf(ConstraintViolationException.class);
    }
}

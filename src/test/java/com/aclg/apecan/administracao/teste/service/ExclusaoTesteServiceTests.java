package com.aclg.apecan.administracao.teste.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.aclg.apecan.administracao.teste.entity.TipoRegistroExclusaoTeste;
import com.aclg.apecan.auth.security.UsuarioPrincipal;
import com.aclg.apecan.auth.service.AtivacaoUsuarioService;
import com.aclg.apecan.equipamento.entity.CategoriaEquipamento;
import com.aclg.apecan.equipamento.entity.Equipamento;
import com.aclg.apecan.equipamento.entity.EstadoConservacao;
import com.aclg.apecan.equipamento.repository.CategoriaEquipamentoRepository;
import com.aclg.apecan.equipamento.repository.EquipamentoRepository;
import com.aclg.apecan.emprestimo.entity.EmprestimoEquipamento;
import com.aclg.apecan.emprestimo.repository.EmprestimoEquipamentoRepository;
import com.aclg.apecan.paciente.dto.NovoPacienteForm;
import com.aclg.apecan.paciente.entity.Paciente;
import com.aclg.apecan.paciente.repository.PacienteRepository;
import com.aclg.apecan.paciente.service.PacienteService;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.usuario.service.UsuarioService;

@SpringBootTest
@Transactional
class ExclusaoTesteServiceTests {

    private static final String SENHA = "Senha ficticia segura 2026";

    @Autowired private ExclusaoTesteService exclusoes;
    @Autowired private UsuarioService usuarios;
    @Autowired private AtivacaoUsuarioService ativacoes;
    @Autowired private PacienteService pacientes;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private CategoriaEquipamentoRepository categoriaRepository;
    @Autowired private EquipamentoRepository equipamentoRepository;
    @Autowired private EmprestimoEquipamentoRepository emprestimoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void excluiPacienteEHistoricoNaMesmaOperacaoEPreservaAuditoria() {
        Usuario admDev = criarAdmDev();
        autenticar(admDev);
        Long pacienteId = pacientes.cadastrar(formularioPaciente());

        exclusoes.excluir(TipoRegistroExclusaoTeste.PACIENTE, pacienteId);

        assertThat(pacienteRepository.existsById(pacienteId)).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM historico_status_paciente WHERE id_paciente = ?", Integer.class, pacienteId))
            .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE id_usuario = ?", Integer.class, admDev.getId()))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("""
            SELECT quantidade_removida FROM auditoria_exclusoes_teste
             WHERE tipo_registro = 'PACIENTE' AND id_registro = ?
            """, Integer.class, pacienteId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
            SELECT justificativa FROM auditoria_exclusoes_teste
             WHERE tipo_registro = 'PACIENTE' AND id_registro = ?
            """, String.class, pacienteId)).isEqualTo("Exclusão confirmada pela interface administrativa.");
    }

    @Test
    void bloqueiaPacienteComEmprestimoSemRemocaoParcial() {
        Usuario admDev = criarAdmDev();
        autenticar(admDev);
        Long pacienteId = pacientes.cadastrar(formularioPaciente());
        Paciente paciente = pacienteRepository.findById(pacienteId).orElseThrow();
        CategoriaEquipamento categoria = categoriaRepository.saveAndFlush(
            new CategoriaEquipamento("Categoria ficticia", null, admDev));
        Equipamento equipamento = new Equipamento(categoria, null, EstadoConservacao.BOM, admDev);
        equipamento.emprestar(admDev);
        equipamentoRepository.saveAndFlush(equipamento);
        emprestimoRepository.saveAndFlush(new EmprestimoEquipamento(equipamento, paciente,
            LocalDate.now(), LocalDate.now().plusDays(7), admDev, "Fixture de teste"));

        assertThatThrownBy(() -> exclusoes.excluir(TipoRegistroExclusaoTeste.PACIENTE,
                pacienteId))
            .isInstanceOf(OperacaoInvalidaException.class)
            .hasMessageContaining("empréstimos vinculados");

        assertThat(pacienteRepository.existsById(pacienteId)).isTrue();
        assertThat(emprestimoRepository.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_exclusoes_teste", Integer.class)).isZero();
    }

    private Usuario criarAdmDev() {
        NovoUsuarioForm formulario = new NovoUsuarioForm();
        formulario.setNome("Adm Dev Ficticio");
        formulario.setLogin("admdev.exclusao");
        formulario.setCpf("52998224725");
        formulario.setEmail("admdev.exclusao@example.invalid");
        formulario.setTelefone("14999999999");
        var criada = usuarios.cadastrarPrimeiroAdministrador(formulario);
        String token = URI.create(criada.ativacao().linkLocal()).getRawQuery().substring("token=".length());
        ativacoes.ativar(token, SENHA, SENHA);
        Usuario admDev = usuarioRepository.findByLogin("admdev.exclusao").orElseThrow();
        autenticar(admDev);
        return admDev;
    }

    private void autenticar(Usuario usuario) {
        UsuarioPrincipal principal = UsuarioPrincipal.de(usuario);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private NovoPacienteForm formularioPaciente() {
        NovoPacienteForm formulario = new NovoPacienteForm();
        formulario.setNome("Paciente ficticio de teste");
        formulario.setCpf("11144477735");
        formulario.setDataNascimento(LocalDate.of(1980, 1, 1));
        formulario.setTelefone("14988888888");
        formulario.setEndereco("Rua ficticia, 100");
        formulario.setLocalTratamento("Unidade ficticia");
        return formulario;
    }

}

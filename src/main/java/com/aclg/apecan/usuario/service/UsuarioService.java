package com.aclg.apecan.usuario.service;

import com.aclg.apecan.auth.security.SessaoUsuarioService;
import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.auth.security.ReautenticacaoService;
import com.aclg.apecan.auth.service.AtivacaoEmitida;
import com.aclg.apecan.auth.service.AtivacaoUsuarioService;
import com.aclg.apecan.auth.service.RevogacaoTokensService;
import com.aclg.apecan.shared.exception.ConflitoNegocioException;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.shared.exception.RecursoNaoEncontradoException;
import com.aclg.apecan.shared.validation.CpfNormalizer;
import com.aclg.apecan.shared.validation.TelefoneNormalizer;
import com.aclg.apecan.usuario.dto.AlterarAcessoForm;
import com.aclg.apecan.usuario.dto.EditarUsuarioForm;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.dto.UsuarioCriadoResultado;
import com.aclg.apecan.usuario.dto.UsuarioResumoDto;
import com.aclg.apecan.usuario.entity.HistoricoAdministracaoUsuario;
import com.aclg.apecan.usuario.entity.StatusUsuario;
import com.aclg.apecan.usuario.entity.TipoEventoAdministracaoUsuario;
import com.aclg.apecan.usuario.entity.TipoPerfil;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.mapper.UsuarioMapper;
import com.aclg.apecan.usuario.repository.HistoricoAdministracaoUsuarioRepository;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Validated
public class UsuarioService {

    public static final String FOTO_PADRAO = "/images/usuario-padrao.svg";

    private final UsuarioRepository usuarioRepository;
    private final HistoricoAdministracaoUsuarioRepository historicoRepository;
    private final AtivacaoUsuarioService ativacaoService;
    private final RevogacaoTokensService revogacaoTokens;
    private final UsuarioMapper usuarioMapper;
    private final UsuarioAtual usuarioAtual;
    private final ReautenticacaoService reautenticacao;
    private final SessaoUsuarioService sessaoUsuarioService;
    private final Clock clock;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            HistoricoAdministracaoUsuarioRepository historicoRepository,
            AtivacaoUsuarioService ativacaoService,
            RevogacaoTokensService revogacaoTokens,
            UsuarioMapper usuarioMapper,
            UsuarioAtual usuarioAtual,
            ReautenticacaoService reautenticacao,
            SessaoUsuarioService sessaoUsuarioService,
            Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.historicoRepository = historicoRepository;
        this.ativacaoService = ativacaoService;
        this.revogacaoTokens = revogacaoTokens;
        this.usuarioMapper = usuarioMapper;
        this.usuarioAtual = usuarioAtual;
        this.reautenticacao = reautenticacao;
        this.sessaoUsuarioService = sessaoUsuarioService;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UsuarioCriadoResultado cadastrarPrimeiroAdministrador(
            @Valid NovoUsuarioForm formulario) {
        if (usuarioRepository.count() != 0) {
            throw new OperacaoInvalidaException(
                "CONFIGURACAO_INICIAL_CONCLUIDA",
                "A configuracao inicial ja foi concluida."
            );
        }

        Usuario usuario = criarUsuario(formulario, TipoPerfil.ADMINISTRADOR, null);
        registrar(
            usuario,
            null,
            TipoEventoAdministracaoUsuario.CRIACAO,
            null,
            TipoPerfil.ADMINISTRADOR,
            null,
            StatusUsuario.ATIVO,
            "Criacao do primeiro administrador."
        );
        AtivacaoEmitida ativacao = ativacaoService.emitir(usuario);
        return new UsuarioCriadoResultado(usuarioMapper.paraResumo(usuario), ativacao);
    }

    @Transactional
    public UsuarioCriadoResultado cadastrar(@Valid NovoUsuarioForm formulario) {
        Usuario responsavel = exigirAdministradorAtual();
        Usuario usuario = criarUsuario(formulario, TipoPerfil.USUARIO, responsavel);
        registrar(
            usuario,
            responsavel,
            TipoEventoAdministracaoUsuario.CRIACAO,
            null,
            TipoPerfil.USUARIO,
            null,
            StatusUsuario.ATIVO,
            "Cadastro de funcionario."
        );
        AtivacaoEmitida ativacao = ativacaoService.emitir(usuario);
        return new UsuarioCriadoResultado(usuarioMapper.paraResumo(usuario), ativacao);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResumoDto> listar() {
        return usuarioRepository.findAll(Sort.by(Sort.Direction.ASC, "nome")).stream()
            .map(usuarioMapper::paraResumo)
            .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResumoDto buscar(Long id) {
        return usuarioMapper.paraResumo(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public UsuarioResumoDto buscarAtual() {
        return usuarioMapper.paraResumo(buscarEntidade(usuarioAtual.exigirId()));
    }

    @Transactional(readOnly = true)
    public EditarUsuarioForm formularioEdicao(Long id) {
        return usuarioMapper.paraFormularioEdicao(buscarEntidade(id));
    }

    @Transactional
    public void atualizar(Long id, @Valid EditarUsuarioForm formulario) {
        exigirAdministradorAtual();
        Usuario usuario = buscarParaAtualizacao(id);

        String login = normalizarLogin(formulario.getLogin());
        String email = normalizarEmail(formulario.getEmail());
        String telefone = normalizarTelefone(formulario.getTelefone());
        validarDuplicidadesEdicao(usuario.getId(), login, email);

        boolean loginAlterado = !usuario.getLogin().equals(login);
        boolean emailAlterado = !usuario.getEmail().equals(email);
        if (emailAlterado) {
            revogacaoTokens.revogarPendentes(usuario);
        }
        usuario.atualizarDadosPessoais(
            normalizarTexto(formulario.getNome()),
            login,
            email,
            FOTO_PADRAO,
            telefone
        );

        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw conflitoDuplicidade();
        }

        if (loginAlterado) {
            sessaoUsuarioService.encerrarSessoes(usuario.getId());
        }
    }

    @Transactional
    public AtivacaoEmitida reemitirAtivacao(Long id) {
        Usuario responsavel = exigirAdministradorAtual();
        Usuario usuario = buscarParaAtualizacao(id);
        AtivacaoEmitida ativacao = ativacaoService.emitir(usuario);
        registrar(
            usuario,
            responsavel,
            TipoEventoAdministracaoUsuario.REEMISSAO_ATIVACAO,
            usuario.getTipoPerfil(),
            usuario.getTipoPerfil(),
            usuario.getStatus(),
            usuario.getStatus(),
            "Reemissao do link de ativacao."
        );
        return ativacao;
    }

    @Transactional
    public void promover(Long id, @Valid AlterarAcessoForm formulario) {
        ContextoAlteracao contexto = prepararAlteracao(id, formulario);
        Usuario alvo = contexto.alvo();
        if (alvo.getTipoPerfil() == TipoPerfil.ADMINISTRADOR) {
            throw new OperacaoInvalidaException(
                "USUARIO_JA_ADMINISTRADOR",
                "O usuario ja possui perfil de administrador."
            );
        }
        if (alvo.getStatus() != StatusUsuario.ATIVO || !alvo.estaAtivado()) {
            throw new OperacaoInvalidaException(
                "USUARIO_NAO_APTO_PARA_PROMOCAO",
                "Ative a conta do usuario antes de promove-lo."
            );
        }

        TipoPerfil anterior = alvo.getTipoPerfil();
        alvo.alterarPerfil(TipoPerfil.ADMINISTRADOR);
        registrar(
            alvo,
            contexto.responsavel(),
            TipoEventoAdministracaoUsuario.PROMOCAO_ADMINISTRADOR,
            anterior,
            TipoPerfil.ADMINISTRADOR,
            alvo.getStatus(),
            alvo.getStatus(),
            contexto.justificativa()
        );
        usuarioRepository.flush();
        sessaoUsuarioService.encerrarSessoes(alvo.getId());
    }

    @Transactional
    public void rebaixar(Long id, @Valid AlterarAcessoForm formulario) {
        ContextoAlteracao contexto = prepararAlteracao(id, formulario);
        Usuario alvo = contexto.alvo();
        if (alvo.getTipoPerfil() != TipoPerfil.ADMINISTRADOR) {
            throw new OperacaoInvalidaException(
                "USUARIO_NAO_ADMINISTRADOR",
                "O usuario nao possui perfil de administrador."
            );
        }
        impedirRemocaoUltimoAdministrador(contexto.administradores(), alvo);

        alvo.alterarPerfil(TipoPerfil.USUARIO);
        registrar(
            alvo,
            contexto.responsavel(),
            TipoEventoAdministracaoUsuario.REBAIXAMENTO_USUARIO,
            TipoPerfil.ADMINISTRADOR,
            TipoPerfil.USUARIO,
            alvo.getStatus(),
            alvo.getStatus(),
            contexto.justificativa()
        );
        usuarioRepository.flush();
        sessaoUsuarioService.encerrarSessoes(alvo.getId());
    }

    @Transactional
    public void desativar(Long id, @Valid AlterarAcessoForm formulario) {
        ContextoAlteracao contexto = prepararAlteracao(id, formulario);
        Usuario alvo = contexto.alvo();
        if (alvo.getStatus() == StatusUsuario.INATIVO) {
            throw new OperacaoInvalidaException(
                "USUARIO_JA_INATIVO",
                "O usuario ja esta inativo."
            );
        }
        if (alvo.getTipoPerfil() == TipoPerfil.ADMINISTRADOR) {
            impedirRemocaoUltimoAdministrador(contexto.administradores(), alvo);
        }

        alvo.desativar(LocalDateTime.now(clock));
        revogacaoTokens.revogarPendentes(alvo);
        registrar(
            alvo,
            contexto.responsavel(),
            TipoEventoAdministracaoUsuario.DESATIVACAO,
            alvo.getTipoPerfil(),
            alvo.getTipoPerfil(),
            StatusUsuario.ATIVO,
            StatusUsuario.INATIVO,
            contexto.justificativa()
        );
        usuarioRepository.flush();
        sessaoUsuarioService.encerrarSessoes(alvo.getId());
    }

    @Transactional
    public void reativar(Long id, @Valid AlterarAcessoForm formulario) {
        ContextoAlteracao contexto = prepararAlteracao(id, formulario);
        Usuario alvo = contexto.alvo();
        if (alvo.getStatus() == StatusUsuario.ATIVO) {
            throw new OperacaoInvalidaException(
                "USUARIO_JA_ATIVO",
                "O usuario ja esta ativo."
            );
        }

        alvo.reativar();
        // Inclui contas desativadas por versões anteriores, que não revogavam links.
        revogacaoTokens.revogarPendentes(alvo);
        registrar(
            alvo,
            contexto.responsavel(),
            TipoEventoAdministracaoUsuario.REATIVACAO,
            alvo.getTipoPerfil(),
            alvo.getTipoPerfil(),
            StatusUsuario.INATIVO,
            StatusUsuario.ATIVO,
            contexto.justificativa()
        );
        usuarioRepository.flush();
    }

    @Transactional(readOnly = true)
    public long quantidadeAdministradoresAtivos() {
        return usuarioRepository.findAll().stream()
            .filter(this::administradorAtivo)
            .count();
    }

    private Usuario criarUsuario(
            NovoUsuarioForm formulario,
            TipoPerfil perfil,
            Usuario criadoPor) {
        String login = normalizarLogin(formulario.getLogin());
        String cpf = CpfNormalizer.normalizar(formulario.getCpf());
        String email = normalizarEmail(formulario.getEmail());
        String telefone = normalizarTelefone(formulario.getTelefone());
        validarDuplicidadesCriacao(login, cpf, email);

        Usuario usuario = new Usuario(
            normalizarTexto(formulario.getNome()),
            login,
            cpf,
            email,
            FOTO_PADRAO,
            telefone,
            perfil,
            criadoPor
        );
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw conflitoDuplicidade();
        }
    }

    private ContextoAlteracao prepararAlteracao(
            Long alvoId,
            AlterarAcessoForm formulario) {
        Long responsavelId = usuarioAtual.exigirId();
        List<Usuario> administradores = usuarioRepository.findAdministradoresForUpdate();
        Usuario responsavel = administradores.stream()
            .filter(usuario -> usuario.getId().equals(responsavelId))
            .findFirst()
            .orElseThrow(() -> new OperacaoInvalidaException(
                "ADMINISTRADOR_NAO_AUTORIZADO",
                "O usuario atual nao pode realizar esta operacao."
            ));
        validarAdministradorAtivo(responsavel);
        validarSenhaAtual(responsavel, formulario.getSenhaAtual());

        Usuario alvo = administradores.stream()
            .filter(usuario -> usuario.getId().equals(alvoId))
            .findFirst()
            .orElseGet(() -> buscarParaAtualizacao(alvoId));
        return new ContextoAlteracao(
            responsavel,
            alvo,
            administradores,
            normalizarTexto(formulario.getJustificativa())
        );
    }

    private Usuario exigirAdministradorAtual() {
        Usuario usuario = buscarEntidade(usuarioAtual.exigirId());
        validarAdministradorAtivo(usuario);
        return usuario;
    }

    private void validarAdministradorAtivo(Usuario usuario) {
        if (!administradorAtivo(usuario)) {
            throw new OperacaoInvalidaException(
                "ADMINISTRADOR_NAO_AUTORIZADO",
                "O usuario atual nao pode realizar esta operacao."
            );
        }
    }

    private boolean administradorAtivo(Usuario usuario) {
        return usuario.getTipoPerfil() == TipoPerfil.ADMINISTRADOR
            && usuario.getStatus() == StatusUsuario.ATIVO
            && usuario.estaAtivado();
    }

    private void validarSenhaAtual(Usuario responsavel, String senhaAtual) {
        reautenticacao.validar(responsavel, senhaAtual);
    }

    private void impedirRemocaoUltimoAdministrador(
            List<Usuario> administradores,
            Usuario alvo) {
        long ativos = administradores.stream().filter(this::administradorAtivo).count();
        if (administradorAtivo(alvo) && ativos <= 1) {
            throw new OperacaoInvalidaException(
                "ULTIMO_ADMINISTRADOR",
                "Nao e permitido remover o ultimo administrador ativo."
            );
        }
    }

    private void registrar(
            Usuario usuario,
            Usuario responsavel,
            TipoEventoAdministracaoUsuario evento,
            TipoPerfil perfilAnterior,
            TipoPerfil perfilNovo,
            StatusUsuario statusAnterior,
            StatusUsuario statusNovo,
            String justificativa) {
        historicoRepository.save(new HistoricoAdministracaoUsuario(
            usuario,
            responsavel,
            evento,
            perfilAnterior,
            perfilNovo,
            statusAnterior,
            statusNovo,
            justificativa,
            LocalDateTime.now(clock)
        ));
    }

    private void validarDuplicidadesCriacao(String login, String cpf, String email) {
        if (usuarioRepository.existsByLogin(login)) {
            throw new ConflitoNegocioException("LOGIN_JA_CADASTRADO", "Login ja cadastrado.");
        }
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new ConflitoNegocioException("CPF_JA_CADASTRADO", "CPF ja cadastrado.");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflitoNegocioException("EMAIL_JA_CADASTRADO", "E-mail ja cadastrado.");
        }
    }

    private void validarDuplicidadesEdicao(Long id, String login, String email) {
        if (usuarioRepository.existsByLoginAndIdNot(login, id)) {
            throw new ConflitoNegocioException("LOGIN_JA_CADASTRADO", "Login ja cadastrado.");
        }
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflitoNegocioException("EMAIL_JA_CADASTRADO", "E-mail ja cadastrado.");
        }
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> usuarioNaoEncontrado(id));
    }

    private Usuario buscarParaAtualizacao(Long id) {
        return usuarioRepository.findByIdForUpdate(id)
            .orElseThrow(() -> usuarioNaoEncontrado(id));
    }

    private RecursoNaoEncontradoException usuarioNaoEncontrado(Long id) {
        return new RecursoNaoEncontradoException(
            "USUARIO_NAO_ENCONTRADO",
            "Usuario " + id + " nao encontrado."
        );
    }

    private ConflitoNegocioException conflitoDuplicidade() {
        return new ConflitoNegocioException(
            "USUARIO_DUPLICADO",
            "Login, CPF ou e-mail ja cadastrado."
        );
    }

    private String normalizarLogin(String login) {
        return normalizarTexto(login).toLowerCase(Locale.ROOT);
    }

    private String normalizarEmail(String email) {
        return normalizarTexto(email).toLowerCase(Locale.ROOT);
    }

    private String normalizarTelefone(String telefone) {
        return TelefoneNormalizer.normalizar(telefone);
    }

    /** Exclusivo do comando bootstrap: recupera uma falha de entrega antes do primeiro login. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AtivacaoEmitida reemitirPrimeiraAtivacao() {
        if (usuarioRepository.count() != 1) {
            throw new OperacaoInvalidaException("CONFIGURACAO_INICIAL_CONCLUIDA",
                "A reemissão inicial exige uma única conta pendente.");
        }
        Usuario usuario = usuarioRepository.findAll().getFirst();
        if (usuario.getTipoPerfil() != TipoPerfil.ADMINISTRADOR || usuario.estaAtivado()
                || usuario.getStatus() != StatusUsuario.ATIVO) {
            throw new OperacaoInvalidaException("CONFIGURACAO_INICIAL_CONCLUIDA",
                "A configuração inicial já foi concluída.");
        }
        AtivacaoEmitida ativacao = ativacaoService.emitir(usuario);
        registrar(usuario, null, TipoEventoAdministracaoUsuario.REEMISSAO_ATIVACAO,
            usuario.getTipoPerfil(), usuario.getTipoPerfil(), usuario.getStatus(), usuario.getStatus(),
            "Reemissão inicial solicitada pelo comando local.");
        return ativacao;
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new OperacaoInvalidaException(
                "CAMPO_OBRIGATORIO",
                "Preencha todos os campos obrigatorios."
            );
        }
        return texto.trim();
    }

    private record ContextoAlteracao(
        Usuario responsavel,
        Usuario alvo,
        List<Usuario> administradores,
        String justificativa
    ) {
    }
}

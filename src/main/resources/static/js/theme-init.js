(() => {
    const chave = 'apecan-tema';
    const sistemaEscuro = window.matchMedia('(prefers-color-scheme: dark)');
    const armazenado = (() => {
        try { return localStorage.getItem(chave); } catch (_) { return null; }
    })();
    const temaInicial = armazenado === 'light' || armazenado === 'dark'
        ? armazenado : (sistemaEscuro.matches ? 'dark' : 'light');
    document.documentElement.dataset.theme = temaInicial;

    const atualizarBotao = (botao) => {
        const escuro = document.documentElement.dataset.theme === 'dark';
        botao.textContent = escuro ? '☀' : '☾';
        botao.setAttribute('aria-label', escuro ? 'Ativar modo claro' : 'Ativar modo noturno');
        botao.setAttribute('aria-pressed', String(escuro));
        botao.title = botao.getAttribute('aria-label');
    };

    document.addEventListener('DOMContentLoaded', () => {
        if (document.querySelector('[data-alternar-tema]')) return;
        const botao = document.createElement('button');
        botao.type = 'button';
        botao.className = 'botao-icone alternar-tema';
        botao.dataset.alternarTema = '';
        atualizarBotao(botao);
        botao.addEventListener('click', () => {
            const novo = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
            document.documentElement.dataset.theme = novo;
            try { localStorage.setItem(chave, novo); } catch (_) { /* preferência apenas nesta página */ }
            atualizarBotao(botao);
        });
        const cabecalho = document.querySelector('.cabecalho');
        if (cabecalho) {
            const usuario = cabecalho.querySelector('.usuario-cabecalho');
            cabecalho.insertBefore(botao, usuario);
        } else {
            botao.classList.add('alternar-tema-global');
            document.body.appendChild(botao);
        }
    });
})();

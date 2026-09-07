(() => {
    document.querySelectorAll('form[data-confirmar]').forEach((formulario) => {
        formulario.addEventListener('submit', (evento) => {
            if (!window.confirm(formulario.dataset.confirmar)) evento.preventDefault();
        });
    });

    document.querySelectorAll('[data-somente-letras]').forEach((campo) => {
        campo.addEventListener('input', () => {
            const valorOriginal = campo.value;
            const posicaoOriginal = campo.selectionStart ?? valorOriginal.length;
            const trechoAntesDoCursor = valorOriginal.slice(0, posicaoOriginal);
            const valorLimpo = valorOriginal.replace(/[^\p{L}\p{M} '’-]/gu, '');

            if (valorLimpo === valorOriginal) return;

            const trechoLimpo = trechoAntesDoCursor.replace(/[^\p{L}\p{M} '’-]/gu, '');
            campo.value = valorLimpo;
            campo.setSelectionRange(trechoLimpo.length, trechoLimpo.length);
        });
    });

    const body = document.body;
    const openButton = document.querySelector('[data-abrir-menu]');
    const closeButtons = document.querySelectorAll('[data-fechar-menu]');
    if (!openButton) return;
    const setMenu = (open) => {
        body.classList.toggle('menu-aberto', open);
        openButton.setAttribute('aria-expanded', String(open));
        if (open) document.querySelector('.barra-lateral a')?.focus();
        else openButton.focus();
    };
    openButton.addEventListener('click', () => setMenu(true));
    closeButtons.forEach((button) => button.addEventListener('click', () => setMenu(false)));
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && body.classList.contains('menu-aberto')) setMenu(false);
    });
    window.addEventListener('resize', () => {
        if (window.innerWidth >= 1024) {
            body.classList.remove('menu-aberto');
            openButton.setAttribute('aria-expanded', 'false');
        }
    });
})();

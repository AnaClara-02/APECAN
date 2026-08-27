(() => {
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

(() => {
    const botao = document.querySelector("[data-alternar-senha]");
    if (!botao) return;

    const campo = document.getElementById(botao.getAttribute("aria-controls"));
    const rotulo = botao.querySelector("[data-rotulo-senha]");
    if (!campo || !rotulo) return;

    botao.addEventListener("click", () => {
        const mostrar = campo.type === "password";
        campo.type = mostrar ? "text" : "password";
        botao.setAttribute("aria-pressed", String(mostrar));
        botao.setAttribute("aria-label", mostrar ? "Ocultar senha" : "Mostrar senha");
        rotulo.textContent = mostrar ? "Ocultar" : "Mostrar";
        campo.focus();
    });
})();

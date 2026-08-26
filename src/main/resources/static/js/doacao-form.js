(() => {
    "use strict";

    const formatadorMoeda = new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    const digitosMonetarios = (valor) => valor.replace(/\D/g, "").replace(/^0+(?=\d)/, "").slice(0, 12);

    const formatarMoeda = (valor) => {
        const digitos = digitosMonetarios(valor);
        const centavos = Number(digitos || "0") / 100;
        return formatadorMoeda.format(centavos);
    };

    const converterParaDecimal = (valor) => {
        const digitos = digitosMonetarios(valor).padStart(3, "0");
        return `${digitos.slice(0, -2)}.${digitos.slice(-2)}`;
    };

    document.addEventListener("DOMContentLoaded", () => {
        const formulario = document.querySelector("[data-form-doacao]");
        if (!formulario) return;

        const seletorTipo = formulario.querySelector("[data-tipo-doacao]");
        const grupos = formulario.querySelectorAll("[data-grupo-doacao]");
        const campoValor = formulario.querySelector("[data-moeda-brl]");

        const atualizarGrupos = () => {
            const tipo = seletorTipo.value;
            grupos.forEach((grupo) => {
                const ativo = grupo.dataset.grupoDoacao === tipo;
                grupo.hidden = !ativo;
                grupo.setAttribute("aria-hidden", String(!ativo));
                grupo.querySelectorAll("input, select, textarea").forEach((campo) => {
                    campo.disabled = !ativo;
                    campo.required = ativo && campo.hasAttribute("data-obrigatorio-tipo");
                });
            });
        };

        campoValor.value = formatarMoeda(campoValor.value);
        campoValor.addEventListener("input", () => {
            campoValor.value = formatarMoeda(campoValor.value);
        });
        seletorTipo.addEventListener("change", atualizarGrupos);
        atualizarGrupos();

        formulario.addEventListener("submit", () => {
            if (!campoValor.disabled) {
                campoValor.value = converterParaDecimal(campoValor.value);
            }
        });
    });
})();

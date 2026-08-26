(() => {
    "use strict";

    const nacionais = (valor) => {
        let digitos = valor.replace(/\D/g, "");
        if (digitos.startsWith("55")) {
            digitos = digitos.slice(2);
        }
        return digitos.slice(0, 11);
    };

    const formatar = (valor) => {
        const digitos = nacionais(valor);
        if (digitos.length === 0) return "+55 ";
        if (digitos.length <= 2) return `+55 (${digitos}`;

        const ddd = digitos.slice(0, 2);
        const numero = digitos.slice(2);
        if (numero.length === 0) return `+55 (${ddd}) `;

        const separador = numero.length > 8 ? 5 : 4;
        if (numero.length <= separador) return `+55 (${ddd}) ${numero}`;
        return `+55 (${ddd}) ${numero.slice(0, separador)}-${numero.slice(separador)}`;
    };

    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll("[data-telefone]").forEach((campo) => {
            campo.setAttribute("type", "tel");
            campo.setAttribute("inputmode", "numeric");
            campo.setAttribute("maxlength", "19");
            campo.setAttribute("placeholder", "+55 (XX) XXXXX-XXXX");
            campo.value = formatar(campo.value);
            campo.addEventListener("input", () => {
                campo.value = formatar(campo.value);
            });
            campo.addEventListener("focus", () => {
                if (campo.value === "") campo.value = "+55 ";
            });
            campo.addEventListener("blur", () => {
                if (campo.value === "+55 ") campo.value = "";
            });
        });
    });
})();

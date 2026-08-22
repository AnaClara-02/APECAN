(() => {
    "use strict";

    const somenteDigitos = (valor) => valor.replace(/\D/g, "").slice(0, 11);

    const formatar = (valor) => {
        const digitos = somenteDigitos(valor);
        if (digitos.length <= 3) return digitos;
        if (digitos.length <= 6) {
            return `${digitos.slice(0, 3)}.${digitos.slice(3)}`;
        }
        if (digitos.length <= 9) {
            return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6)}`;
        }
        return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-${digitos.slice(9)}`;
    };

    document.addEventListener("DOMContentLoaded", () => {
        const camposCpf = document.querySelectorAll("[data-cpf]");

        camposCpf.forEach((campo) => {
            campo.setAttribute("inputmode", "numeric");
            campo.setAttribute("maxlength", "14");
            campo.value = formatar(campo.value);
            campo.addEventListener("input", () => {
                campo.value = formatar(campo.value);
            });
        });

        document.querySelectorAll("form").forEach((formulario) => {
            formulario.addEventListener("submit", () => {
                formulario.querySelectorAll("[data-cpf]").forEach((campo) => {
                    campo.value = somenteDigitos(campo.value);
                });
            });
        });
    });
})();

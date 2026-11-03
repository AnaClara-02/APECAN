(() => {
  "use strict";

  document.querySelectorAll("[data-despesa-form]").forEach((form) => {
    const possuiNota = form.querySelector("[data-possui-nota]");
    const camposNota = form.querySelector("[data-campos-nota]");
    const camposNotaFiscal = [...form.querySelectorAll("[data-campo-nota]")];
    const banco = form.querySelector("[data-banco-pagamento]");
    const outroBancoGrupo = form.querySelector("[data-outro-banco]");
    const outroBanco = form.querySelector("[data-campo-outro]");
    const cnpj = form.querySelector("#cnpjEmitente");

    const alternarCamposNota = () => {
      const ativo = possuiNota.checked;
      camposNota.hidden = !ativo;
      camposNotaFiscal.forEach((campo) => {
        campo.disabled = !ativo;
        campo.required = ativo;
        if (!ativo) campo.value = "";
      });
    };

    const alternarOutroBanco = () => {
      const ativo = banco.value === "OUTRO";
      outroBancoGrupo.hidden = !ativo;
      outroBanco.disabled = !ativo;
      outroBanco.required = ativo;
      if (!ativo) outroBanco.value = "";
    };

    const formatarCnpj = () => {
      const valor = cnpj.value.replace(/[^a-z0-9]/gi, "").toUpperCase().slice(0, 14);
      const partes = [
        valor.slice(0, 2),
        valor.slice(2, 5),
        valor.slice(5, 8),
        valor.slice(8, 12),
        valor.slice(12, 14)
      ];
      let formatado = partes[0];
      if (partes[1]) formatado += "." + partes[1];
      if (partes[2]) formatado += "." + partes[2];
      if (partes[3]) formatado += "/" + partes[3];
      if (partes[4]) formatado += "-" + partes[4];
      cnpj.value = formatado;
    };

    possuiNota.addEventListener("change", alternarCamposNota);
    banco.addEventListener("change", alternarOutroBanco);
    cnpj.addEventListener("input", formatarCnpj);
    alternarCamposNota();
    alternarOutroBanco();
    formatarCnpj();
  });
})();

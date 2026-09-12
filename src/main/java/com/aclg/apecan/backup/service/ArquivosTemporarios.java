package com.aclg.apecan.backup.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

final class ArquivosTemporarios {

	private ArquivosTemporarios() {
	}

	static void apagarRecursivamente(Path caminho) {
		if (caminho == null || !Files.exists(caminho)) {
			return;
		}
		try (var caminhos = Files.walk(caminho)) {
			caminhos.sorted(Comparator.reverseOrder()).forEach(ArquivosTemporarios::apagarSilenciosamente);
		}
		catch (IOException ignored) {
			// A limpeza também é tentada individualmente; nunca expor caminhos no retorno ao usuário.
		}
	}

	static void apagarSilenciosamente(Path caminho) {
		if (caminho == null) {
			return;
		}
		try {
			Files.deleteIfExists(caminho);
		}
		catch (IOException ignored) {
			caminho.toFile().deleteOnExit();
		}
	}
}

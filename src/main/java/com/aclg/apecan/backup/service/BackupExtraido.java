package com.aclg.apecan.backup.service;

import java.nio.file.Path;

public record BackupExtraido(Path diretorio, Path dump, BackupManifest manifesto) implements AutoCloseable {

	@Override
	public void close() {
		ArquivosTemporarios.apagarRecursivamente(diretorio);
	}
}

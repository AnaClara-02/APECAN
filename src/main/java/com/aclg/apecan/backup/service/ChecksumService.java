package com.aclg.apecan.backup.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class ChecksumService {

	public String sha256(Path arquivo) {
		try (InputStream entrada = Files.newInputStream(arquivo)) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] buffer = new byte[8192];
			int lidos;
			while ((lidos = entrada.read(buffer)) >= 0) {
				digest.update(buffer, 0, lidos);
			}
			return HexFormat.of().formatHex(digest.digest());
		}
		catch (IOException | NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Não foi possível calcular a integridade do backup.", exception);
		}
	}
}

package com.aclg.apecan.backup.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class RecuperacaoEncerramentoService {

	private final ConfigurableApplicationContext contexto;
	private final boolean encerrarAposRestaurar;

	public RecuperacaoEncerramentoService(ConfigurableApplicationContext contexto,
			@Value("${apecan.recovery.shutdown-after-restore:false}") boolean encerrarAposRestaurar) {
		this.contexto = contexto;
		this.encerrarAposRestaurar = encerrarAposRestaurar;
	}

	public boolean agendarSeConfigurado() {
		if (!encerrarAposRestaurar) {
			return false;
		}
		var executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "apecan-recovery-shutdown");
			thread.setDaemon(false);
			return thread;
		});
		executor.schedule(() -> {
			SpringApplication.exit(contexto, () -> 0);
			executor.shutdown();
		}, 4, TimeUnit.SECONDS);
		return true;
	}
}

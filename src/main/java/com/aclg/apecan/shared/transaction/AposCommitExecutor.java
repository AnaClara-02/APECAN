package com.aclg.apecan.shared.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AposCommitExecutor {

	private static final Logger LOG = LoggerFactory.getLogger(AposCommitExecutor.class);

	public void executar(Runnable tarefa, String descricaoSegura) {
		Runnable protegida = () -> {
			try {
				tarefa.run();
			}
			catch (RuntimeException exception) {
				LOG.error("Falha ao executar tarefa apos commit: {}", descricaoSegura, exception);
			}
		};
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			protegida.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				protegida.run();
			}
		});
	}
}

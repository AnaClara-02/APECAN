package com.aclg.apecan.backup.repository;

import com.aclg.apecan.backup.entity.HistoricoBackup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoBackupRepository extends JpaRepository<HistoricoBackup, Long> {
}

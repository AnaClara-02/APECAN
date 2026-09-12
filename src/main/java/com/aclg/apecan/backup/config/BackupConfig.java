package com.aclg.apecan.backup.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BackupProperties.class)
public class BackupConfig {
}

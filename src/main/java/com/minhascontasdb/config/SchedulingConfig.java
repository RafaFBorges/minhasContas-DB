package com.minhascontasdb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Habilita as tarefas periódicas (@Scheduled), hoje só a limpeza de refresh tokens vencidos
@Configuration
@EnableScheduling
public class SchedulingConfig {
}

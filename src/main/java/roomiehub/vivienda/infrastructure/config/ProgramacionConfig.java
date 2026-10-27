package roomiehub.vivienda.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa el @Scheduled de TareasProgramadas; app.jobs.cron = "-" lo apaga (los tests lo hacen). */
@Configuration
@EnableScheduling
public class ProgramacionConfig {}

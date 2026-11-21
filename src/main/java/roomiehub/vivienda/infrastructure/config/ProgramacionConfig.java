package roomiehub.vivienda.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Activa los procesos programados y app jobs cron en guion los apaga como en los tests
@Configuration
@EnableScheduling
public class ProgramacionConfig {}

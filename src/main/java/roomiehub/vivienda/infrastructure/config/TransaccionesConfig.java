package roomiehub.vivienda.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// La transaccion se abre antes que RlsContextAspect para que set_config quede dentro
@Configuration
@EnableTransactionManagement(order = 0, proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TransaccionesConfig {}

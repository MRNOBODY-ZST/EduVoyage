package cn.edu.shmtu.eduvoyage.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Shared application beans that don't belong to a specific infrastructure
 * concern. The {@link Clock} is injected wherever business rules depend on the
 * current time (e.g. homework deadlines) so those rules can be driven
 * deterministically from tests with a fixed clock.
 */
@Configuration(proxyBeanMethods = false)
public class AppConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}

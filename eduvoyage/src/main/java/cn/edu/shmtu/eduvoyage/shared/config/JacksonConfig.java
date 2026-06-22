package cn.edu.shmtu.eduvoyage.shared.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * JSON codec customisation shared by WebFlux and manual error renderers.
 *
 * <p>Snowflake ids exceed JavaScript's safe integer range, so boxed {@link Long}
 * values are emitted as strings. Primitive {@code long} counters/timestamps stay
 * numeric.</p>
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer longIdStringJsonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("eduvoyage-long-id-string");
            module.addSerializer(Long.class, ToStringSerializer.instance);
            builder.addModule(module);
        };
    }
}

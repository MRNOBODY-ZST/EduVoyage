package cn.edu.shmtu.eduvoyage.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration as a {@link CorsConfigurationSource} bean. Spring Security
 * picks this up automatically in {@code ServerHttpSecurity#cors}. Origins are
 * overridable via {@code EDUVOYAGE_CORS_ORIGINS}; defaults cover the Vite dev
 * server.
 */
@Configuration(proxyBeanMethods = false)
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value(
                    "${EDUVOYAGE_CORS_ORIGINS:http://localhost:5173,http://127.0.0.1:5173}") String origins) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(origins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Trace-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

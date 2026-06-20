package cn.edu.shmtu.eduvoyage.shared.security;

import cn.edu.shmtu.eduvoyage.shared.config.EduVoyageProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import reactor.core.publisher.Mono;

/**
 * Reactive security configuration implementing the dual-token + RBAC scheme.
 *
 * <ul>
 *   <li><b>Route-level</b>: a permit-all allowlist (login, register, refresh,
 *       captcha, share links, swagger, health) is open; everything else
 *       requires a valid access token.</li>
 *   <li><b>Method-level</b>: {@code @EnableReactiveMethodSecurity} turns on
 *       {@code @PreAuthorize} so handlers can assert
 *       {@code hasRole('TEACHER')} / {@code hasAuthority('course:create')}.</li>
 * </ul>
 *
 * <p>Stateless: no server-side {@code SecurityContext} is persisted; identity is
 * carried entirely by the JWT and re-established per request by
 * {@link JwtServerAuthenticationConverter}.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class ReactiveSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The JWT converter already returns a fully-authenticated token, so the
     * manager is a pass-through that simply echoes it back.
     */
    @Bean
    public ReactiveAuthenticationManager jwtAuthenticationManager() {
        return Mono::just;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            EduVoyageProperties properties,
            JwtServerAuthenticationConverter jwtConverter,
            ReactiveAuthenticationManager authenticationManager,
            SecurityResponseHandlers responseHandlers,
            CorsConfigurationSource corsConfigurationSource) {

        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(authenticationManager);
        jwtFilter.setServerAuthenticationConverter(jwtConverter);
        // stateless: never persist the SecurityContext between requests
        jwtFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());

        String[] permitAll = properties.security().permitAll().toArray(String[]::new);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(permitAll).permitAll()
                        .anyExchange().authenticated())
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(responseHandlers)
                        .accessDeniedHandler(responseHandlers))
                .build();
    }
}

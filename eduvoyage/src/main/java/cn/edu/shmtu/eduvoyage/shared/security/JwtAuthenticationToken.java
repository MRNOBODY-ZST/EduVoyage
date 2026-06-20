package cn.edu.shmtu.eduvoyage.shared.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Authentication backed by a validated access-token {@link AuthUser}. Authorities
 * are the union of:
 * <ul>
 *   <li>role codes prefixed {@code ROLE_} (so {@code hasRole('ADMIN')} works), and</li>
 *   <li>raw permission codes (so {@code hasAuthority('course:create')} works).</li>
 * </ul>
 * Already authenticated on construction — it is only created from a verified JWT.
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final transient AuthUser principal;

    public JwtAuthenticationToken(AuthUser principal) {
        super(buildAuthorities(principal));
        this.principal = principal;
        setAuthenticated(true);
    }

    private static Collection<GrantedAuthority> buildAuthorities(AuthUser user) {
        Stream<String> roleAuthorities = user.roles() == null ? Stream.empty()
                : user.roles().stream().map(r -> "ROLE_" + r);
        Stream<String> permAuthorities = user.permissions() == null ? Stream.empty()
                : user.permissions().stream();
        return Stream.concat(roleAuthorities, permAuthorities)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Object getCredentials() {
        return null; // credentials (the raw token) are not retained
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.username();
    }
}

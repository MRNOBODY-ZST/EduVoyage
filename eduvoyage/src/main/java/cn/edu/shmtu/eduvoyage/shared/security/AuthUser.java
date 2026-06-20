package cn.edu.shmtu.eduvoyage.shared.security;

import java.io.Serializable;
import java.util.Set;

/**
 * Immutable authenticated principal placed into the reactive security context
 * after a JWT is validated. Carries everything route- and method-level
 * authorization needs without another DB round-trip.
 *
 * @param id          user primary key
 * @param username    login name
 * @param roles       role codes (e.g. {@code STUDENT}, {@code TEACHER}, {@code ADMIN})
 * @param permissions permission codes (e.g. {@code course:create})
 */
public record AuthUser(
        Long id,
        String username,
        Set<String> roles,
        Set<String> permissions
) implements Serializable {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}

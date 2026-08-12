package com.sanjay.aisecurity.security;

import com.sanjay.aisecurity.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetails implementation.
 *
 * <p>Adapts the platform {@link User} entity to the Spring Security
 * {@link UserDetails} contract. Spring Security uses this object throughout
 * the authentication filter chain to enforce access control decisions.</p>
 *
 * <p>Role enum values are prefixed with {@code ROLE_} to satisfy Spring
 * Security's {@link SimpleGrantedAuthority} naming convention.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public class CustomUserDetails implements UserDetails {

    /** The underlying domain User entity. */
    @Getter
    private final User user;

    /**
     * Constructs a {@code CustomUserDetails} adapter for the given {@link User}.
     *
     * @param user the authenticated domain user; must not be {@code null}
     */
    public CustomUserDetails(User user) {
        this.user = user;
    }

    /**
     * Returns the single {@link GrantedAuthority} derived from the user's role.
     *
     * <p>Format: {@code ROLE_<ROLE_NAME>} (e.g. {@code ROLE_USER}, {@code ROLE_ADMIN}).</p>
     *
     * @return an immutable singleton list containing the user's authority
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    /**
     * Returns the BCrypt-encoded password stored for the user.
     *
     * @return the hashed password string
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Returns the user's email address, which serves as the authentication
     * principal (username) in this application.
     *
     * @return the user's email address
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /**
     * Indicates whether the account is non-expired.
     * Currently all accounts are considered non-expired.
     *
     * @return {@code true} always
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the account is non-locked.
     * Currently all accounts are considered non-locked.
     *
     * @return {@code true} always
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the credentials are non-expired.
     * Currently all credentials are considered non-expired.
     *
     * @return {@code true} always
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Delegates to the {@link User#isActive()} flag to enforce account
     * deactivation at the Spring Security layer.
     *
     * @return {@code true} if the account is active; {@code false} if deactivated
     */
    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}

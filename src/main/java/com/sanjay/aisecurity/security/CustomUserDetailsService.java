package com.sanjay.aisecurity.security;

import com.sanjay.aisecurity.entity.User;
import com.sanjay.aisecurity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService.
 *
 * <p>Implements Spring Security's {@link UserDetailsService} to resolve
 * user accounts from the MySQL database using the email address as the
 * unique login identifier.</p>
 *
 * <p>Invoked by the {@link org.springframework.security.authentication.AuthenticationManager}
 * during credential verification and by {@link JwtAuthenticationFilter} when
 * populating the {@link org.springframework.security.core.context.SecurityContext}
 * from a valid JWT.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a {@link UserDetails} instance by the provided email address.
     *
     * <p>The {@code username} parameter in the Spring Security contract maps
     * to the user's email address in this application.</p>
     *
     * @param email the email address of the user to look up
     * @return a {@link CustomUserDetails} adapter wrapping the found {@link User}
     * @throws UsernameNotFoundException if no user exists with the given email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        return new CustomUserDetails(user);
    }
}

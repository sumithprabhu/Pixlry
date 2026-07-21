package com.imageplatform.auth.security;

import com.imageplatform.auth.entity.User;
import com.imageplatform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SERVICE: auth-service
 * PURPOSE: Bridges our User entity with Spring Security's authentication mechanism.
 *
 * WHY THIS EXISTS:
 *   Spring Security's AuthenticationManager (used during login) needs a way to load
 *   a user by username (email in our case) so it can verify the password. It doesn't
 *   know about our User entity or UserRepository — it only speaks UserDetails.
 *   This class is the adapter between the two worlds.
 *
 * WHEN IS THIS CALLED?
 *   Only during LOGIN. The AuthenticationManager calls loadUserByUsername(email),
 *   gets back a UserDetails, then compares the provided password against the stored
 *   BCrypt hash. After that, for all subsequent requests, the JWT filter handles
 *   identity — this service is never called again until the next login.
 *
 * INTERVIEW Q: What is UserDetailsService?
 *   A Spring Security interface with a single method: loadUserByUsername(String).
 *   It's the extension point for plugging your own user store (DB, LDAP, in-memory)
 *   into Spring Security's authentication machinery.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Spring Security's built-in User class (not our entity) — just a wrapper
        // that carries username, password hash, and authorities into the auth flow.
        return new org.springframework.security.core.userdetails.User(
                user.getId().toString(),          // principal = userId (not email)
                user.getPassword(),               // BCrypt hash — Spring verifies this
                user.isEnabled(),                 // account enabled?
                true,                             // account non-expired
                true,                             // credentials non-expired
                true,                             // account non-locked
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}

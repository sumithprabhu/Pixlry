package com.imageplatform.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SERVICE: auth-service
 * PURPOSE: Central Spring Security configuration — defines what's protected,
 *          what's public, and how authentication works.
 *
 * KEY DECISIONS:
 *
 * 1. CSRF disabled — CSRF attacks exploit browser cookie behaviour. We don't use
 *    cookies; we use Authorization headers with JWT. CSRF is irrelevant here.
 *    Enabling it would just break our API clients with 403s for no security benefit.
 *
 * 2. STATELESS session — the server never creates an HttpSession. Every request
 *    must carry its own identity via JWT. This is what makes horizontal scaling
 *    possible — any server instance can handle any request.
 *
 * 3. JwtAuthenticationFilter placed BEFORE UsernamePasswordAuthenticationFilter —
 *    because we want our JWT filter to populate the SecurityContext first. If we
 *    placed it after, the UsernamePasswordAuthenticationFilter would not find an
 *    authenticated principal and might interfere with the flow.
 *
 * 4. DaoAuthenticationProvider — wires together our UserDetailsService (loads user
 *    from DB) and PasswordEncoder (verifies BCrypt hash). The AuthenticationManager
 *    delegates to this provider during login.
 *
 * INTERVIEW Q: What is AuthenticationManager vs AuthenticationProvider?
 *   AuthenticationManager is the entry point — it accepts an Authentication object
 *   (e.g., username+password) and returns a fully populated one if valid.
 *   AuthenticationProvider is the actual implementation that knows HOW to verify
 *   credentials for a specific auth type (DAO, LDAP, OAuth2, etc.).
 *   AuthenticationManager delegates to a list of providers.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public routes — no token needed.
                        // /api/auth/** covers /login, /register, /refresh.
                        // These routes produce tokens, so they can't carry one.
                        .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                // Insert our JWT filter before Spring's default form-login filter.
                // If the JWT is valid, SecurityContext is populated before
                // AuthorizationFilter runs its permit/deny check.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider())
                .build();
    }

    /**
     * DaoAuthenticationProvider: used by AuthenticationManager during login.
     * It calls userDetailsService.loadUserByUsername() then uses passwordEncoder
     * to verify the raw password against the stored BCrypt hash.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposed as a bean so AuthService can inject it and call authenticate()
     * directly — the production-standard way to verify credentials in a service
     * rather than doing the password check manually.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with default strength (10 rounds).
        // Strength 10 means 2^10 = 1024 iterations — slow enough to resist brute-force,
        // fast enough for ~100ms per hash on modern hardware.
        return new BCryptPasswordEncoder();
    }
}

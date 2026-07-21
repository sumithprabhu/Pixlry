package com.imageplatform.auth.service;

import com.imageplatform.auth.dto.AuthResponse;
import com.imageplatform.auth.dto.LoginRequest;
import com.imageplatform.auth.dto.RefreshTokenRequest;
import com.imageplatform.auth.dto.RegisterRequest;
import com.imageplatform.auth.entity.RefreshToken;
import com.imageplatform.auth.entity.Role;
import com.imageplatform.auth.entity.User;
import com.imageplatform.auth.repository.RefreshTokenRepository;
import com.imageplatform.auth.repository.UserRepository;
import com.imageplatform.auth.security.JwtService;
import com.imageplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * SERVICE: auth-service
 * PURPOSE: Core authentication business logic — register, login, refresh, logout.
 *
 * KEY PATTERNS:
 *
 * 1. AuthenticationManager for login (not manual password check):
 *    We delegate credential verification to Spring Security's AuthenticationManager
 *    rather than calling passwordEncoder.matches() ourselves. Why?
 *    - It's the standard contract — plugging in 2FA, account locking, or LDAP later
 *      only requires a new AuthenticationProvider, not changes here.
 *    - It triggers all security events (bad credentials, account locked) automatically.
 *    - It keeps auth logic in one place.
 *
 * 2. Refresh token rotation:
 *    Every time a refresh token is used, we:
 *    a. Revoke the old one (it can never be used again).
 *    b. Issue a brand new refresh token.
 *    This is called "refresh token rotation". If an attacker steals a refresh token
 *    and uses it first, the legitimate user's next refresh attempt will fail
 *    (old token is already revoked), alerting the system to a potential compromise.
 *
 * 3. @Transactional on register/refresh:
 *    Multiple DB writes happen (save user + save refresh token). If either fails,
 *    the whole operation rolls back. Without @Transactional, you could end up with
 *    a user row but no refresh token, leaving the system in an inconsistent state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .build();

        userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            // AuthenticationManager calls CustomUserDetailsService.loadUserByUsername(email)
            // then verifies the BCrypt hash. Throws BadCredentialsException on failure.
            // We pass email as principal but CustomUserDetailsService maps email → userId.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            // Always return the same message for wrong email OR wrong password.
            // Specific messages ("email not found" vs "wrong password") help attackers
            // enumerate valid accounts — this is called user enumeration.
            throw new BusinessException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (stored.isRevoked()) {
            // A revoked token being used is a strong signal of token theft.
            // Revoke ALL tokens for this user as a defensive measure.
            refreshTokenRepository.revokeAllUserTokens(stored.getUser());
            log.warn("Revoked refresh token reused for userId={}. All tokens invalidated.", stored.getUser().getId());
            throw new BusinessException("Refresh token has been revoked", HttpStatus.UNAUTHORIZED);
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Refresh token has expired. Please log in again.", HttpStatus.UNAUTHORIZED);
        }

        // Token rotation: revoke the used token, issue a fresh one.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildAuthResponse(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByToken(rawRefreshToken)
                .ifPresent(token -> {
                    refreshTokenRepository.revokeAllUserTokens(token.getUser());
                    log.info("User logged out, all tokens revoked for userId={}", token.getUser().getId());
                });
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String rawRefreshToken = UUID.randomUUID().toString(); // opaque random token, not a JWT

        // Revoke existing tokens before saving the new one — one active refresh token per user.
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(rawRefreshToken)
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiryMs))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900000) // 15 minutes in ms — matches jwt.access-token-expiry-ms
                .build();
    }
}

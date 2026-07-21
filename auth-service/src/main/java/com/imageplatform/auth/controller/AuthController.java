package com.imageplatform.auth.controller;

import com.imageplatform.auth.dto.AuthResponse;
import com.imageplatform.auth.dto.LoginRequest;
import com.imageplatform.auth.dto.RefreshTokenRequest;
import com.imageplatform.auth.dto.RegisterRequest;
import com.imageplatform.auth.service.AuthService;
import com.imageplatform.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * SERVICE: auth-service
 * PURPOSE: REST API surface for all authentication operations.
 *
 * ROUTE SUMMARY:
 *   POST /api/auth/register  — create account, returns access + refresh token
 *   POST /api/auth/login     — verify credentials, returns access + refresh token
 *   POST /api/auth/refresh   — exchange refresh token for a new token pair
 *   POST /api/auth/logout    — revoke refresh token server-side
 *
 * All routes are under /api/auth/** which is PERMITTED in SecurityConfig — no JWT needed.
 * These routes produce tokens, so they cannot require one to access them.
 *
 * INTERVIEW Q: Why return tokens in response body and not as cookies?
 *   Cookies are automatic — the browser sends them on every request, making CSRF
 *   attacks possible. Authorization header tokens require explicit JS code to attach,
 *   so CSRF is not a threat. The trade-off: cookies can be HttpOnly (JS can't read them),
 *   but body tokens are JS-readable (XSS risk). For REST APIs, body tokens are standard.
 *   Browser SPAs should store tokens in memory, not localStorage.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }
}

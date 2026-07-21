package com.imageplatform.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * SERVICE: auth-service
 * PURPOSE: Intercepts every incoming HTTP request and checks for a valid JWT.
 *
 * WHY OncePerRequestFilter?
 *   In Servlet containers, a single logical request can trigger multiple filter-chain
 *   passes (e.g., via forward/include dispatches). OncePerRequestFilter guarantees
 *   this filter runs exactly once per request regardless of dispatch type.
 *
 * WHAT THIS FILTER DOES:
 *   1. Reads the "Authorization: Bearer <token>" header.
 *   2. Validates the JWT signature and expiry via JwtService.
 *   3. Extracts userId and role from the token claims.
 *   4. Puts a UsernamePasswordAuthenticationToken into the SecurityContextHolder
 *      so downstream filters (AuthorizationFilter) know the request is authenticated.
 *
 * WHAT THIS FILTER DOES NOT DO:
 *   - It does NOT hit the database. The JWT is self-contained (userId + role are
 *     embedded as claims). Making a DB call on every request defeats the purpose
 *     of JWT. Token revocation is handled via short expiry (15 min) + refresh tokens.
 *   - It does NOT return 401 on missing/invalid token. If there's no token, it simply
 *     does nothing and calls filterChain.doFilter(). The AuthorizationFilter at the
 *     end of the chain will return 401 for protected routes. This way, public routes
 *     like /api/auth/login work fine without a token.
 *
 * INTERVIEW Q: Why not put JWT validation in a controller or service?
 *   Because we need it to run before the request reaches any controller — including
 *   before the AuthorizationFilter checks permissions. The filter chain is the only
 *   place guaranteed to run before controller dispatch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No token present — let the chain continue.
        // Public routes (/api/auth/**) will be allowed by SecurityConfig.permitAll().
        // Protected routes will be denied by AuthorizationFilter.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7); // strip "Bearer "

        if (!jwtService.isTokenValid(token)) {
            log.warn("Invalid or expired JWT from {}", request.getRemoteAddr());
            filterChain.doFilter(request, response);
            return;
        }

        // Token is valid — extract identity from claims and populate SecurityContext.
        // From this point, any downstream filter or controller can call
        // SecurityContextHolder.getContext().getAuthentication() to get the user.
        Claims claims = jwtService.extractAllClaims(token);
        String userId = claims.getSubject();         // we set userId as the JWT subject
        String role   = claims.get("role", String.class);

        // UsernamePasswordAuthenticationToken(principal, credentials, authorities)
        // credentials=null because we never need the password after initial auth.
        // "ROLE_" prefix is required by Spring Security's hasRole() checks.
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        // Attach request metadata (IP, session) to the authentication object.
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Authenticated userId={} role={}", userId, role);

        filterChain.doFilter(request, response);
    }
}

package com.imageplatform.gateway.filter;

import com.imageplatform.gateway.security.GatewayJwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * SERVICE: api-gateway
 * PURPOSE: Global JWT validation filter — runs on every request before routing.
 *
 * HOW THIS DIFFERS FROM auth-service's JwtAuthenticationFilter:
 *   - auth-service filter: does NOTHING on missing/invalid token (lets AuthorizationFilter decide)
 *   - gateway filter: actively REJECTS (401) missing/invalid tokens for protected routes
 *
 * WHY THIS DESIGN?
 *   The gateway is the only entry point exposed to the internet. It's the last
 *   chance to stop unauthenticated traffic. Downstream services (job-service,
 *   worker-service, etc.) have no security config at all — they trust that the
 *   gateway already validated the request.
 *
 * WHAT THIS ADDS TO FORWARDED REQUESTS:
 *   - X-User-Id header: the UUID of the authenticated user (extracted from JWT subject)
 *   - X-User-Role header: the user's role (USER or ADMIN)
 *   Downstream services read these headers instead of decoding the JWT themselves.
 *   This is the "token relay" pattern — validate once at the edge, propagate identity.
 *
 * WHY GlobalFilter + Ordered?
 *   GlobalFilter runs on all routes. Ordered ensures it runs BEFORE Spring Cloud
 *   Gateway's built-in route filters (order = -1 means highest priority).
 *
 * REACTIVE NOTE:
 *   This is WebFlux code — no blocking I/O. The filter returns Mono<Void> (a reactive
 *   stream) instead of calling filterChain.doFilter() imperatively. All Spring Cloud
 *   Gateway filters must be reactive.
 *
 * INTERVIEW Q: How do you propagate user identity from gateway to microservices?
 *   Validate the JWT at the gateway, extract the user ID and role from claims,
 *   then add them as trusted internal headers (X-User-Id, X-User-Role) on the
 *   forwarded request. Downstream services read these headers directly — no DB call,
 *   no JWT re-validation. The headers are trusted because they can only be set by
 *   the gateway (not by external clients — the gateway strips them on inbound requests).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final GatewayJwtService jwtService;

    // Routes that do not require a JWT token.
    // /api/auth/** = login, register, refresh (they produce the token, can't carry one)
    // /ws/**      = WebSocket handshake (authenticated separately via connection params)
    private static final List<String> PUBLIC_PATHS = List.of("/api/auth/", "/ws/", "/actuator/");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing Authorization header for path: {}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Authorization header required");
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            log.warn("Invalid or expired JWT for path: {}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        Claims claims = jwtService.extractAllClaims(token);
        String userId = claims.getSubject();
        String role   = claims.get("role", String.class);

        // Strip any incoming X-User-* headers from external requests (security measure —
        // prevents clients from spoofing their identity by sending these headers directly).
        // Then re-add them with values we extracted and trust from the validated JWT.
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                })
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .build();

        log.debug("Authenticated request — userId={} role={} path={}", userId, role, path);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -1; // run before all other gateway filters
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String reason) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("X-Rejection-Reason", reason);
        return exchange.getResponse().setComplete();
    }
}

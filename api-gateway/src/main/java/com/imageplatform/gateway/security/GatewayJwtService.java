package com.imageplatform.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * SERVICE: api-gateway
 * PURPOSE: Validate JWTs at the gateway edge — before requests reach any downstream service.
 *
 * WHY DUPLICATE JWT LOGIC HERE (not share from common)?
 *   The gateway runs on WebFlux/Netty (reactive stack). The auth-service's JwtService
 *   is a Spring @Service with @Value injection, which works in both stacks. However,
 *   the gateway does NOT need or have a database — it just needs to verify the token
 *   signature and extract claims. Keeping this small and gateway-local avoids coupling
 *   common to JJWT (a third-party lib that would then propagate to every service).
 *
 * CRITICAL: The jwt.secret MUST be identical to the one in auth-service.
 *   JWT is verified with the same HMAC-SHA256 key that signed it. If secrets differ,
 *   every token will fail validation. In Docker Compose this is enforced via shared
 *   JWT_SECRET environment variable.
 */
@Slf4j
@Component
public class GatewayJwtService {

    @Value("${jwt.secret}")
    private String secret;

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

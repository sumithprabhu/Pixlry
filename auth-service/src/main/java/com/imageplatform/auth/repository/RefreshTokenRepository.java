package com.imageplatform.auth.repository;

import com.imageplatform.auth.entity.RefreshToken;
import com.imageplatform.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * SERVICE: auth-service
 * PURPOSE: Persistence for refresh tokens.
 *
 * WHY STORE REFRESH TOKENS IN DB?
 *   Unlike access tokens (short-lived, ~15 min, we let them expire naturally),
 *   refresh tokens are long-lived (~7 days). If a user's device is stolen, we need
 *   to be able to revoke their refresh token before it expires. The only way to do
 *   that is to store them server-side and check validity on use.
 *   Access tokens cannot be revoked early — this is a known JWT trade-off.
 *   That's why access tokens are kept short-lived.
 *
 * INTERVIEW Q: Can you revoke a JWT?
 *   Not directly — a JWT is stateless and self-contained. You can:
 *   1. Keep access tokens short-lived (15 min) so the damage window is small.
 *   2. Store refresh tokens in a DB/Redis and revoke them there (what we do here).
 *   3. Maintain a token blocklist in Redis (adds latency on every request).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    // Bulk revoke: called on logout or when issuing a new refresh token.
    // @Modifying + @Query is more efficient than loading all tokens into memory.
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user AND r.revoked = false")
    void revokeAllUserTokens(User user);
}

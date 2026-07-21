package com.imageplatform.notification.repository;

import com.imageplatform.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * SERVICE: notification-service
 * PURPOSE: Persistence for notification history — so users can see past alerts even
 *          after they've dismissed the WebSocket push or reconnected later.
 *
 * WHY STORE NOTIFICATIONS IN A DB?
 *   WebSocket is ephemeral — if the user's browser disconnects and reconnects,
 *   they missed any pushes that happened while they were offline. The DB is the
 *   persistent record of what happened to their jobs.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Newest first — user sees most recent job updates at the top
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadFalse(UUID userId);

    // Bulk-mark-as-read is more efficient than loading each entity and saving one-by-one
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    void markAllAsReadForUser(UUID userId);
}

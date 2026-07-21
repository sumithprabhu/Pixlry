package com.imageplatform.notification.controller;

import com.imageplatform.common.dto.ApiResponse;
import com.imageplatform.notification.entity.Notification;
import com.imageplatform.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SERVICE: notification-service
 * PURPOSE: REST API for notification history — used when a user opens the notification
 *          panel in the Angular dashboard to see past job updates.
 *
 * HOW THIS WORKS WITH WEBSOCKET:
 *   Real-time updates come via WebSocket push (no polling needed).
 *   These REST endpoints serve the notification HISTORY:
 *   - On page load: Angular calls GET /api/notifications to populate the panel
 *   - Unread badge: Angular calls GET /api/notifications/unread-count on login
 *   - After reading: Angular calls POST /api/notifications/read-all
 *
 * X-User-Id header is injected by the API Gateway after JWT validation.
 * This service trusts it without any further auth checks.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getAll(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getNotificationsForUser(userId)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(
            @RequestHeader("X-User-Id") UUID userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @RequestHeader("X-User-Id") UUID userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", null));
    }
}

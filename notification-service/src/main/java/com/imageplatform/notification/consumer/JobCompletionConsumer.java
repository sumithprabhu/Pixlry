package com.imageplatform.notification.consumer;

import com.imageplatform.common.constants.JobStatus;
import com.imageplatform.common.event.JobCompletedEvent;
import com.imageplatform.notification.entity.Notification;
import com.imageplatform.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * SERVICE: notification-service
 * PURPOSE: React to job completion events by doing two things atomically:
 *   1. Persist the notification to DB (so user sees it even after reconnecting)
 *   2. Push a real-time update via WebSocket (instant feedback in the browser)
 *
 * WHY BOTH PERSIST AND PUSH?
 *   WebSocket push is ephemeral — if the user's tab is closed, they miss it.
 *   DB persistence ensures they see the notification history on next login.
 *   Both together give the best UX: instant AND durable.
 *
 * WebSocket push mechanics:
 *   convertAndSendToUser(userId, "/queue/job-updates", payload) sends to the
 *   destination "/user/{userId}/queue/job-updates". Only the browser session
 *   subscribed with that userId receives the message — other users don't see it.
 *   The STOMP broker handles the per-user routing internally.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobCompletionConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @RabbitListener(queues = "job.completed.notifications")
    public void consume(JobCompletedEvent event) {
        log.info("Notification event received: jobId={} userId={} status={}",
                event.getJobId(), event.getUserId(), event.getStatus());

        String message = buildMessage(event);

        // 1. Persist to DB
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .jobId(event.getJobId())
                .status(event.getStatus().name())
                .message(message)
                .read(false)
                .build();
        notificationService.save(notification);

        // 2. Push to the specific user's WebSocket channel
        // Angular subscribes to "/user/queue/job-updates" and receives this instantly
        messagingTemplate.convertAndSendToUser(
                event.getUserId().toString(),
                "/queue/job-updates",
                event
        );

        log.info("Pushed WebSocket notification to userId={}", event.getUserId());
    }

    private String buildMessage(JobCompletedEvent event) {
        if (event.getStatus() == JobStatus.COMPLETED) {
            return String.format("Your image job completed in %dms", event.getProcessingTimeMs());
        }
        return "Your image job failed: " + event.getErrorMessage();
    }
}

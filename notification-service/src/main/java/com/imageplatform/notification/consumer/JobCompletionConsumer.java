package com.imageplatform.notification.consumer;

import com.imageplatform.common.event.JobCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobCompletionConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "job.completed")
    public void consume(JobCompletedEvent event) {
        log.info("Pushing notification for job: {} to user: {}", event.getJobId(), event.getUserId());

        // Push to per-user private channel — only this user's browser will receive it
        messagingTemplate.convertAndSendToUser(
                event.getUserId().toString(),
                "/queue/job-updates",
                event
        );
    }
}

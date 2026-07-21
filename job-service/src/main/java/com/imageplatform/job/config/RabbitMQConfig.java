package com.imageplatform.job.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SERVICE: job-service
 * PURPOSE: Declares all RabbitMQ resources this service uses — exchange, queues, bindings.
 *
 * FAN-OUT PATTERN VIA TOPIC EXCHANGE:
 *   When a worker completes a job, it publishes ONE event with routing key "job.completed".
 *   Three separate services all need to react to this:
 *     - job-service   → update job status in DB
 *     - notification  → push WebSocket update to user
 *     - analytics     → increment processed job counter
 *
 *   If all three listened to a single "job.completed" queue, RabbitMQ would round-robin
 *   between them (competing consumers) — each event would go to ONLY ONE consumer.
 *   That's not what we want. We want ALL THREE to get EVERY event.
 *
 *   Solution: THREE separate queues, all bound with the same routing key.
 *   Each service declares and owns its own queue.
 *   The topic exchange copies each published message to ALL matching queues.
 *
 *   Published by worker:  image.jobs exchange  ──► routing key: "job.completed"
 *                                                     │
 *                    ┌────────────────────────────────┤
 *                    │                                │                  │
 *           job.completed.status         job.completed.notifications  job.completed.analytics
 *                    │                                │                  │
 *              job-service                  notification-service   analytics-service
 *
 * DEAD LETTER QUEUE (DLQ) on job.created:
 *   If a worker crashes or throws an unrecoverable exception while processing a message,
 *   the message would normally get re-queued and retried indefinitely (poison message loop).
 *   DLX routes failed messages to a dead-letter exchange instead.
 *   We can then inspect or replay them manually. The DLX is declared by convention here
 *   but the actual DLX queue/exchange is created separately (or by ops tooling).
 *
 * IDEMPOTENCY NOTE:
 *   All queue/exchange declarations are durable (survive broker restart) and idempotent —
 *   if the resource already exists with the same properties, RabbitMQ ignores the declaration.
 *   This is safe to run on every app startup.
 *
 * INTERVIEW Q: How do you fan-out a message to multiple consumers in RabbitMQ?
 *   Use a topic exchange with multiple queues bound to the same routing key pattern.
 *   Each consumer has its own queue — messages are copied to all matching queues
 *   so every consumer gets every message independently.
 */
@Configuration
public class RabbitMQConfig {

    // Exchange name shared by all services
    public static final String JOB_EXCHANGE = "image.jobs";

    // Queue this service produces to (worker consumes)
    public static final String JOB_CREATED_QUEUE = "job.created";

    // Queue this service consumes from (worker produces, we update job status)
    public static final String JOB_COMPLETED_STATUS_QUEUE = "job.completed.status";

    public static final String JOB_CREATED_KEY   = "job.created";
    public static final String JOB_COMPLETED_KEY = "job.completed";

    @Bean
    public TopicExchange jobExchange() {
        return new TopicExchange(JOB_EXCHANGE, true, false);
    }

    @Bean
    public Queue jobCreatedQueue() {
        // x-dead-letter-exchange: if a message is rejected/expired, route it to the DLX
        // so it's not silently dropped and can be inspected later.
        return QueueBuilder.durable(JOB_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", JOB_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Queue jobCompletedStatusQueue() {
        return QueueBuilder.durable(JOB_COMPLETED_STATUS_QUEUE).build();
    }

    @Bean
    public Binding jobCreatedBinding() {
        return BindingBuilder.bind(jobCreatedQueue()).to(jobExchange()).with(JOB_CREATED_KEY);
    }

    @Bean
    public Binding jobCompletedStatusBinding() {
        return BindingBuilder.bind(jobCompletedStatusQueue()).to(jobExchange()).with(JOB_COMPLETED_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(messageConverter());
        return template;
    }
}

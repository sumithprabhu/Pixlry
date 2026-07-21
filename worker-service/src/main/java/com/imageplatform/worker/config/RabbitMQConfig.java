package com.imageplatform.worker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SERVICE: worker-service
 * PURPOSE: Declare the queue this worker consumes from, and set up JSON message conversion.
 *
 * WHAT THE WORKER OWNS IN RABBITMQ:
 *   - Consumes from: job.created (declared here)
 *   - Publishes to:  image.jobs exchange with routing key "job.completed"
 *                    (the exchange is declared by job-service, but declaring it here
 *                    too is safe — RabbitMQ ignores duplicate declarations with same properties)
 *
 * WHY DECLARE THE EXCHANGE HERE TOO?
 *   The worker publishes completion events. If it starts before job-service
 *   (which declares the exchange), the publish would fail with "exchange not found".
 *   Declaring the exchange in every service that uses it makes each service self-sufficient
 *   regardless of startup order. Duplicate declarations are idempotent in RabbitMQ.
 */
@Configuration
public class RabbitMQConfig {

    public static final String JOB_EXCHANGE       = "image.jobs";
    public static final String JOB_CREATED_QUEUE  = "job.created";
    public static final String JOB_COMPLETED_KEY  = "job.completed";

    @Bean
    public TopicExchange jobExchange() {
        return new TopicExchange(JOB_EXCHANGE, true, false);
    }

    // Declare the queue so it exists even if job-service hasn't started yet.
    @Bean
    public Queue jobCreatedQueue() {
        return QueueBuilder.durable(JOB_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", JOB_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Binding jobCreatedBinding() {
        return BindingBuilder.bind(jobCreatedQueue()).to(jobExchange()).with(JOB_CREATED_QUEUE);
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

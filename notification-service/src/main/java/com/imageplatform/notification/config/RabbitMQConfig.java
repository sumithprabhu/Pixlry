package com.imageplatform.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SERVICE: notification-service
 * PURPOSE: Declare this service's queue in the fan-out pattern.
 *
 * This service owns "job.completed.notifications" — its private copy of every
 * job completion event. The exchange copies each "job.completed" message into
 * this queue independently of what job-service and analytics-service consume.
 *
 * See job-service RabbitMQConfig for the full fan-out pattern explanation.
 */
@Configuration
public class RabbitMQConfig {

    public static final String JOB_EXCHANGE                    = "image.jobs";
    public static final String JOB_COMPLETED_NOTIFICATIONS_QUEUE = "job.completed.notifications";
    public static final String JOB_COMPLETED_KEY               = "job.completed";

    @Bean
    public TopicExchange jobExchange() {
        return new TopicExchange(JOB_EXCHANGE, true, false);
    }

    @Bean
    public Queue jobCompletedNotificationsQueue() {
        return QueueBuilder.durable(JOB_COMPLETED_NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    public Binding jobCompletedNotificationsBinding() {
        return BindingBuilder
                .bind(jobCompletedNotificationsQueue())
                .to(jobExchange())
                .with(JOB_COMPLETED_KEY);
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

package com.imageplatform.analytics.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SERVICE: analytics-service
 * PURPOSE: Declare this service's queue in the fan-out pattern.
 *
 * The analytics service maintains its OWN view of job statistics, populated
 * from events rather than querying another service's database.
 * This is the "event-sourced analytics" pattern — analytics data is derived
 * from the same events that drive the rest of the system.
 */
@Configuration
public class RabbitMQConfig {

    public static final String JOB_EXCHANGE                  = "image.jobs";
    public static final String JOB_COMPLETED_ANALYTICS_QUEUE = "job.completed.analytics";
    public static final String JOB_COMPLETED_KEY             = "job.completed";

    @Bean
    public TopicExchange jobExchange() {
        return new TopicExchange(JOB_EXCHANGE, true, false);
    }

    @Bean
    public Queue jobCompletedAnalyticsQueue() {
        return QueueBuilder.durable(JOB_COMPLETED_ANALYTICS_QUEUE).build();
    }

    @Bean
    public Binding jobCompletedAnalyticsBinding() {
        return BindingBuilder
                .bind(jobCompletedAnalyticsQueue())
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

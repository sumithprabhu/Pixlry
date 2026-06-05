package com.imageplatform.job.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String JOB_EXCHANGE     = "image.jobs";
    public static final String JOB_CREATED_QUEUE   = "job.created";
    public static final String JOB_COMPLETED_QUEUE = "job.completed";
    public static final String JOB_CREATED_KEY  = "job.created";
    public static final String JOB_COMPLETED_KEY = "job.completed";

    @Bean
    public TopicExchange jobExchange() {
        return new TopicExchange(JOB_EXCHANGE, true, false);
    }

    @Bean
    public Queue jobCreatedQueue() {
        return QueueBuilder.durable(JOB_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", JOB_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Queue jobCompletedQueue() {
        return QueueBuilder.durable(JOB_COMPLETED_QUEUE).build();
    }

    @Bean
    public Binding jobCreatedBinding() {
        return BindingBuilder.bind(jobCreatedQueue()).to(jobExchange()).with(JOB_CREATED_KEY);
    }

    @Bean
    public Binding jobCompletedBinding() {
        return BindingBuilder.bind(jobCompletedQueue()).to(jobExchange()).with(JOB_COMPLETED_KEY);
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

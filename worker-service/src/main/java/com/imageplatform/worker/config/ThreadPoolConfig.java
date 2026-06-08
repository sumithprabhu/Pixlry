package com.imageplatform.worker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    @Value("${worker.thread-pool-size:10}")
    private int threadPoolSize;

    @Bean
    public ExecutorService imageProcessingExecutor() {
        return Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "img-worker-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }
}

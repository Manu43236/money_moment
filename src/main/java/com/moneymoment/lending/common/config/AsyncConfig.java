package com.moneymoment.lending.common.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "eodExecutor")
    public Executor eodExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("EOD-");
        executor.setRejectedExecutionHandler((r, ex) -> {
            throw new RuntimeException("EOD is already queued or running");
        });
        executor.initialize();
        return executor;
    }
}

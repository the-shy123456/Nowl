package com.unimarket.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class SearchAsyncConfig {

    @Bean(name = "searchTrackingExecutor")
    public Executor searchTrackingExecutor(
        @Value("${unimarket.search.tracking.async.core-pool-size:2}") int corePoolSize,
        @Value("${unimarket.search.tracking.async.max-pool-size:4}") int maxPoolSize,
        @Value("${unimarket.search.tracking.async.queue-capacity:5000}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("search-tracking-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(Math.max(corePoolSize, maxPoolSize));
        executor.setQueueCapacity(queueCapacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}


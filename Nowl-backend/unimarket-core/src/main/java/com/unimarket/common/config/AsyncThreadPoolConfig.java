package com.unimarket.common.config;


import com.unimarket.security.UserContextTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncThreadPoolConfig {

    @Bean("AiTaskAsyncExecutor") // 自定义线程池名称
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 核心线程数（常驻线程）
        executor.setMaxPoolSize(10); // 最大线程数（核心线程忙且队列满时，最多创建的线程数）
        executor.setQueueCapacity(20); // 任务队列容量（核心线程忙时，任务先入队列）
        executor.setThreadNamePrefix("Ai-async-"); // 线程名前缀（便于排查问题）
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略（队列满+最大线程数满时，由调用方线程执行）
        executor.setAllowCoreThreadTimeOut(true);
        
        // 设置TaskDecorator,用于传递ThreadLocal用户上下文到异步线程
        executor.setTaskDecorator(new UserContextTaskDecorator());
        
        executor.initialize(); // 初始化线程池
        return executor;
    }
}

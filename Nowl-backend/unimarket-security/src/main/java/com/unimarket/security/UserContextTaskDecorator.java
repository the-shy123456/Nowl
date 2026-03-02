package com.unimarket.security;

import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

/**
 * 用户上下文任务装饰器
 * 用于在异步任务执行时传递ThreadLocal中的用户上下文
 */
public class UserContextTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        // 在主线程中获取当前用户上下文
        UserContext context = UserContextHolder.getContext();
        
        // 返回一个新的Runnable,在异步线程中设置用户上下文
        return () -> {
            try {
                // 在异步线程中设置用户上下文
                UserContextHolder.setContext(context);
                // 执行实际任务
                runnable.run();
            } finally {
                // 清理异步线程的ThreadLocal
                UserContextHolder.clear();
            }
        };
    }
}

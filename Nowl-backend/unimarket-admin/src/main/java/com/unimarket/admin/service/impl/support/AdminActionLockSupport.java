package com.unimarket.admin.service.impl.support;

import com.unimarket.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 管理端关键写操作分布式锁封装，避免并发重复处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminActionLockSupport {

    private static final long LOCK_WAIT_SECONDS = 3L;
    private static final long LOCK_LEASE_SECONDS = 10L;

    private final RedissonClient redissonClient;

    public void withLock(String lockKey, Runnable action) {
        withLock(lockKey, () -> {
            action.run();
            return null;
        });
    }

    public <T> T withLock(String lockKey, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("管理端操作获取锁被中断: lockKey={}", lockKey);
            throw new BusinessException("系统繁忙，请稍后重试");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}


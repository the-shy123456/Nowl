package com.unimarket.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类
 */
@Component
@RequiredArgsConstructor
public class RedisCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Random random = new Random();

    /**
     * 设置缓存（永久）
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 设置缓存（带过期时间，单位：秒）
     */
    public void set(String key, Object value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取缓存（指定类型）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : (T) value;
    }

    /**
     * 设置List缓存
     */
    public <T> void setCacheList(String key, List<T> dataList) {
        redisTemplate.opsForValue().set(key, dataList);
    }

    /**
     * 设置List缓存（带过期时间）
     */
    public <T> void setCacheList(String key, List<T> dataList, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, dataList, timeout, unit);
    }

    /**
     * 获取List缓存
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getCacheList(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : (List<T>) value;
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 判断key是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 设置过期时间（单位：秒）
     */
    public Boolean expire(String key, long seconds) {
        return redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    /**
     * 获取过期时间（单位：秒）
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 自增
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 自增（指定步长）
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 自减
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 设置带随机过期时间的缓存（防止雪崩）
     * @param seconds 基础过期时间（秒）
     * @param jitterPercent 随机抖动百分比 (0-100)
     */
    public void setWithJitter(String key, Object value, long seconds, int jitterPercent) {
        long jitter = (long) (seconds * (random.nextInt(jitterPercent + 1) / 100.0));
        long finalTimeout = seconds + (random.nextBoolean() ? jitter : -jitter);
        this.set(key, value, Math.max(1, finalTimeout), TimeUnit.SECONDS);
    }

    /**
     * 自减（指定步长）
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }
}
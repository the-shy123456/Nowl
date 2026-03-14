package com.unimarket.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 搜索埋点（历史/热搜）异步服务。
 *
 * <p>注意：当线程池/队列打满时会丢弃埋点任务，避免影响搜索接口响应时间。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchTrackingAsyncService {

    private final SearchService searchService;

    @Async("searchTrackingExecutor")
    public void recordSearchHistory(Long userId, String keyword) {
        try {
            searchService.recordSearchHistory(userId, keyword);
        } catch (Exception e) {
            log.debug("recordSearchHistory async failed, userId={}, keyword={}", userId, keyword, e);
        }
    }

    @Async("searchTrackingExecutor")
    public void incrementHotWord(String keyword, String schoolCode) {
        try {
            searchService.incrementHotWord(keyword, schoolCode);
        } catch (Exception e) {
            log.debug("incrementHotWord async failed, keyword={}, schoolCode={}", keyword, schoolCode, e);
        }
    }
}


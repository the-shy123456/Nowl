package com.unimarket.module.notice.service;

import com.unimarket.module.notice.vo.NoticeVO;
import java.util.List;

public interface NoticeService {
    void sendNotice(Long userId, String title, String content, Integer type);
    void sendNotice(Long userId, String title, String content, Integer type, Long relatedId);
    List<NoticeVO> getMyNotices(Long userId);
    void markAsRead(Long userId, Long noticeId);
    void markAllAsRead(Long userId);
    long getUnreadCount(Long userId);
}

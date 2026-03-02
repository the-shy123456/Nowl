package com.unimarket.module.chat.service;

import com.unimarket.module.chat.dto.ChatMessageDTO;
import com.unimarket.module.chat.vo.ChatBlockItemVO;
import com.unimarket.module.chat.vo.ChatMessageVO;
import com.unimarket.module.chat.vo.ContactVO;

import java.util.List;

public interface ChatService {

    /**
     * 发送消息
     */
    void sendMessage(Long senderId, ChatMessageDTO dto);

    /**
     * 获取聊天记录
     */
    List<ChatMessageVO> getHistory(Long userId, Long contactId);

    /**
     * 标记消息为已读
     */
    void markAsRead(Long userId, Long contactId);

    /**
     * 标记当前用户全部私信为已读
     */
    void markAllAsRead(Long userId);

    /**
     * 获取最近联系人列表
     */
    List<ContactVO> getRecentContacts(Long userId);

    /**
     * 拉黑用户
     */
    void blockUser(Long userId, Long targetUserId);

    /**
     * 解除拉黑
     */
    void unblockUser(Long userId, Long targetUserId);

    /**
     * 获取拉黑列表
     */
    List<ChatBlockItemVO> getBlockList(Long userId, String keyword);

    /**
     * 查询双方拉黑状态
     */
    int getBlockRelation(Long userId, Long targetUserId);
}

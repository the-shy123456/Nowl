package com.unimarket.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.chat.dto.ChatMessageDTO;
import com.unimarket.module.chat.entity.ChatBlockRecord;
import com.unimarket.module.chat.entity.ChatMessage;
import com.unimarket.module.chat.mapper.ChatBlockRecordMapper;
import com.unimarket.module.chat.mapper.ChatMessageMapper;
import com.unimarket.module.chat.service.ChatService;
import com.unimarket.module.chat.vo.ChatBlockItemVO;
import com.unimarket.module.chat.vo.ChatMessageVO;
import com.unimarket.module.chat.vo.ContactVO;
import com.unimarket.module.risk.dto.RiskContext;
import com.unimarket.module.risk.enums.RiskAction;
import com.unimarket.module.risk.enums.RiskEventType;
import com.unimarket.module.risk.service.RiskControlService;
import com.unimarket.module.risk.vo.RiskDecisionResult;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatBlockRecordMapper chatBlockRecordMapper;
    private final UserInfoMapper userInfoMapper;
    private final RiskControlService riskControlService;

    @Override
    public void sendMessage(Long senderId, ChatMessageDTO dto) {
        Long receiverId = dto.getReceiverId();
        if (isBlocked(senderId, receiverId)) {
            throw new BusinessException("对方已将你拉黑，消息发送失败");
        }
        if (isBlocked(receiverId, senderId)) {
            throw new BusinessException("你已拉黑对方，请先解除拉黑");
        }

        UserInfo sender = userInfoMapper.selectById(senderId);
        if (sender == null) {
            throw new BusinessException("发送者不存在");
        }

        Map<String, Object> features = new HashMap<>();
        features.put("receiverId", receiverId);
        features.put("messageType", dto.getType());
        features.put("contentLength", dto.getContent() == null ? 0 : dto.getContent().length());

        Map<String, Object> rawPayload = new HashMap<>();
        rawPayload.put("senderId", senderId);
        rawPayload.put("receiverId", receiverId);
        rawPayload.put("content", dto.getContent());
        rawPayload.put("messageType", dto.getType());

        RiskDecisionResult decision = riskControlService.evaluate(RiskContext.builder()
                .eventType(RiskEventType.CHAT_SEND)
                .userId(senderId)
                .subjectId(String.valueOf(senderId))
                .schoolCode(sender.getSchoolCode())
                .campusCode(sender.getCampusCode())
                .features(features)
                .rawPayload(rawPayload)
                .build());

        if (decision.getAction() != RiskAction.ALLOW) {
            throw new BusinessException("当前聊天行为触发风控策略，请稍后再试");
        }

        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setSchoolCode(sender.getSchoolCode());
        message.setCampusCode(sender.getCampusCode());
        message.setContent(dto.getContent());
        message.setMessageType(dto.getType() != null ? dto.getType() : 0); // 默认文本
        message.setIsRead(0);
        message.setRiskLevel(decision.getRiskLevel());
        
        chatMessageMapper.insert(message);
    }

    @Override
    public List<ChatMessageVO> getHistory(Long userId, Long contactId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getReceiverId, contactId))
                .or(w -> w.eq(ChatMessage::getSenderId, contactId).eq(ChatMessage::getReceiverId, userId))
                .orderByAsc(ChatMessage::getCreateTime);
        return chatMessageMapper.selectList(wrapper).stream()
                .map(this::toChatMessageVO)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long userId, Long contactId) {
        LambdaUpdateWrapper<ChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatMessage::getSenderId, contactId)
                .eq(ChatMessage::getReceiverId, userId)
                .eq(ChatMessage::getIsRead, 0)
                .set(ChatMessage::getIsRead, 1);
        chatMessageMapper.update(null, wrapper);
    }

    @Override
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<ChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatMessage::getReceiverId, userId)
                .eq(ChatMessage::getIsRead, 0)
                .set(ChatMessage::getIsRead, 1);
        chatMessageMapper.update(null, wrapper);
    }

    @Override
    public List<ContactVO> getRecentContacts(Long userId) {
        // 1. 直接从数据库拉取各联系人的最新一条消息
        List<ChatMessage> recentMessages = chatMessageMapper.selectRecentMessages(userId);
        if (recentMessages.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 批量查询未读数
        List<Map<String, Object>> unreadStats = chatMessageMapper.selectUnreadCounts(userId);
        Map<Long, Integer> unreadMap = unreadStats.stream().collect(Collectors.toMap(
                m -> ((Number) m.get("contactId")).longValue(),
                m -> ((Number) m.get("count")).intValue()
        ));

        // 3. 收集联系人ID并批量查询用户信息
        List<Long> contactIds = recentMessages.stream()
                .map(msg -> msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId())
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, UserInfo> userMap = userInfoMapper.selectBatchIds(contactIds).stream()
                .collect(Collectors.toMap(UserInfo::getUserId, u -> u));

        // 4. 组装VO
        List<ContactVO> result = new ArrayList<>();
        for (ChatMessage msg : recentMessages) {
            Long contactId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            UserInfo contactUser = userMap.get(contactId);

            if (contactUser != null) {
                ContactVO vo = new ContactVO();
                vo.setUserId(contactId);
                vo.setNickName(contactUser.getNickName());
                vo.setAvatar(contactUser.getImageUrl());
                vo.setLastMessage(msg.getContent());
                vo.setLastTime(msg.getCreateTime());
                vo.setUnreadCount(unreadMap.getOrDefault(contactId, 0));
                result.add(vo);
            }
        }

        return result;
    }

    @Override
    public void blockUser(Long userId, Long targetUserId) {
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException("目标用户不存在");
        }
        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能拉黑自己");
        }

        LambdaQueryWrapper<ChatBlockRecord> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(ChatBlockRecord::getUserId, userId)
            .eq(ChatBlockRecord::getBlockedUserId, targetUserId);
        Long existCount = chatBlockRecordMapper.selectCount(existsWrapper);
        if (existCount != null && existCount > 0) {
            return;
        }

        ChatBlockRecord blockRecord = new ChatBlockRecord();
        blockRecord.setUserId(userId);
        blockRecord.setBlockedUserId(targetUserId);
        chatBlockRecordMapper.insert(blockRecord);
    }

    @Override
    public void unblockUser(Long userId, Long targetUserId) {
        LambdaQueryWrapper<ChatBlockRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatBlockRecord::getUserId, userId)
            .eq(ChatBlockRecord::getBlockedUserId, targetUserId);
        chatBlockRecordMapper.delete(wrapper);
    }

    @Override
    public List<ChatBlockItemVO> getBlockList(Long userId, String keyword) {
        LambdaQueryWrapper<ChatBlockRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatBlockRecord::getUserId, userId)
            .orderByDesc(ChatBlockRecord::getCreateTime);
        List<ChatBlockRecord> blockRecords = chatBlockRecordMapper.selectList(wrapper);
        if (blockRecords.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> blockedUserIds = blockRecords.stream()
            .map(ChatBlockRecord::getBlockedUserId)
            .distinct()
            .collect(Collectors.toList());
        Map<Long, UserInfo> userMap = userInfoMapper.selectBatchIds(blockedUserIds)
            .stream()
            .collect(Collectors.toMap(UserInfo::getUserId, user -> user));

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<ChatBlockItemVO> result = new ArrayList<>();
        for (ChatBlockRecord blockRecord : blockRecords) {
            UserInfo blockedUser = userMap.get(blockRecord.getBlockedUserId());
            if (blockedUser == null) {
                continue;
            }

            if (!normalizedKeyword.isEmpty()) {
                String nickName = Optional.ofNullable(blockedUser.getNickName()).orElse("");
                String studentNo = Optional.ofNullable(blockedUser.getStudentNo()).orElse("");
                String lowerNickName = nickName.toLowerCase(Locale.ROOT);
                String lowerStudentNo = studentNo.toLowerCase(Locale.ROOT);
                if (!lowerNickName.contains(normalizedKeyword) && !lowerStudentNo.contains(normalizedKeyword)) {
                    continue;
                }
            }

            ChatBlockItemVO itemVO = new ChatBlockItemVO();
            itemVO.setUserId(blockedUser.getUserId());
            itemVO.setNickName(blockedUser.getNickName());
            itemVO.setAvatar(blockedUser.getImageUrl());
            itemVO.setBlockTime(blockRecord.getCreateTime());
            result.add(itemVO);
        }

        return result;
    }

    @Override
    public int getBlockRelation(Long userId, Long targetUserId) {
        if (targetUserId == null || targetUserId <= 0 || userId.equals(targetUserId)) {
            return 0;
        }
        boolean meBlockedTarget = isBlocked(userId, targetUserId);
        boolean targetBlockedMe = isBlocked(targetUserId, userId);
        if (meBlockedTarget && targetBlockedMe) {
            return 3;
        }
        if (meBlockedTarget) {
            return 1;
        }
        if (targetBlockedMe) {
            return 2;
        }
        return 0;
    }

    private boolean isBlocked(Long userId, Long targetUserId) {
        LambdaQueryWrapper<ChatBlockRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatBlockRecord::getUserId, userId)
            .eq(ChatBlockRecord::getBlockedUserId, targetUserId);
        Long count = chatBlockRecordMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private ChatMessageVO toChatMessageVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}

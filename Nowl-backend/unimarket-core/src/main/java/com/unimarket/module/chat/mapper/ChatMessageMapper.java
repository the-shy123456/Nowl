package com.unimarket.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息Mapper
 */
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 聊天消息Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 获取用户及其联系人的最新消息列表
     * @param userId 当前用户ID
     * @return 消息列表
     */
    @Select("SELECT * FROM chat_message WHERE message_id IN (" +
            "  SELECT MAX(message_id) FROM chat_message " +
            "  WHERE sender_id = #{userId} OR receiver_id = #{userId} " +
            "  GROUP BY LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id)" +
            ") ORDER BY create_time DESC")
    List<ChatMessage> selectRecentMessages(@Param("userId") Long userId);

    /**
     * 获取各联系人的未读计数
     */
    @Select("SELECT sender_id as contactId, COUNT(*) as count FROM chat_message " +
            "WHERE receiver_id = #{userId} AND is_read = 0 " +
            "GROUP BY sender_id")
    List<java.util.Map<String, Object>> selectUnreadCounts(@Param("userId") Long userId);
}

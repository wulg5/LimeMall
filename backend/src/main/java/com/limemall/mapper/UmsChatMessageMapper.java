package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsChatMessage;
import com.limemall.vo.ChatConversationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 客服聊天消息数据访问层。
 */
@Mapper
public interface UmsChatMessageMapper extends BaseMapper<UmsChatMessage> {

    /**
     * 客服工作台会话列表：有聊天记录的会员 + 最后一条消息 + 未读数（按最近消息倒序）。
     */
    @Select("""
            SELECT m.id AS memberId, m.nickname AS nickname, m.username AS username, m.avatar AS avatar,
                   (SELECT c.content FROM ums_chat_message c
                     WHERE c.member_id = m.id ORDER BY c.id DESC LIMIT 1) AS lastContent,
                   (SELECT c2.create_time FROM ums_chat_message c2
                     WHERE c2.member_id = m.id ORDER BY c2.id DESC LIMIT 1) AS lastTime,
                   (SELECT COUNT(*) FROM ums_chat_message c3
                     WHERE c3.member_id = m.id AND c3.from_type = 1 AND c3.is_read = 0) AS unreadCount
            FROM ums_member m
            WHERE EXISTS (SELECT 1 FROM ums_chat_message c4 WHERE c4.member_id = m.id)
            ORDER BY lastTime DESC
            """)
    List<ChatConversationVO> selectConversations();
}

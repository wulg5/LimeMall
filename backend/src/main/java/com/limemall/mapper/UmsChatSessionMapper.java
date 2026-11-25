package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsChatSession;
import com.limemall.vo.ChatSessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 客服会话数据访问层。
 */
@Mapper
public interface UmsChatSessionMapper extends BaseMapper<UmsChatSession> {

    /** 待接入队列：进行中且未被分配的会话 */
    @Select("""
            SELECT s.id AS id, s.member_id AS memberId, m.nickname AS memberNickname,
                   m.username AS memberUsername, m.avatar AS memberAvatar,
                   s.agent_id AS agentId, a.nickname AS agentNickname,
                   s.status AS status, s.create_time AS createTime,
                   s.assign_time AS assignTime, s.end_time AS endTime,
                   (SELECT c.content FROM ums_chat_message c WHERE c.session_id = s.id ORDER BY c.id DESC LIMIT 1) AS lastContent,
                   (SELECT c2.create_time FROM ums_chat_message c2 WHERE c2.session_id = s.id ORDER BY c2.id DESC LIMIT 1) AS lastMessageTime,
                   (SELECT COUNT(*) FROM ums_chat_message c3 WHERE c3.session_id = s.id AND c3.from_type = 1 AND c3.is_read = 0) AS unreadCount
            FROM ums_chat_session s
            JOIN ums_member m ON m.id = s.member_id
            LEFT JOIN ums_admin a ON a.id = s.agent_id
            WHERE s.status = 0 AND s.agent_id IS NULL
            ORDER BY s.id DESC
            """)
    List<ChatSessionVO> selectQueue();

    /** 我的接待会话：进行中且分配给当前客服 */
    @Select("""
            SELECT s.id AS id, s.member_id AS memberId, m.nickname AS memberNickname,
                   m.username AS memberUsername, m.avatar AS memberAvatar,
                   s.agent_id AS agentId, a.nickname AS agentNickname,
                   s.status AS status, s.create_time AS createTime,
                   s.assign_time AS assignTime, s.end_time AS endTime,
                   (SELECT c.content FROM ums_chat_message c WHERE c.session_id = s.id ORDER BY c.id DESC LIMIT 1) AS lastContent,
                   (SELECT c2.create_time FROM ums_chat_message c2 WHERE c2.session_id = s.id ORDER BY c2.id DESC LIMIT 1) AS lastMessageTime,
                   (SELECT COUNT(*) FROM ums_chat_message c3 WHERE c3.session_id = s.id AND c3.from_type = 1 AND c3.is_read = 0) AS unreadCount
            FROM ums_chat_session s
            JOIN ums_member m ON m.id = s.member_id
            LEFT JOIN ums_admin a ON a.id = s.agent_id
            WHERE s.status = 0 AND s.agent_id = #{agentId}
            ORDER BY s.id DESC
            """)
    List<ChatSessionVO> selectMine(@Param("agentId") Long agentId);
}

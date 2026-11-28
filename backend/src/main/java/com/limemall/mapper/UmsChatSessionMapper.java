package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsChatSession;
import com.limemall.vo.ChatSessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客服会话数据访问层。
 */
@Mapper
public interface UmsChatSessionMapper extends BaseMapper<UmsChatSession> {

    // 与 UmsMenuMapper.selectByAdminId 的权限判定保持一致。
    String ELIGIBLE_AGENT = """
            EXISTS (
                SELECT 1
                FROM ums_admin candidate
                JOIN ums_admin_role ar ON ar.admin_id = candidate.id
                JOIN ums_role_menu rm ON rm.role_id = ar.role_id
                JOIN ums_menu m ON m.id = rm.menu_id
                WHERE candidate.id = s.agent_id AND candidate.status = 1
                  AND m.status = 1 AND m.perm = 'ums:chat:reply'
            )
            """;

    @Select("SELECT s.agent_id FROM ums_chat_session s WHERE s.id = #{id} AND s.agent_id = #{agentId} AND " + ELIGIBLE_AGENT)
    Long selectEligibleAgentId(@Param("id") Long id, @Param("agentId") Long agentId);

    @Update("""
            UPDATE ums_chat_session s
            SET agent_id = #{agentId}, assign_time = #{assignedAt}
            WHERE s.id = #{id} AND s.status = 0 AND NOT
            """ + ELIGIBLE_AGENT)
    int claimAvailable(@Param("id") Long id, @Param("agentId") Long agentId,
                       @Param("assignedAt") LocalDateTime assignedAt);

    @Select("SELECT * FROM ums_chat_session WHERE id = #{id} FOR UPDATE")
    UmsChatSession lockById(Long id);

    @Update("UPDATE ums_chat_session SET agent_id = NULL, assign_time = NULL WHERE agent_id = #{agentId} AND status = 0")
    int releaseOpenSessions(Long agentId);

    @Update("""
            <script>
            UPDATE ums_chat_session s
            SET agent_id = NULL, assign_time = NULL
            WHERE s.status = 0 AND s.agent_id IS NOT NULL
            <if test="agentId != null">AND s.agent_id = #{agentId}</if>
            AND NOT
            """ + ELIGIBLE_AGENT + "</script>")
    int releaseIneligibleSessions(@Param("agentId") Long agentId);

    /** 待接入队列：进行中且没有具备回复权限的接待客服。 */
    @Select("""
            SELECT s.id AS id, s.member_id AS memberId, m.nickname AS memberNickname,
                   m.username AS memberUsername, m.avatar AS memberAvatar, m.level AS memberLevel, m.user_no AS memberUserNo,
                   s.status AS status, s.create_time AS createTime, s.end_time AS endTime,
                   (SELECT c.content FROM ums_chat_message c WHERE c.session_id = s.id ORDER BY c.id DESC LIMIT 1) AS lastContent,
                   (SELECT c2.create_time FROM ums_chat_message c2 WHERE c2.session_id = s.id ORDER BY c2.id DESC LIMIT 1) AS lastMessageTime,
                   (SELECT COUNT(*) FROM ums_chat_message c3 WHERE c3.session_id = s.id AND c3.from_type = 1 AND c3.is_read = 0) AS unreadCount
            FROM ums_chat_session s
            JOIN ums_member m ON m.id = s.member_id
            WHERE s.status = 0 AND NOT
            """ + ELIGIBLE_AGENT + """
            ORDER BY s.id DESC
            """)
    List<ChatSessionVO> selectQueue();

    /** 我的接待会话：进行中且分配给当前客服 */
    @Select("""
            SELECT s.id AS id, s.member_id AS memberId, m.nickname AS memberNickname,
                   m.username AS memberUsername, m.avatar AS memberAvatar, m.level AS memberLevel, m.user_no AS memberUserNo,
                   s.agent_id AS agentId, a.nickname AS agentNickname,
                   s.status AS status, s.create_time AS createTime,
                   s.assign_time AS assignTime, s.end_time AS endTime,
                   (SELECT c.content FROM ums_chat_message c WHERE c.session_id = s.id ORDER BY c.id DESC LIMIT 1) AS lastContent,
                   (SELECT c2.create_time FROM ums_chat_message c2 WHERE c2.session_id = s.id ORDER BY c2.id DESC LIMIT 1) AS lastMessageTime,
                   (SELECT COUNT(*) FROM ums_chat_message c3 WHERE c3.session_id = s.id AND c3.from_type = 1 AND c3.is_read = 0) AS unreadCount
            FROM ums_chat_session s
            JOIN ums_member m ON m.id = s.member_id
            LEFT JOIN ums_admin a ON a.id = s.agent_id
            WHERE s.status = 0 AND s.agent_id = #{agentId} AND
            """ + ELIGIBLE_AGENT + """
            ORDER BY s.id DESC
            """)
    List<ChatSessionVO> selectMine(@Param("agentId") Long agentId);
}

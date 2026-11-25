package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.limemall.common.BusinessException;
import com.limemall.entity.UmsChatMessage;
import com.limemall.mapper.UmsChatMessageMapper;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.vo.ChatConversationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 客服聊天服务（会员 ⇄ 客服双向消息）。
 *
 * 设计说明：
 * - 消息正文按纯文本存储与渲染（前端不做 HTML 渲染），杜绝 XSS；
 * - 采用轮询（前端 5 秒）而非 WebSocket：课设部署无需长连接网关，
 *   在 Nginx 反代下零配置可用；如需实时性可平滑升级为 WebSocket/SSE；
 * - 读取历史即标记已读：会话双方各自只标记"对方发来"的消息。
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_CONTENT_LENGTH = 500;
    private static final int HISTORY_LIMIT = 100;

    public static final int FROM_MEMBER = 1;
    public static final int FROM_ADMIN = 2;

    private final UmsChatMessageMapper chatMapper;
    private final UmsMemberMapper memberMapper;

    /** 会员发送消息 */
    public void sendFromMember(Long memberId, String content) {
        saveMessage(memberId, null, FROM_MEMBER, content);
    }

    /** 管理员（客服）回复消息 */
    public void sendFromAdmin(Long adminId, Long memberId, String content) {
        if (memberMapper.selectById(memberId) == null) {
            throw new BusinessException("会员不存在");
        }
        saveMessage(memberId, adminId, FROM_ADMIN, content);
    }

    /** 会员端拉取历史（同时把管理员发来的消息标记已读） */
    public List<UmsChatMessage> memberHistory(Long memberId) {
        chatMapper.update(null, new LambdaUpdateWrapper<UmsChatMessage>()
                .set(UmsChatMessage::getIsRead, 1)
                .eq(UmsChatMessage::getMemberId, memberId)
                .eq(UmsChatMessage::getFromType, FROM_ADMIN)
                .eq(UmsChatMessage::getIsRead, 0));
        return lastMessages(memberId);
    }

    /** 客服端拉取某会员历史（同时把会员发来的消息标记已读） */
    public List<UmsChatMessage> adminHistory(Long memberId) {
        chatMapper.update(null, new LambdaUpdateWrapper<UmsChatMessage>()
                .set(UmsChatMessage::getIsRead, 1)
                .eq(UmsChatMessage::getMemberId, memberId)
                .eq(UmsChatMessage::getFromType, FROM_MEMBER)
                .eq(UmsChatMessage::getIsRead, 0));
        return lastMessages(memberId);
    }

    /** 客服工作台会话列表 */
    public List<ChatConversationVO> conversations() {
        return chatMapper.selectConversations();
    }

    private void saveMessage(Long memberId, Long adminId, int fromType, String content) {
        String text = content == null ? "" : content.trim();
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("消息不能为空");
        }
        if (text.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("消息不能超过 " + MAX_CONTENT_LENGTH + " 字");
        }
        UmsChatMessage message = new UmsChatMessage();
        message.setMemberId(memberId);
        message.setAdminId(adminId);
        message.setFromType(fromType);
        message.setContent(text);
        message.setIsRead(0);
        chatMapper.insert(message);
    }

    /** 最近 100 条消息（按时间正序返回） */
    private List<UmsChatMessage> lastMessages(Long memberId) {
        List<UmsChatMessage> messages = chatMapper.selectList(
                new LambdaQueryWrapper<UmsChatMessage>()
                        .eq(UmsChatMessage::getMemberId, memberId)
                        .orderByDesc(UmsChatMessage::getId)
                        .last("LIMIT " + HISTORY_LIMIT));
        Collections.reverse(messages);
        return messages;
    }
}

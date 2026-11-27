package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.entity.UmsAdmin;
import com.limemall.entity.UmsChatMessage;
import com.limemall.entity.UmsChatSession;
import com.limemall.entity.UmsMember;
import com.limemall.mapper.UmsAdminMapper;
import com.limemall.mapper.UmsChatMessageMapper;
import com.limemall.mapper.UmsChatSessionMapper;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.vo.ChatSessionVO;
import com.limemall.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 客服会话服务（会话制 + 一对一分派）。
 *
 * 设计说明：
 * - 每个会员同一时间最多一个「进行中」会话；会话经客服「接入」后绑定唯一接待客服；
 * - 仅接待客服本人可回复（一对一分派），超级管理员/运营只能查看记录（只读监管）；
 * - 客服可结束会话；会员在会话结束后再发消息会自动创建新的待接入会话；
 * - 消息纯文本存储与渲染防 XSS；前端轮询获取（Nginx 零配置），可平滑升级 WebSocket。
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_CONTENT_LENGTH = 500;
    private static final int HISTORY_LIMIT = 100;

    public static final int FROM_MEMBER = 1;
    public static final int FROM_ADMIN = 2;
    public static final int SESSION_OPEN = 0;
    public static final int SESSION_ENDED = 1;

    private final UmsChatSessionMapper sessionMapper;
    private final UmsChatMessageMapper messageMapper;
    private final UmsMemberMapper memberMapper;
    private final UmsAdminMapper adminMapper;
    private final ChatWebSocketHandler realtime;

    // ==================================================================
    // 会员端
    // ==================================================================

    /** 会员端当前会话与消息（拉取即把客服发来的消息标记已读） */
    public Map<String, Object> memberCurrent(Long memberId) {
        Map<String, Object> data = new HashMap<>();
        UmsChatSession session = findOpenSession(memberId, false);
        if (session == null) {
            session = sessionMapper.selectOne(new LambdaQueryWrapper<UmsChatSession>()
                    .eq(UmsChatSession::getMemberId, memberId)
                    .eq(UmsChatSession::getStatus, SESSION_ENDED)
                    .orderByDesc(UmsChatSession::getId).last("LIMIT 1"));
        }
        if (session == null) {
            data.put("session", null);
            data.put("messages", List.of());
            return data;
        }
        markRead(session.getId(), FROM_ADMIN);
        data.put("session", buildSessionVO(session));
        data.put("messages", lastMessages(session.getId()));
        return data;
    }

    /** 会员发送消息：无进行中会话则自动创建（待接入） */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void memberSend(Long memberId, String content) {
        String text = validateContent(content);
        if (memberMapper.lockById(memberId) == null) {
            throw new BusinessException("会员不存在");
        }
        UmsChatSession session = findOpenSession(memberId, true);
        if (session == null) {
            session = new UmsChatSession();
            session.setMemberId(memberId);
            session.setStatus(SESSION_OPEN);
            sessionMapper.insert(session);
        }
        insertMessage(session.getId(), memberId, null, FROM_MEMBER, text);
        realtime.notifyAdmins();
    }

    // ==================================================================
    // 客服端
    // ==================================================================

    /** 待接入队列 */
    public List<ChatSessionVO> queue() {
        return sessionMapper.selectQueue();
    }

    /** 我的接待中会话 */
    public List<ChatSessionVO> mine(Long agentId) {
        return sessionMapper.selectMine(agentId);
    }

    /** 全部会话历史（只读，分页） */
    public PageResult<ChatSessionVO> history(long pageNum, long pageSize) {
        Page<UmsChatSession> page = sessionMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UmsChatSession>().orderByDesc(UmsChatSession::getId));
        List<UmsChatSession> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(page, List.of());
        }
        List<Long> sessionIds = records.stream().map(UmsChatSession::getId).toList();
        Map<Long, UmsMember> memberMap = memberMapper.selectBatchIds(
                        records.stream().map(UmsChatSession::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(UmsMember::getId, Function.identity()));
        Map<Long, UmsAdmin> agentMap = adminMapper.selectBatchIds(
                        records.stream().map(UmsChatSession::getAgentId)
                                .filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(UmsAdmin::getId, Function.identity()));
        Map<Long, UmsChatMessage> lastMessageMap = messageMapper.selectList(
                        new LambdaQueryWrapper<UmsChatMessage>()
                                .in(UmsChatMessage::getSessionId, sessionIds)
                                .orderByDesc(UmsChatMessage::getId))
                .stream()
                .collect(Collectors.toMap(UmsChatMessage::getSessionId, Function.identity(), (a, b) -> a));

        List<ChatSessionVO> vos = records.stream().map(session -> {
            ChatSessionVO vo = new ChatSessionVO();
            vo.setId(session.getId());
            vo.setMemberId(session.getMemberId());
            UmsMember member = memberMap.get(session.getMemberId());
            if (member != null) {
                vo.setMemberNickname(member.getNickname());
                vo.setMemberUsername(member.getUsername());
                vo.setMemberAvatar(member.getAvatar());
                vo.setMemberLevel(member.getLevel());
                vo.setMemberUserNo(member.getUserNo());
            }
            vo.setAgentId(session.getAgentId());
            UmsAdmin agent = session.getAgentId() == null ? null : agentMap.get(session.getAgentId());
            vo.setAgentNickname(agent == null ? null : agent.getNickname());
            vo.setStatus(session.getStatus());
            vo.setCreateTime(session.getCreateTime());
            vo.setAssignTime(session.getAssignTime());
            vo.setEndTime(session.getEndTime());
            UmsChatMessage last = lastMessageMap.get(session.getId());
            if (last != null) {
                vo.setLastContent(last.getContent());
                vo.setLastMessageTime(last.getCreateTime());
            }
            vo.setUnreadCount(0L);
            return vo;
        }).toList();
        return PageResult.of(page, vos);
    }

    /** 会话消息记录：任何具有查看权限的管理员可看；接待客服查看时标记会员消息已读 */
    public List<UmsChatMessage> agentMessages(Long adminId, Long sessionId) {
        UmsChatSession session = loadSession(sessionId);
        if (adminId != null && adminId.equals(session.getAgentId())) {
            markRead(sessionId, FROM_MEMBER);
        }
        return lastMessages(sessionId);
    }

    /** 接入会话：CAS 抢占（agent_id 为空 → 我），防止多个客服同时接待 */
    @Transactional
    public void claim(Long adminId, Long sessionId) {
        UmsChatSession session = loadSession(sessionId);
        if (session.getStatus() != SESSION_OPEN) {
            throw new BusinessException("会话已结束，无法接入");
        }
        int affected = sessionMapper.update(null, new LambdaUpdateWrapper<UmsChatSession>()
                .set(UmsChatSession::getAgentId, adminId)
                .set(UmsChatSession::getAssignTime, LocalDateTime.now())
                .eq(UmsChatSession::getId, sessionId)
                .isNull(UmsChatSession::getAgentId)
                .eq(UmsChatSession::getStatus, SESSION_OPEN));
        if (affected == 0) {
            throw new BusinessException("该会话已被其他客服接入");
        }
        realtime.notifyAdmins();
        realtime.notifyMember(session.getMemberId());
    }

    /** 结束会话：仅接待客服本人可操作（条件更新防并发） */
    @Transactional
    public void end(Long adminId, Long sessionId) {
        UmsChatSession session = loadSession(sessionId);
        if (session.getStatus() != SESSION_OPEN) {
            throw new BusinessException("会话已结束");
        }
        if (!adminId.equals(session.getAgentId())) {
            throw new BusinessException("仅该会话的接待客服可结束会话");
        }
        int affected = sessionMapper.update(null, new LambdaUpdateWrapper<UmsChatSession>()
                .set(UmsChatSession::getStatus, SESSION_ENDED)
                .set(UmsChatSession::getEndTime, LocalDateTime.now())
                .eq(UmsChatSession::getId, sessionId)
                .eq(UmsChatSession::getStatus, SESSION_OPEN)
                .eq(UmsChatSession::getAgentId, adminId));
        if (affected == 0) {
            throw new BusinessException("会话状态已变更，请刷新后重试");
        }
        realtime.notifyAdmins();
        realtime.notifyMember(session.getMemberId());
    }

    /** 客服回复：仅该会话的接待客服本人（一对一分派） */
    @Transactional
    public void agentSend(Long adminId, Long sessionId, String content) {
        String text = validateContent(content);
        UmsChatSession session = sessionMapper.lockById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (session.getStatus() != SESSION_OPEN) {
            throw new BusinessException("会话已结束，无法回复");
        }
        if (session.getAgentId() == null) {
            throw new BusinessException("请先接入该会话再回复");
        }
        if (!session.getAgentId().equals(adminId)) {
            throw new BusinessException("仅该会话的接待客服可回复");
        }
        insertMessage(sessionId, session.getMemberId(), adminId, FROM_ADMIN, text);
        realtime.notifyMember(session.getMemberId());
    }

    // ==================================================================
    // 私有方法
    // ==================================================================

    private UmsChatSession findOpenSession(Long memberId, boolean lock) {
        return sessionMapper.selectOne(new LambdaQueryWrapper<UmsChatSession>()
                .eq(UmsChatSession::getMemberId, memberId)
                .eq(UmsChatSession::getStatus, SESSION_OPEN)
                .orderByDesc(UmsChatSession::getId)
                .last(lock ? "LIMIT 1 FOR UPDATE" : "LIMIT 1"));
    }

    private UmsChatSession loadSession(Long sessionId) {
        UmsChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        return session;
    }

    private void insertMessage(Long sessionId, Long memberId, Long adminId, int fromType, String content) {
        UmsChatMessage message = new UmsChatMessage();
        message.setSessionId(sessionId);
        message.setMemberId(memberId);
        message.setAdminId(adminId);
        message.setFromType(fromType);
        message.setContent(content);
        message.setIsRead(0);
        messageMapper.insert(message);
    }

    /** 标记某会话中「指定发送方」的消息为已读 */
    private void markRead(Long sessionId, int fromType) {
        messageMapper.update(null, new LambdaUpdateWrapper<UmsChatMessage>()
                .set(UmsChatMessage::getIsRead, 1)
                .eq(UmsChatMessage::getSessionId, sessionId)
                .eq(UmsChatMessage::getFromType, fromType)
                .eq(UmsChatMessage::getIsRead, 0));
    }

    private List<UmsChatMessage> lastMessages(Long sessionId) {
        List<UmsChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<UmsChatMessage>()
                        .eq(UmsChatMessage::getSessionId, sessionId)
                        .orderByDesc(UmsChatMessage::getId)
                        .last("LIMIT " + HISTORY_LIMIT));
        Collections.reverse(messages);
        return messages;
    }

    private String validateContent(String content) {
        String text = content == null ? "" : content.trim();
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("消息不能为空");
        }
        if (text.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("消息不能超过 " + MAX_CONTENT_LENGTH + " 字");
        }
        return text;
    }

    /** 单个会话 VO（会员端展示用） */
    private ChatSessionVO buildSessionVO(UmsChatSession session) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setMemberId(session.getMemberId());
        UmsMember member = memberMapper.selectById(session.getMemberId());
        if (member != null) {
            vo.setMemberNickname(member.getNickname());
            vo.setMemberUsername(member.getUsername());
            vo.setMemberAvatar(member.getAvatar());
                vo.setMemberLevel(member.getLevel());
                vo.setMemberUserNo(member.getUserNo());
        }
        vo.setAgentId(session.getAgentId());
        if (session.getAgentId() != null) {
            UmsAdmin agent = adminMapper.selectById(session.getAgentId());
            vo.setAgentNickname(agent == null ? null : agent.getNickname());
        }
        vo.setStatus(session.getStatus());
        vo.setCreateTime(session.getCreateTime());
        vo.setAssignTime(session.getAssignTime());
        vo.setEndTime(session.getEndTime());
        return vo;
    }
}

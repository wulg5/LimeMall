package com.limemall.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客服实时消息通道（原生 WebSocket，事件驱动刷新）。
 *
 * 设计：
 * - 握手鉴权：浏览器 WebSocket 无法自定义请求头，令牌经查询参数 `?token=` 传入，
 *   在连接建立时用 JWT + 账号状态双重校验（与 REST 过滤器同一套规则）；
 * - 服务端只推送「数据已变更」事件（CHAT_UPDATE），消息内容仍由 REST 拉取——
 *   简单、幂等、易排障；前端收到事件即刷新，断线自动重连并保留轮询兜底；
 * - 单机内存会话表，多实例部署需引入 Redis Pub/Sub 或消息队列广播（接口语义不变）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /** memberId -> 会员端连接 */
    private final Map<Long, Set<WebSocketSession>> memberSessions = new ConcurrentHashMap<>();
    /** adminId -> 管理端连接 */
    private final Map<Long, Set<WebSocketSession>> adminSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 身份由 ChatHandshakeInterceptor 在握手阶段校验并写入
        String type = (String) session.getAttributes().get("wsType");
        Long userId = (Long) session.getAttributes().get("wsUserId");
        if (type == null || userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }
        session.getAttributes().put("type", type);
        session.getAttributes().put("userId", userId);
        if ("ADMIN".equals(type)) {
            adminSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        } else {
            memberSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object type = session.getAttributes().get("type");
        Object userId = session.getAttributes().get("userId");
        if (type == null || userId == null) {
            return;
        }
        Map<Long, Set<WebSocketSession>> bucket = "ADMIN".equals(type) ? adminSessions : memberSessions;
        Set<WebSocketSession> sessions = bucket.get((Long) userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                bucket.remove((Long) userId);
            }
        }
    }

    /** 通知指定会员：数据已变更 */
    public void notifyMember(Long memberId) {
        sendTo(memberSessions.get(memberId));
    }

    /** 通知全部在线管理员/客服：数据已变更 */
    public void notifyAdmins() {
        adminSessions.values().forEach(this::sendTo);
    }

    private void sendTo(Collection<WebSocketSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage("{\"type\":\"CHAT_UPDATE\"}");
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (Exception e) {
                log.debug("WebSocket 推送失败（忽略）: {}", e.getMessage());
            }
        }
    }

}

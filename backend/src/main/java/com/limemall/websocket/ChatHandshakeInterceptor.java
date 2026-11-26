package com.limemall.websocket;

import com.limemall.entity.UmsAdmin;
import com.limemall.entity.UmsMember;
import com.limemall.mapper.UmsAdminMapper;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器。
 *
 * 在协议升级（101）**之前**完成令牌与账号状态校验：
 * 非法令牌直接返回 403，拒绝握手，不建立连接（比"先升级再踢下线"更干净）。
 * 鉴权通过后把身份写入 attributes，供 ChatWebSocketHandler 使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UmsMemberMapper memberMapper;
    private final UmsAdminMapper adminMapper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String token = extractToken(request.getURI().getQuery());
            if (token == null) {
                return reject(response);
            }
            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            String type = claims.get("type", String.class);
            if ("ADMIN".equals(type)) {
                UmsAdmin admin = adminMapper.selectById(userId);
                if (admin == null || admin.getStatus() == null || admin.getStatus() != 1) {
                    return reject(response);
                }
                attributes.put("wsType", "ADMIN");
                attributes.put("wsUserId", userId);
                return true;
            }
            if ("MEMBER".equals(type)) {
                UmsMember member = memberMapper.selectById(userId);
                if (member == null || member.getStatus() == null || member.getStatus() != 1) {
                    return reject(response);
                }
                attributes.put("wsType", "MEMBER");
                attributes.put("wsUserId", userId);
                return true;
            }
            return reject(response);
        } catch (Exception e) {
            return reject(response);
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private boolean reject(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return false;
    }

    private String extractToken(String query) {
        if (query == null || !query.contains("token=")) {
            return null;
        }
        for (String pair : query.split("&")) {
            if (pair.startsWith("token=")) {
                String value = URLDecoder.decode(pair.substring(6), StandardCharsets.UTF_8);
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }
}

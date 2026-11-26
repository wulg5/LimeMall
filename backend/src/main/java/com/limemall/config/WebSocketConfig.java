package com.limemall.config;

import com.limemall.websocket.ChatHandshakeInterceptor;
import com.limemall.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：客服实时通道 /ws/chat。
 * 握手阶段的鉴权由 ChatWebSocketHandler 内部完成（JWT + 账号状态），
 * 因此允许任意来源握手（生产出口由 Nginx/cloudflared 控制）。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatHandshakeInterceptor chatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}

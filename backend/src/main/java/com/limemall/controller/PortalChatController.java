package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.dto.ChatSendDTO;
import com.limemall.entity.UmsChatMessage;
import com.limemall.security.SecurityUtils;
import com.limemall.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 会员端联系客服接口（会话制：每个会员同一时间一个进行中会话）。
 */
@Tag(name = "前台-客服聊天")
@RestController
@RequestMapping("/portal/chat")
@RequiredArgsConstructor
public class PortalChatController {

    private final ChatService chatService;

    @GetMapping("/current")
    @Operation(summary = "当前会话与消息（拉取即标记客服消息已读）")
    public Result<Map<String, Object>> current() {
        return Result.ok(chatService.memberCurrent(SecurityUtils.getMemberId()));
    }

    @GetMapping("/sessions/{id}/messages")
    @Operation(summary = "分页查看本人的会话消息")
    public Result<List<UmsChatMessage>> messages(@PathVariable Long id, @RequestParam(required = false) Long beforeId) {
        return Result.ok(chatService.memberMessages(SecurityUtils.getMemberId(), id, beforeId));
    }

    @PostMapping("/messages")
    @Operation(summary = "发送消息（无进行中会话时自动创建待接入会话）")
    public Result<Void> send(@Valid @RequestBody ChatSendDTO dto) {
        chatService.memberSend(SecurityUtils.getMemberId(), dto.getContent());
        return Result.ok();
    }
}

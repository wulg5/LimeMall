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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员端联系客服接口。
 */
@Tag(name = "前台-客服聊天")
@RestController
@RequestMapping("/portal/chat")
@RequiredArgsConstructor
public class PortalChatController {

    private final ChatService chatService;

    @GetMapping("/messages")
    @Operation(summary = "我的聊天记录（拉取即标记已读）")
    public Result<List<UmsChatMessage>> messages() {
        return Result.ok(chatService.memberHistory(SecurityUtils.getMemberId()));
    }

    @PostMapping("/messages")
    @Operation(summary = "发送消息给客服")
    public Result<Void> send(@Valid @RequestBody ChatSendDTO dto) {
        chatService.sendFromMember(SecurityUtils.getMemberId(), dto.getContent());
        return Result.ok();
    }
}

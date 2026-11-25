package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.dto.ChatSendDTO;
import com.limemall.entity.UmsChatMessage;
import com.limemall.security.SecurityUtils;
import com.limemall.service.ChatService;
import com.limemall.vo.ChatConversationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客服工作台接口（管理端）。
 */
@Tag(name = "后台-客服聊天")
@RestController
@RequestMapping("/admin/chat")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ums:chat:list')")
public class AdminChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    @Operation(summary = "会话列表（含未读数）")
    public Result<List<ChatConversationVO>> conversations() {
        return Result.ok(chatService.conversations());
    }

    @GetMapping("/conversations/{memberId}/messages")
    @Operation(summary = "某会员的消息记录（拉取即标记已读）")
    public Result<List<UmsChatMessage>> messages(@PathVariable Long memberId) {
        return Result.ok(chatService.adminHistory(memberId));
    }

    @PostMapping("/conversations/{memberId}/messages")
    @Operation(summary = "回复会员")
    public Result<Void> reply(@PathVariable Long memberId, @Valid @RequestBody ChatSendDTO dto) {
        chatService.sendFromAdmin(SecurityUtils.getAdminId(), memberId, dto.getContent());
        return Result.ok();
    }
}

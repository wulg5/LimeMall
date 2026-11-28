package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.dto.ChatSendDTO;
import com.limemall.entity.UmsChatMessage;
import com.limemall.security.SecurityUtils;
import com.limemall.service.ChatService;
import com.limemall.vo.ChatSessionVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客服工作台接口（会话制 + 一对一分派）。
 *
 * 权限模型（职责分离）：
 * - ums:chat:list：查看会话与消息记录（超级管理员/运营 = 只读监管视角）；
 * - ums:chat:reply：仅客服专员角色持有（接入会话、回复、结束会话）；
 * - 回复还要求「当前管理员 == 会话接待客服」，杜绝多名客服对同一会话重复回复。
 */
@Tag(name = "后台-客服工作台")
@RestController
@RequestMapping("/admin/chat")
@RequiredArgsConstructor
public class AdminChatController {

    private final ChatService chatService;

    @GetMapping("/queue")
    @PreAuthorize("hasAuthority('ums:chat:list')")
    @Operation(summary = "待接入会话队列")
    public Result<List<ChatSessionVO>> queue() {
        return Result.ok(chatService.queue());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('ums:chat:list')")
    @Operation(summary = "我的接待中会话")
    public Result<List<ChatSessionVO>> mine() {
        return Result.ok(chatService.mine(SecurityUtils.getAdminId()));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('ums:chat:list')")
    @Operation(summary = "全部会话历史（只读）")
    public Result<PageResult<ChatSessionVO>> history(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(chatService.history(pageNum, pageSize));
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize("hasAuthority('ums:chat:list')")
    @Operation(summary = "查询会话当前状态")
    public Result<ChatSessionVO> session(@PathVariable Long id) {
        return Result.ok(chatService.sessionDetail(id));
    }

    @GetMapping("/sessions/{id}/messages")
    @PreAuthorize("hasAuthority('ums:chat:list')")
    @Operation(summary = "会话消息记录（接待客服查看时自动标记已读）")
    public Result<List<UmsChatMessage>> messages(@PathVariable Long id, @RequestParam(required = false) Long beforeId) {
        return Result.ok(chatService.agentMessages(SecurityUtils.getAdminId(), id, beforeId));
    }

    @PostMapping("/sessions/{id}/claim")
    @PreAuthorize("hasAuthority('ums:chat:reply')")
    @Operation(summary = "接入会话（分配给我，一对一）")
    public Result<Void> claim(@PathVariable Long id) {
        chatService.claim(SecurityUtils.getAdminId(), id);
        return Result.ok();
    }

    @PostMapping("/sessions/{id}/end")
    @PreAuthorize("hasAuthority('ums:chat:reply')")
    @Operation(summary = "结束会话")
    public Result<Void> end(@PathVariable Long id) {
        chatService.end(SecurityUtils.getAdminId(), id);
        return Result.ok();
    }

    @PostMapping("/sessions/{id}/messages")
    @PreAuthorize("hasAuthority('ums:chat:reply')")
    @Operation(summary = "回复会员消息（仅接待客服本人）")
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody ChatSendDTO dto) {
        chatService.agentSend(SecurityUtils.getAdminId(), id, dto.getContent());
        return Result.ok();
    }
}

package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.dto.CommentSubmitDTO;
import com.limemall.security.SecurityUtils;
import com.limemall.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员评价接口。
 */
@Tag(name = "前台-商品评价")
@RestController
@RequestMapping("/portal/comments")
@RequiredArgsConstructor
public class PortalCommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "提交评价（仅已完成订单）")
    public Result<Integer> submit(@Valid @RequestBody CommentSubmitDTO dto) {
        return Result.ok(commentService.submit(SecurityUtils.getMemberId(), dto));
    }
}

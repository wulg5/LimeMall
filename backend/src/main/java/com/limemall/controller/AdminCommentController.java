package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.service.CommentService;
import com.limemall.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价管理接口（管理端）。
 */
@Tag(name = "后台-评价管理")
@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    @PreAuthorize("hasAuthority('pms:comment:list')")
    @Operation(summary = "分页查询评价")
    public Result<PageResult<CommentVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(commentService.adminPage(pageNum, pageSize));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pms:comment:delete')")
    @Operation(summary = "删除评价（内容违规处理）")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }
}

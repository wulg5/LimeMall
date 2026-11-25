package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.service.MemberService;
import com.limemall.vo.MemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员管理接口。
 */
@Tag(name = "后台-会员管理")
@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    @PreAuthorize("hasAuthority('ums:member:list')")
    @Operation(summary = "分页查询会员")
    public Result<PageResult<MemberVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status) {
        return Result.ok(memberService.page(pageNum, pageSize, keyword, status));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ums:member:update')")
    @Operation(summary = "启用/禁用会员")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        memberService.updateStatus(id, status);
        return Result.ok();
    }
}

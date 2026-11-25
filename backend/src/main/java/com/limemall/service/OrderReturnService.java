package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.dto.ReturnApplyDTO;
import com.limemall.dto.ReturnHandleDTO;
import com.limemall.entity.OmsOrder;
import com.limemall.entity.OmsOrderReturn;
import com.limemall.entity.UmsMember;
import com.limemall.mapper.OmsOrderMapper;
import com.limemall.mapper.OmsOrderReturnMapper;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.vo.ReturnVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 退款申请服务：管理员审核 + 会员端申请。
 *
 * 业务规则：
 * 1. 仅「待发货/已发货」订单可申请退款；
 * 2. 同一订单同时只允许存在一条待审核申请；
 * 3. 审核通过后：回补库存、回退销量、恢复优惠券、关闭订单（同一事务）。
 */
@Service
@RequiredArgsConstructor
public class OrderReturnService {

    private final OmsOrderReturnMapper returnMapper;
    private final OmsOrderMapper orderMapper;
    private final UmsMemberMapper memberMapper;
    private final OrderService orderService;

    /**
     * 管理员分页查询退款申请。
     */
    public PageResult<ReturnVO> page(long pageNum, long pageSize, Integer status, String orderSn) {
        LambdaQueryWrapper<OmsOrderReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, OmsOrderReturn::getStatus, status)
                .like(StringUtils.hasText(orderSn), OmsOrderReturn::getOrderSn, orderSn)
                .orderByDesc(OmsOrderReturn::getId);
        Page<OmsOrderReturn> page = returnMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<OmsOrderReturn> returns = page.getRecords();
        if (returns.isEmpty()) {
            return PageResult.of(page, new ArrayList<>());
        }
        Map<Long, UmsMember> memberMap = memberMapper.selectBatchIds(
                        returns.stream().map(OmsOrderReturn::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(UmsMember::getId, Function.identity()));

        List<ReturnVO> records = returns.stream().map(ret -> {
            ReturnVO vo = new ReturnVO();
            vo.setId(ret.getId());
            vo.setOrderId(ret.getOrderId());
            vo.setOrderSn(ret.getOrderSn());
            vo.setMemberId(ret.getMemberId());
            UmsMember member = memberMap.get(ret.getMemberId());
            vo.setMemberNickname(member == null ? "" : member.getNickname());
            vo.setAmount(ret.getAmount());
            vo.setReason(ret.getReason());
            vo.setDescription(ret.getDescription());
            vo.setStatus(ret.getStatus());
            vo.setHandleNote(ret.getHandleNote());
            vo.setHandleTime(ret.getHandleTime());
            vo.setCreateTime(ret.getCreateTime());
            return vo;
        }).toList();
        return PageResult.of(page, records);
    }

    /**
     * 管理员审核退款申请。
     */
    @Transactional
    public void handle(Long id, ReturnHandleDTO dto) {
        OmsOrderReturn ret = returnMapper.selectById(id);
        if (ret == null) {
            throw new BusinessException("退款申请不存在");
        }
        if (ret.getStatus() != 0) {
            throw new BusinessException("该申请已处理，请勿重复操作");
        }
        if (Boolean.TRUE.equals(dto.getApprove())) {
            OmsOrder order = orderMapper.selectById(ret.getOrderId());
            if (order == null) {
                throw new BusinessException("关联订单不存在");
            }
            if (order.getStatus() != OrderService.STATUS_UNSHIPPED
                    && order.getStatus() != OrderService.STATUS_SHIPPED) {
                throw new BusinessException("当前订单状态不允许退款（仅待发货/已发货订单支持）");
            }
            // 回补库存/销量/优惠券，并关闭订单
            orderService.restoreStockAndCoupon(order);
            OmsOrder update = new OmsOrder();
            update.setId(order.getId());
            update.setStatus(OrderService.STATUS_CLOSED);
            update.setCloseTime(LocalDateTime.now());
            orderMapper.updateById(update);
            ret.setStatus(1);
        } else {
            ret.setStatus(2);
        }
        ret.setHandleNote(dto.getHandleNote());
        ret.setHandleTime(LocalDateTime.now());
        returnMapper.updateById(ret);
    }

    /**
     * 会员端申请退款。
     */
    public void apply(Long memberId, ReturnApplyDTO dto) {
        OmsOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != OrderService.STATUS_UNSHIPPED
                && order.getStatus() != OrderService.STATUS_SHIPPED) {
            throw new BusinessException("当前订单状态不支持申请退款");
        }
        Long pending = returnMapper.selectCount(new LambdaQueryWrapper<OmsOrderReturn>()
                .eq(OmsOrderReturn::getOrderId, order.getId())
                .eq(OmsOrderReturn::getStatus, 0));
        if (pending > 0) {
            throw new BusinessException("该订单已有待处理的退款申请，请耐心等待审核");
        }
        OmsOrderReturn ret = new OmsOrderReturn();
        ret.setOrderId(order.getId());
        ret.setOrderSn(order.getOrderSn());
        ret.setMemberId(memberId);
        ret.setReason(dto.getReason());
        ret.setDescription(dto.getDescription());
        ret.setAmount(order.getPayAmount());
        ret.setStatus(0);
        returnMapper.insert(ret);
    }

    /**
     * 查询订单的最新退款申请（订单详情页展示用）。
     */
    public OmsOrderReturn getLatestByOrderId(Long orderId) {
        return returnMapper.selectOne(new LambdaQueryWrapper<OmsOrderReturn>()
                .eq(OmsOrderReturn::getOrderId, orderId)
                .orderByDesc(OmsOrderReturn::getId)
                .last("LIMIT 1"));
    }
}

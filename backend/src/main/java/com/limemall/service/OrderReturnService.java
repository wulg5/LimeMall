package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.dto.ReturnApplyDTO;
import com.limemall.dto.ReturnHandleDTO;
import com.limemall.entity.OmsOrder;
import com.limemall.entity.OmsOrderReturn;
import com.limemall.entity.UmsMember;
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
 * 1. 待发货、已发货或确认收货后 7 天内可以申请；已收货订单先确认退货入库。
 * 2. 同一订单只能同时处理一笔退款；
 * 3. 支付渠道确认后，同一事务内回补库存、回退销量、恢复优惠券并关闭订单。
 */
@Service
@RequiredArgsConstructor
public class OrderReturnService {

    private final OmsOrderReturnMapper returnMapper;
    private final UmsMemberMapper memberMapper;
    private final OrderService orderService;
    private final PointsLedgerService pointsLedgerService;

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
     * 审核通过后由退款任务处理，渠道确认前保留原订单状态。
     */
    @Transactional
    public void handle(Long id, ReturnHandleDTO dto) {
        OmsOrderReturn refund = returnMapper.selectById(id);
        if (refund == null) {
            throw new BusinessException("退款申请不存在");
        }
        OmsOrder order = orderService.lockOrder(refund.getOrderId());
        boolean approve = Boolean.TRUE.equals(dto.getApprove());
        int nextStatus = OmsOrderReturn.REJECTED;
        if (approve) {
            requireRefundable(order);
            nextStatus = order.getStatus() == OrderService.STATUS_COMPLETED
                    ? OmsOrderReturn.AWAITING_STOCK : OmsOrderReturn.PROCESSING;
        }
        int changed = returnMapper.update(null, new LambdaUpdateWrapper<OmsOrderReturn>()
                .set(OmsOrderReturn::getStatus, nextStatus)
                .set(OmsOrderReturn::getHandleNote, dto.getHandleNote())
                .set(OmsOrderReturn::getHandleTime, LocalDateTime.now())
                .eq(OmsOrderReturn::getId, id)
                .eq(OmsOrderReturn::getStatus, OmsOrderReturn.PENDING));
        if (changed == 0) {
            throw new BusinessException("该申请已处理，请勿重复操作");
        }
    }

    @Transactional
    public void confirmReturnedStock(Long id) {
        OmsOrderReturn refund = returnMapper.selectById(id);
        if (refund == null) {
            throw new BusinessException("退款申请不存在");
        }
        OmsOrder order = orderService.lockOrder(refund.getOrderId());
        if (order.getStatus() != OrderService.STATUS_COMPLETED) {
            throw new BusinessException("仅已收货订单需要确认退货入库");
        }
        int changed = returnMapper.update(null, new LambdaUpdateWrapper<OmsOrderReturn>()
                .set(OmsOrderReturn::getStatus, OmsOrderReturn.PROCESSING)
                .eq(OmsOrderReturn::getId, id)
                .eq(OmsOrderReturn::getStatus, OmsOrderReturn.AWAITING_STOCK));
        if (changed == 0) {
            throw new BusinessException("该申请不在待退货入库状态");
        }
    }

    private void requireRefundable(OmsOrder order) {
        if (order.getStatus() != OrderService.STATUS_UNSHIPPED
                && order.getStatus() != OrderService.STATUS_SHIPPED
                && order.getStatus() != OrderService.STATUS_COMPLETED) {
            throw new BusinessException("当前订单状态不允许退款");
        }
    }

    /**
     * 会员端申请退款。
     */
    @Transactional
    public void apply(Long memberId, ReturnApplyDTO dto) {
        OmsOrder order = orderService.lockOrder(dto.getOrderId());
        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException("订单不存在");
        }
        requireRefundable(order);
        if (order.getStatus() == OrderService.STATUS_COMPLETED) {
            orderService.lockProductsForOrder(order.getId());
            LocalDateTime deadline = OrderService.returnDeadline(order);
            if (deadline == null || !LocalDateTime.now().isBefore(deadline)) {
                throw new BusinessException("订单已超过确认收货后 7 天的退款申请期限");
            }
        }
        orderService.requireNoActiveRefund(order.getId());
        OmsOrderReturn ret = new OmsOrderReturn();
        ret.setOrderId(order.getId());
        ret.setOrderSn(order.getOrderSn());
        ret.setMemberId(memberId);
        ret.setReason(dto.getReason());
        ret.setDescription(dto.getDescription());
        ret.setAmount(order.getPayAmount());
        ret.setStatus(OmsOrderReturn.PENDING);
        returnMapper.insert(ret);
    }

    /** 支付渠道确认后才执行本地关单、回补与更新退款状态。 */
    @Transactional
    public void confirmRefund(OmsOrderReturn refund) {
        OmsOrder order = orderService.lockOrder(refund.getOrderId());
        int changed = returnMapper.update(null, new LambdaUpdateWrapper<OmsOrderReturn>()
                .set(OmsOrderReturn::getStatus, OmsOrderReturn.REFUNDED)
                .eq(OmsOrderReturn::getId, refund.getId())
                .eq(OmsOrderReturn::getStatus, OmsOrderReturn.PROCESSING));
        if (changed == 0) {
            return;
        }
        // 已关闭的迟到付款订单在原关单时已经回补过。
        if (order.getStatus() != OrderService.STATUS_CLOSED) {
            requireRefundable(order);
            pointsLedgerService.revokeForRefund(order);
            orderService.closeOrder(order);
        }
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

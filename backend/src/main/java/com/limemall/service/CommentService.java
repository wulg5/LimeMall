package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.dto.CommentSubmitDTO;
import com.limemall.entity.OmsOrder;
import com.limemall.entity.OmsOrderItem;
import com.limemall.entity.PmsComment;
import com.limemall.entity.PmsProduct;
import com.limemall.entity.UmsMember;
import com.limemall.mapper.OmsOrderItemMapper;
import com.limemall.mapper.OmsOrderMapper;
import com.limemall.mapper.PmsCommentMapper;
import com.limemall.mapper.PmsProductMapper;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.vo.CommentVO;
import com.limemall.vo.ProductRatingVO;
import com.limemall.vo.PublicCommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品评价服务。
 *
 * 业务规则：
 * 1. 只有「已完成（确认收货）」订单可以评价，且订单必须归属当前会员；
 * 2. 同一订单同一商品仅可评价一次（数据库唯一键 + 服务层幂等跳过）；
 * 3. 昵称与等级在评价时快照，前台纯文本渲染（防 XSS）。
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final PmsCommentMapper commentMapper;
    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper itemMapper;
    private final UmsMemberMapper memberMapper;
    private final PmsProductMapper productMapper;

    /**
     * 提交评价（按订单批量逐商品，已评价的自动跳过）。
     */
    @Transactional
    public int submit(Long memberId, CommentSubmitDTO dto) {
        OmsOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != OrderService.STATUS_COMPLETED) {
            throw new BusinessException("仅已完成（确认收货）的订单可以评价");
        }
        Set<Long> orderProductIds = itemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                        .eq(OmsOrderItem::getOrderId, order.getId()))
                .stream().map(OmsOrderItem::getProductId).collect(Collectors.toSet());
        UmsMember member = memberMapper.selectById(memberId);

        int added = 0;
        for (CommentSubmitDTO.Item item : dto.getComments()) {
            if (!orderProductIds.contains(item.getProductId())) {
                throw new BusinessException("存在不属于该订单的商品，请刷新后重试");
            }
            Long exists = commentMapper.selectCount(new LambdaQueryWrapper<PmsComment>()
                    .eq(PmsComment::getOrderId, order.getId())
                    .eq(PmsComment::getProductId, item.getProductId()));
            if (exists > 0) {
                continue;
            }
            PmsComment comment = new PmsComment();
            comment.setProductId(item.getProductId());
            comment.setOrderId(order.getId());
            comment.setMemberId(memberId);
            comment.setMemberNickname(member == null ? "" : member.getNickname());
            comment.setMemberLevel(member == null || member.getLevel() == null ? 1 : member.getLevel());
            comment.setRating(item.getRating());
            comment.setContent(item.getContent().trim());
            commentMapper.insert(comment);
            added++;
        }
        if (added == 0) {
            throw new BusinessException("该订单商品均已评价");
        }
        return added;
    }

    /**
     * 商品公开评价分页（前台）。
     */
    public PageResult<PublicCommentVO> productComments(Long productId, long pageNum, long pageSize) {
        Page<PmsComment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<PmsComment>()
                        .eq(PmsComment::getProductId, productId)
                        .orderByDesc(PmsComment::getId));
        List<PublicCommentVO> records = page.getRecords().stream().map(comment -> {
            PublicCommentVO vo = new PublicCommentVO();
            vo.setId(comment.getId());
            vo.setMemberNickname(comment.getMemberNickname());
            vo.setMemberLevel(comment.getMemberLevel());
            vo.setRating(comment.getRating());
            vo.setContent(comment.getContent());
            vo.setCreateTime(comment.getCreateTime());
            return vo;
        }).toList();
        return PageResult.of(page, records);
    }

    /**
     * 全部商品评分聚合（供商品列表批量展示）。
     */
    public Map<Long, ProductRatingVO> allRatings() {
        return commentMapper.selectAllRatings().stream()
                .collect(Collectors.toMap(ProductRatingVO::getProductId, Function.identity()));
    }

    /**
     * 单个商品评分聚合（供商品详情）。
     */
    public ProductRatingVO ratingOf(Long productId) {
        ProductRatingVO rating = commentMapper.selectRatingByProduct(productId);
        if (rating == null) {
            rating = new ProductRatingVO();
            rating.setProductId(productId);
            rating.setCommentCount(0L);
        }
        return rating;
    }

    /**
     * 管理端分页查询（含商品名）。
     */
    public PageResult<CommentVO> adminPage(long pageNum, long pageSize) {
        Page<PmsComment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<PmsComment>().orderByDesc(PmsComment::getId));
        List<PmsComment> comments = page.getRecords();
        if (comments.isEmpty()) {
            return PageResult.of(page, new ArrayList<>());
        }
        Map<Long, String> productNames = productMapper.selectBatchIds(
                        comments.stream().map(PmsComment::getProductId).distinct().toList())
                .stream().collect(Collectors.toMap(PmsProduct::getId, PmsProduct::getName));
        List<CommentVO> records = comments.stream().map(comment -> {
            CommentVO vo = new CommentVO();
            vo.setId(comment.getId());
            vo.setProductId(comment.getProductId());
            vo.setProductName(productNames.getOrDefault(comment.getProductId(), "（商品已删除）"));
            vo.setOrderId(comment.getOrderId());
            vo.setMemberNickname(comment.getMemberNickname());
            vo.setMemberLevel(comment.getMemberLevel());
            vo.setRating(comment.getRating());
            vo.setContent(comment.getContent());
            vo.setCreateTime(comment.getCreateTime());
            return vo;
        }).toList();
        return PageResult.of(page, records);
    }

    /**
     * 管理端删除评价（内容违规处理）。
     */
    public void delete(Long id) {
        if (commentMapper.selectById(id) == null) {
            throw new BusinessException("评价不存在");
        }
        commentMapper.deleteById(id);
    }
}

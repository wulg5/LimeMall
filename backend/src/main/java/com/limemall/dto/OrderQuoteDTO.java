package com.limemall.dto;

import com.limemall.dto.OrderCreateDTO.Item;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderQuoteDTO {
    @NotEmpty(message = "请选择要购买的商品")
    private List<@NotNull @Valid Item> items;

    private Long couponMemberId;
}

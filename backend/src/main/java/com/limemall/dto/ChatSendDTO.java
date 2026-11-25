package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送聊天消息请求参数。
 */
@Data
public class ChatSendDTO {

    @NotBlank(message = "消息不能为空")
    @Size(max = 500, message = "消息不能超过 500 字")
    private String content;
}

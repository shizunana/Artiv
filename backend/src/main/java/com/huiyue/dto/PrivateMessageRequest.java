package com.huiyue.dto;

import lombok.Data;

/** 发送 1v1 私信请求；发送者由 JWT 上下文确定。 */
@Data
public class PrivateMessageRequest {

    private Long toId;

    /** 可选：应征沟通关联的企划。 */
    private Long projectId;

    private Long orderId;

    private String content;
}

package com.artiv.dto;

import lombok.Data;

import java.util.List;

/** 发送 1v1 私信请求；发送者由 JWT 上下文确定。 */
@Data
public class PrivateMessageRequest {

    private Long toId;

    /** 可选：应征沟通关联的企划。 */
    private Long projectId;

    private Long orderId;

    private String content;

    /**
     * 图片消息的已上传地址；与 content 二选一。
     * 每个地址必须来自 /api/v1/upload，服务层会限制为 /uploads/ 下的 1-9 张图片。
     */
    private List<String> images;
}

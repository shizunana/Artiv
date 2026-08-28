package com.artiv.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderStageSubmitRequest {
    private String content;

    /** 画师本次提交的原图，按上传顺序保存；单次最多 9 张。 */
    private List<String> images;

    /** 兼容旧客户端的单图字段，新客户端应使用 images。 */
    private String image;
}

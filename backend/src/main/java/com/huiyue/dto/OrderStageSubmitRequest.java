package com.huiyue.dto;

import lombok.Data;

@Data
public class OrderStageSubmitRequest {
    private String content;
    /** 画师本次提交的唯一原图；服务端据此生成低分辨率水印预览。 */
    private String image;
}

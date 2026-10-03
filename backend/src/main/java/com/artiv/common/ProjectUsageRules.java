package com.artiv.common;

/** 稿件用途仅记录约定，不参与价格计算、招募模式或订单状态流转。 */
public final class ProjectUsageRules {
    private ProjectUsageRules() {}

    public static String require(String value) {
        if (!"personal".equals(value) && !"commercial".equals(value)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "稿件用途只能为个人用途或商业用途");
        }
        return value;
    }
}

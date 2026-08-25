package com.artiv.common;

import java.time.LocalDate;

/**
 * 企划时间统一规则：截稿日期当天仍可招募，次日起才视为过期。
 */
public final class ProjectTimeRules {

    public static final int MIN_DEADLINE_DAYS = 2;

    private ProjectTimeRules() {
    }

    public static LocalDate minimumDeadline() {
        return LocalDate.now().plusDays(MIN_DEADLINE_DAYS);
    }

    public static void requireEditableDeadline(LocalDate deadline) {
        if (deadline == null || deadline.isBefore(minimumDeadline())) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(),
                    "截稿日期不能早于当前日期 2 天后");
        }
    }

    public static boolean isExpired(LocalDate deadline) {
        return deadline == null || deadline.isBefore(LocalDate.now());
    }
}

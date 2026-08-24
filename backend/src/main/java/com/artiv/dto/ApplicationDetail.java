package com.artiv.dto;

import com.artiv.entity.Application;
import com.artiv.entity.Work;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationDetail {

    private Application application;

    /** 定向邀请尚未形成应征时用于稳定标识列表项。 */
    private Long invitationId;

    /** 定向邀请状态：0 待处理、1 已接受；拒绝后不再进入应征画师列表。 */
    private Integer invitationStatus;

    private ApplicantPainterSummary painter;

    private List<Work> works;

    private Boolean hasActiveOrder;

    private Long activeOrderId;
}

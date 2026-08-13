package com.huiyue.dto;

import com.huiyue.entity.OrderEvent;
import com.huiyue.entity.OrderInfo;
import com.huiyue.entity.OrderStage;
import lombok.Data;

import java.util.List;

@Data
public class OrderDetailResponse {
    private OrderInfo order;
    private String employerName;
    private String painterName;
    private String viewerRole;
    private List<OrderStage> submissions;
    private List<OrderEvent> events;
}

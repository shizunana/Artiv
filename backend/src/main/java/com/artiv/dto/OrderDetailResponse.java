package com.artiv.dto;

import com.artiv.entity.OrderEvent;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.OrderStage;
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

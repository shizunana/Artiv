package com.artiv.service;

import com.artiv.dto.admin.AdminArbitrationRequest;
import com.artiv.dto.admin.DisputeCreateRequest;
import com.artiv.dto.admin.DisputeEvidenceRequest;
import com.artiv.entity.OrderDispute;

import java.util.Map;

public interface DisputeService {
    OrderDispute create(Long orderId, Long userId, DisputeCreateRequest request);
    Map<String, Object> current(Long orderId, Long userId);
    Map<String, Object> addEvidence(Long disputeId, Long userId, DisputeEvidenceRequest request);
    Map<String, Object> listAdmin(long page, long size, Integer status, String keyword);
    Map<String, Object> detailAdmin(Long id);
    Map<String, Object> arbitrate(Long adminId, Long id, AdminArbitrationRequest request);
}

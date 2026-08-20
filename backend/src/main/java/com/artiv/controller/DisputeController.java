package com.artiv.controller;

import com.artiv.common.Result;
import com.artiv.dto.admin.DisputeCreateRequest;
import com.artiv.dto.admin.DisputeEvidenceRequest;
import com.artiv.entity.OrderDispute;
import com.artiv.security.UserContext;
import com.artiv.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DisputeController {
    private final DisputeService disputeService;

    @PostMapping("/orders/{orderId}/disputes")
    public Result<OrderDispute> create(@PathVariable Long orderId,
                                       @Valid @RequestBody DisputeCreateRequest request) {
        return Result.ok(disputeService.create(orderId, UserContext.getUserId(), request));
    }

    @GetMapping("/orders/{orderId}/disputes/current")
    public Result<Map<String, Object>> current(@PathVariable Long orderId) {
        return Result.ok(disputeService.current(orderId, UserContext.getUserId()));
    }

    @PostMapping("/disputes/{id}/evidence")
    public Result<Map<String, Object>> evidence(@PathVariable Long id,
                                                @Valid @RequestBody DisputeEvidenceRequest request) {
        return Result.ok(disputeService.addEvidence(id, UserContext.getUserId(), request));
    }
}

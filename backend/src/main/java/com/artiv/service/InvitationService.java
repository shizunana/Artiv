package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.dto.InvitationAcceptRequest;
import com.artiv.dto.InvitationProjectSummary;
import com.artiv.dto.InvitationRejectRequest;
import com.artiv.dto.PublicInvitationCreateRequest;
import com.artiv.entity.Invitation;

public interface InvitationService {

    Invitation createPublic(PublicInvitationCreateRequest request, Long employerId);

    Page<InvitationProjectSummary> listMine(long page, long size, Long userId, Integer status);

    InvitationProjectSummary getMineByProject(Long projectId, Long userId);

    Invitation accept(Long id, InvitationAcceptRequest request, Long painterId);

    Invitation reject(Long id, InvitationRejectRequest request, Long painterId);

    void hideReminder(Long id, Long painterId);
}

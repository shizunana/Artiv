package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.dto.InvitationRequest;
import com.artiv.entity.Invitation;

public interface InvitationService {

    Page<Invitation> listMine(long page, long size, Long userId);

    Invitation create(InvitationRequest request, Long employerId);

    Invitation respond(Long id, Integer status, Long painterId);
}

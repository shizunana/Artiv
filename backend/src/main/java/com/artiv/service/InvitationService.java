package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.entity.Invitation;

public interface InvitationService {

    Page<Invitation> listMine(long page, long size, Long userId);

    Invitation create(Invitation invitation);

    Invitation respond(Long id, Integer status, Long painterId);
}

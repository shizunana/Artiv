package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.entity.Invitation;
import com.huiyue.mapper.InvitationMapper;
import com.huiyue.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final InvitationMapper invitationMapper;

    @Override
    public Page<Invitation> listMine(long page, long size, Long userId) {
        LambdaQueryWrapper<Invitation> wrapper = new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getPainterId, userId)
                .eq(Invitation::getDeleted, 0)
                .orderByDesc(Invitation::getCreatedAt);
        return invitationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Invitation create(Invitation invitation) {
        invitation.setId(null);
        invitation.setStatus(0);
        invitation.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        invitation.setCreatedAt(now);
        invitation.setUpdatedAt(now);
        invitationMapper.insert(invitation);
        return invitation;
    }

    @Override
    public Invitation respond(Long id, Integer status, Long painterId) {
        Invitation invitation = invitationMapper.selectById(id);
        if (invitation == null || !invitation.getPainterId().equals(painterId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        if (invitation.getStatus() != 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID);
        }
        invitation.setStatus(status);
        invitationMapper.updateById(invitation);
        return invitation;
    }
}

package com.artiv.service;

import com.artiv.dto.admin.CertificationSubmitRequest;
import com.artiv.entity.ArtistCertification;
import com.artiv.entity.User;
import com.artiv.mapper.ArtistCertificationMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.impl.CertificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CertificationServiceTest {
    @Mock ArtistCertificationMapper certificationMapper;
    @Mock UserMapper userMapper;
    @Mock WorkMapper workMapper;
    @Mock MessageService messageService;
    @Mock AdminAuditService auditService;
    @InjectMocks CertificationServiceImpl certificationService;

    @Test
    void submitRequiresProfileAndStoresApplicationSnapshot() {
        User painter = new User();
        painter.setId(8L);
        painter.setRole(2);
        painter.setCertStatus(0);
        painter.setBio("擅长人物与场景插画");
        when(userMapper.selectByIdForUpdate(8L)).thenReturn(painter);
        when(workMapper.selectCount(any())).thenReturn(4L);

        CertificationSubmitRequest request = new CertificationSubmitRequest();
        request.setCommonId("Artist-8");
        request.setCommonSite("https://example.com/artist-8");
        ArtistCertification result = certificationService.submit(8L, request);

        assertEquals(0, result.getStatus());
        assertEquals(4, result.getPublicWorkCount());
        assertEquals(1, painter.getCertStatus());
        verify(certificationMapper).insert(result);
        verify(userMapper).updateById(painter);
    }

    @Test
    void submitRejectsPainterWithFewerThanFourPublicWorks() {
        User painter = new User();
        painter.setId(8L);
        painter.setRole(2);
        painter.setCertStatus(0);
        painter.setBio("擅长人物与场景插画");
        when(userMapper.selectByIdForUpdate(8L)).thenReturn(painter);
        when(workMapper.selectCount(any())).thenReturn(3L);
        CertificationSubmitRequest request = new CertificationSubmitRequest();
        request.setCommonId("Artist-8");

        assertThrows(com.artiv.common.BusinessException.class,
                () -> certificationService.submit(8L, request));

        verify(certificationMapper, never()).insert(any(ArtistCertification.class));
        verify(userMapper, never()).updateById(any(User.class));
    }
}

package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.dto.CertificationChangeRequest;
import com.artiv.dto.admin.CertificationSubmitRequest;
import com.artiv.entity.ArtistCertification;
import com.artiv.entity.User;
import com.artiv.mapper.ArtistCertificationMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.impl.CertificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationServiceTest {
    @Mock ArtistCertificationMapper certificationMapper;
    @Mock UserMapper userMapper;
    @Mock WorkMapper workMapper;
    @Mock MessageService messageService;
    @Mock AdminAuditService auditService;
    @InjectMocks CertificationServiceImpl certificationService;

    @Test
    void submitRequiresProfileAndStoresInitialApplicationSnapshot() {
        User applicant = applicant(8L, 0);
        when(userMapper.selectByIdForUpdate(8L)).thenReturn(applicant);
        when(workMapper.selectCount(any())).thenReturn(4L);

        ArtistCertification result = certificationService.submit(8L,
                initialRequest(" Artist-8 ", "https://www.pixiv.net/users/8"));

        assertEquals(0, result.getApplicationType());
        assertEquals(0, result.getStatus());
        assertEquals(4, result.getPublicWorkCount());
        assertEquals("Artist-8", result.getCommonId());
        assertEquals(1, applicant.getCertStatus());
        verify(certificationMapper).insert(result);
        verify(userMapper).updateById(applicant);
    }

    @Test
    void submitRejectsApplicantWithFewerThanFourPublicWorks() {
        User applicant = applicant(8L, 0);
        when(userMapper.selectByIdForUpdate(8L)).thenReturn(applicant);
        when(workMapper.selectCount(any())).thenReturn(3L);

        assertThrows(BusinessException.class, () -> certificationService.submit(8L,
                initialRequest("Artist-8", "https://www.pixiv.net/users/8")));

        verify(certificationMapper, never()).insert(any(ArtistCertification.class));
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void submitRejectsUnsupportedOrPlatformRootSite() {
        User applicant = applicant(8L, 0);
        when(userMapper.selectByIdForUpdate(8L)).thenReturn(applicant);
        when(workMapper.selectCount(any())).thenReturn(4L);

        assertThrows(BusinessException.class, () -> certificationService.submit(8L,
                initialRequest("Artist-8", "https://www.pixiv.net/")));
        verify(certificationMapper, never()).insert(any(ArtistCertification.class));
    }

    @Test
    void certifiedUserChangeDoesNotOverwriteEffectiveDataBeforeReview() {
        User applicant = applicant(8L, 2);
        applicant.setArtistName("OldName");
        applicant.setArtistSite("https://www.pixiv.net/users/8");
        when(userMapper.selectByIdForUpdate(8L)).thenReturn(applicant);
        when(workMapper.selectCount(any())).thenReturn(6L);

        CertificationChangeRequest request = new CertificationChangeRequest();
        request.setCommonId("NewName");
        request.setCommonSite("https://space.bilibili.com/800");
        ArtistCertification result = certificationService.submitChange(8L, request);

        assertEquals(1, result.getApplicationType());
        assertEquals("OldName", result.getPreviousCommonId());
        assertEquals("https://www.pixiv.net/users/8", result.getPreviousCommonSite());
        assertEquals("OldName", applicant.getArtistName());
        assertEquals("https://www.pixiv.net/users/8", applicant.getArtistSite());
        verify(certificationMapper).insert(result);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void approvingChangeReplacesEffectiveDataAndKeepsCertification() {
        User applicant = applicant(8L, 2);
        applicant.setArtistName("OldName");
        applicant.setArtistSite("https://www.pixiv.net/users/8");
        ArtistCertification change = pending(91L, 8L, 1,
                "NewName", "https://space.bilibili.com/800");
        when(certificationMapper.selectByIdForUpdate(91L)).thenReturn(change);
        when(userMapper.selectByIdForUpdate(8L)).thenReturn(applicant);

        ArtistCertification decided = certificationService.decide(3L, 91L, true, "资料可确认");

        assertEquals(1, decided.getStatus());
        assertEquals(2, applicant.getCertStatus());
        assertEquals("NewName", applicant.getArtistName());
        assertEquals("https://space.bilibili.com/800", applicant.getArtistSite());
        verify(messageService).createNotification(eq(8L), eq(0),
                eq("CERTIFICATION_PROFILE_CHANGE_APPROVED"), eq(null), any(String.class));
        verify(auditService).record(eq(3L), eq("APPROVE_CERTIFICATION"),
                eq("ARTIST_CERTIFICATION"), eq(91L), any(), eq(change), eq("资料可确认"));
    }

    @Test
    void rejectingInitialApplicationReturnsUserToUnverified() {
        User applicant = applicant(8L, 1);
        ArtistCertification initial = pending(92L, 8L, 0,
                "Artist-8", "https://www.pixiv.net/users/8");
        when(certificationMapper.selectByIdForUpdate(92L)).thenReturn(initial);
        when(userMapper.selectByIdForUpdate(8L)).thenReturn(applicant);

        certificationService.decide(3L, 92L, false, "无法核实身份");

        assertEquals(2, initial.getStatus());
        assertEquals(0, applicant.getCertStatus());
        assertNull(applicant.getArtistName());
        verify(messageService).createNotification(eq(8L), eq(0),
                eq("CERTIFICATION_REJECTED"), eq(null), any(String.class));
    }

    @Test
    void approvingInitialApplicationRechecksCurrentPublicWorks() {
        User applicant = applicant(8L, 1);
        ArtistCertification initial = pending(93L, 8L, 0,
                "Artist-8", "https://www.pixiv.net/users/8");
        when(certificationMapper.selectByIdForUpdate(93L)).thenReturn(initial);
        when(userMapper.selectByIdForUpdate(8L)).thenReturn(applicant);
        when(workMapper.selectCount(any())).thenReturn(3L);

        assertThrows(BusinessException.class,
                () -> certificationService.decide(3L, 93L, true, "同意"));

        verify(certificationMapper, never()).updateById(any(ArtistCertification.class));
        verify(userMapper, never()).updateById(any(User.class));
        verify(messageService, never()).createNotification(any(), any(), any(), any(), any());
    }

    private User applicant(Long id, int certStatus) {
        User user = new User();
        user.setId(id);
        user.setRole(1);
        user.setStatus(0);
        user.setDeleted(0);
        user.setCertStatus(certStatus);
        user.setBio("擅长人物与场景插画");
        return user;
    }

    private CertificationSubmitRequest initialRequest(String commonId, String site) {
        CertificationSubmitRequest request = new CertificationSubmitRequest();
        request.setCommonId(commonId);
        request.setCommonSite(site);
        return request;
    }

    private ArtistCertification pending(Long id, Long userId, int type, String commonId, String commonSite) {
        ArtistCertification certification = new ArtistCertification();
        certification.setId(id);
        certification.setUserId(userId);
        certification.setApplicationType(type);
        certification.setCommonId(commonId);
        certification.setCommonSite(commonSite);
        certification.setStatus(0);
        certification.setDeleted(0);
        return certification;
    }
}

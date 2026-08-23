package com.artiv.service;

import com.artiv.dto.CertificationChangeRequest;
import com.artiv.dto.CertificationStatusResponse;
import com.artiv.dto.admin.CertificationSubmitRequest;
import com.artiv.entity.ArtistCertification;

import java.util.Map;

public interface CertificationService {
    ArtistCertification submit(Long userId, CertificationSubmitRequest request);
    ArtistCertification submitChange(Long userId, CertificationChangeRequest request);
    CertificationStatusResponse status(Long userId);
    ArtistCertification latest(Long userId);
    Map<String, Object> listAdmin(long page, long size, Integer status, Integer applicationType, String keyword);
    Map<String, Object> detailAdmin(Long id);
    ArtistCertification decide(Long adminId, Long id, boolean approved, String reason);
}

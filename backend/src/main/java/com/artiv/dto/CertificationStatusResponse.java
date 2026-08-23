package com.artiv.dto;

import com.artiv.entity.ArtistCertification;
import lombok.Data;

/** 当前用户的画师认证状态、正式资料和申请资格。 */
@Data
public class CertificationStatusResponse {
    private Integer certStatus;
    private String artistName;
    private String artistSite;
    private String painterBio;
    private String tags;
    private long publicWorkCount;
    private boolean painterBioCompleted;
    private boolean eligibleForInitialApplication;
    private ArtistCertification pendingInitialApplication;
    private ArtistCertification pendingChangeApplication;
}

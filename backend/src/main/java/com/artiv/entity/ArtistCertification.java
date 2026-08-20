package com.artiv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("artist_certification")
public class ArtistCertification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String commonId;
    private String commonSite;
    private Integer publicWorkCount;
    /** 0 待审核、1 通过、2 驳回 */
    private Integer status;
    private Long reviewerId;
    private String reviewReason;
    private LocalDateTime reviewedAt;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

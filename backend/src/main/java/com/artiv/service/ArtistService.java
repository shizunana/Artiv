package com.artiv.service;

import com.artiv.dto.ArtistSummary;
import com.artiv.entity.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface ArtistService {

    Page<ArtistSummary> listArtists(long page, long size, String category, String subjects, String keyword);

    /** 向导专用：类型取公开作品与当前在售橱窗并集，题材仍仅匹配个人标签。 */
    Page<ArtistSummary> listGuideArtists(long page, long size, String category, String subjects, String keyword);

    List<ArtistSummary> buildGuideSummaries(List<User> painters);

    List<ArtistSummary> buildSummaries(List<User> painters);
}

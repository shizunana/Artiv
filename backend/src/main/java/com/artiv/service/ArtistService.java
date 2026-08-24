package com.artiv.service;

import com.artiv.dto.ArtistSummary;
import com.artiv.entity.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface ArtistService {

    Page<ArtistSummary> listArtists(long page, long size, String category, String subjects, String keyword);

    List<ArtistSummary> buildSummaries(List<User> painters);
}

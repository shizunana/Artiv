package com.artiv.service;

import com.artiv.dto.ArtistSummary;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface ArtistService {

    Page<ArtistSummary> listArtists(long page, long size, String category, String subjects, String keyword);
}

package com.huiyue.service;

import com.huiyue.dto.ArtistSummary;

import java.util.List;

public interface ArtistService {

    List<ArtistSummary> listArtists(String category, String subjects, String keyword);
}

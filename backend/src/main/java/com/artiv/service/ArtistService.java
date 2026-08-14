package com.artiv.service;

import com.artiv.dto.ArtistSummary;

import java.util.List;

public interface ArtistService {

    List<ArtistSummary> listArtists(String category, String subjects, String keyword);
}

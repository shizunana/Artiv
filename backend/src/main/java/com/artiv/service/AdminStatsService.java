package com.artiv.service;

import java.time.LocalDate;
import java.util.Map;

public interface AdminStatsService {
    Map<String, Object> overview();
    Map<String, Object> operations(LocalDate startDate, LocalDate endDate);
    Map<String, Object> demand(LocalDate startDate, LocalDate endDate);
}

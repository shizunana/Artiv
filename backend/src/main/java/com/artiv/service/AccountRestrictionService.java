package com.artiv.service;

import com.artiv.entity.User;

public interface AccountRestrictionService {
    User refresh(Long userId);
    void requireAvailable(Long userId, String actionName);
    int releaseExpiredBans();
}

package com.artiv.service;

import com.artiv.config.GuideConfig;
import com.artiv.dto.guide.GuideCapabilities;
import com.artiv.guide.client.GuideModelRouter;
import org.springframework.stereotype.Service;

@Service
public class GuideService {
    private final GuideConfig config;
    private final GuideModelRouter router;

    public GuideService(GuideConfig config, GuideModelRouter router) {
        this.config = config;
        this.router = router;
    }

    public GuideCapabilities capabilities() {
        boolean configured = router.supported()
                && config.getApiKey() != null && !config.getApiKey().isBlank();
        return new GuideCapabilities(config.isEnabled(), config.isEnabled() && configured,
                config.isEnabled() && configured, false, config.getProvider(), config.getTextModel(), Math.min(4000, config.getMaxInputChars()));
    }
}

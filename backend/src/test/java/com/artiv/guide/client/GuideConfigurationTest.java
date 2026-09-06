package com.artiv.guide.client;

import com.artiv.config.GuideConfig;
import com.artiv.service.GuideService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;

class GuideConfigurationTest {
    @Test void beansStartWithDefaultOffAndWithoutKeyWhenEnabled() {
        var runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
                .withUserConfiguration(GuideConfig.class, GuideService.class, DeepSeekGuideModelClient.class, GuideModelRouter.class)
                .withBean(ObjectMapper.class);
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertFalse(context.getBean(GuideService.class).capabilities().enabled());
            assertNotNull(context.getBean(GuideModelClient.class));
        });
        runner.withPropertyValues("artiv.guide.enabled=true", "artiv.guide.api-key=").run(context -> {
            assertNull(context.getStartupFailure());
            var capabilities = context.getBean(GuideService.class).capabilities();
            assertTrue(capabilities.enabled());
            assertFalse(capabilities.modelConfigured());
        });
    }
}

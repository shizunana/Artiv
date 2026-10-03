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

    @Test void flashModelEnablesChatAndReferenceImages() {
        var config = new GuideConfig();
        config.setEnabled(true);
        config.setApiKey("test-key");
        config.setTextModel("deepseek-flash");
        config.setVisionModel("deepseek-flash");
        var client = new DeepSeekGuideModelClient(config, new ObjectMapper());
        var router = new GuideModelRouter(config, java.util.List.of(client));
        var capabilities = new GuideService(config, router).capabilities();
        assertTrue(capabilities.chatEnabled());
        assertTrue(capabilities.imageUploadEnabled());
        assertEquals("deepseek-flash", capabilities.textModel());
        config.setVisionModel("deepseek-unknown");
        assertFalse(new GuideService(config, router).capabilities().imageUploadEnabled());
    }
}

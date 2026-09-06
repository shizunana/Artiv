package com.artiv.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 向导默认关闭；密钥仅由后端环境注入，不生成包含密钥的 toString。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "artiv.guide")
public class GuideConfig {
    private boolean enabled = false;
    private String provider = "deepseek";
    private String apiKey = "";
    private String textModel = "deepseek-v4-flash-vision-exp";
    private String visionModel = "deepseek-v4-flash-vision-exp";
    private int timeoutSeconds = 90;
    private int maxOutputTokens = 2048;
    private int maxInputChars = 4000;
    private int maxContextChars = 24000;
    private int maxTurns = 50;
    private int maxSessions = 100;
    private int maxSessionsPerUser = 5;
    private int sessionTtlMinutes = 1440;
}

package com.artiv.dto.guide;

/** 仅返回可公开的阶段状态；配置密钥不等于模型已验证。 */
public record GuideCapabilities(boolean enabled, boolean modelConfigured,
                                boolean chatEnabled, boolean imageUploadEnabled,
                                String provider, String textModel, int maxInputChars) { }

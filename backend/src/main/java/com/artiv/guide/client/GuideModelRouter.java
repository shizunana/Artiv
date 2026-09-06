package com.artiv.guide.client;

import com.artiv.config.GuideConfig;
import org.springframework.stereotype.Component;
import java.util.List;

/** 新供应商实现 GuideModelClient 并注册为 Bean 即可接入，业务与 UI 无需依赖供应商协议。 */
@Component
public class GuideModelRouter {
    private final GuideConfig config;
    private final List<GuideModelClient> clients;
    public GuideModelRouter(GuideConfig config, List<GuideModelClient> clients) {
        this.config = config;
        this.clients = clients;
    }
    public boolean supported() { return clients.stream().anyMatch(c -> c.provider().equals(config.getProvider())); }
    public GuideModelClient selected() {
        return clients.stream().filter(c -> c.provider().equals(config.getProvider())).findFirst()
                .orElseThrow(() -> new GuideModelException("该供应商仅预留接口，尚未接入；请使用已配置的 DeepSeek"));
    }
}

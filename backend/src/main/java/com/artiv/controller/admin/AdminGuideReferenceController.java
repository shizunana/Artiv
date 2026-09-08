package com.artiv.controller.admin;

import com.artiv.common.BusinessException;
import com.artiv.common.Result;
import com.artiv.guide.session.GuideSessionStore;
import com.artiv.guide.tool.GuidePublicReader;
import com.artiv.security.UserContext;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** 用户点击来源时复验：必须是本人会话中实际返回过的公开资源，且当前仍可见。 */
@RestController
@RequestMapping("/api/v1/admin/guide/sessions")
public class AdminGuideReferenceController {
    private final GuideSessionStore sessions;
    private final GuidePublicReader reader;
    public AdminGuideReferenceController(GuideSessionStore sessions, GuidePublicReader reader) { this.sessions = sessions; this.reader = reader; }
    @GetMapping("/{sessionId}/references/{kind}/{id}")
    public Result<Map<String, String>> resolve(@PathVariable String sessionId, @PathVariable String kind, @PathVariable String id) {
        var session = sessions.get(UserContext.getUserId(), sessionId);
        boolean found = session.messages().stream().flatMap(m -> m.references().stream())
                .anyMatch(r -> r.kind().equals(kind) && r.id().equals(id));
        if (!found || !id.matches("[1-9][0-9]{0,18}") || !java.util.Set.of("project", "stall", "artist").contains(kind)) throw unavailable();
        long number;
        try { number = Long.parseLong(id); } catch (NumberFormatException e) { throw unavailable(); }
        reader.requireVisible(kind, number);
        return Result.ok(Map.of("kind", kind, "id", id));
    }
    private BusinessException unavailable() { return new BusinessException(404, "引用不存在或已失效，请重新查询"); }
}

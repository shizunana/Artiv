package com.artiv.guide.session;

import com.artiv.common.BusinessException;
import com.artiv.config.GuideConfig;
import com.artiv.guide.client.GuideCancellation;
import com.artiv.guide.client.GuideModelClient;
import com.artiv.guide.tool.GuideReference;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.*;

/** 单机测试阶段的有界内存存储；只保存向导数据，重启后清空。 */
@Component
public class GuideSessionStore {
    public record Message(String id, String role, String text, String status, String error, GuideModelClient.Usage usage, List<GuideReference> references) { }
    public record Snapshot(String id, List<Message> messages, String activeMessageId, long expiresAt) { }
    public record Run(long ownerId, String sessionId, String clientMessageId, String assistantId,
                      List<GuideModelClient.Message> context, GuideCancellation cancellation) { }
    private static class Session {
        final String id = UUID.randomUUID().toString();
        final long owner;
        final List<Message> messages = new ArrayList<>();
        final Set<String> usedIds = new HashSet<>();
        long touched;
        Run active;
        Session(long owner, long now) { this.owner = owner; touched = now; }
    }
    private final GuideConfig config;
    private final Clock clock;
    private final Map<String, Session> sessions = new HashMap<>();
    @org.springframework.beans.factory.annotation.Autowired
    public GuideSessionStore(GuideConfig config) { this(config, Clock.systemUTC()); }
    public GuideSessionStore(GuideConfig config, Clock clock) { this.config = config; this.clock = clock; }

    public Snapshot create(long owner) {
        cleanup();
        synchronized (this) {
        if (sessions.size() >= Math.min(100, config.getMaxSessions())
                || sessions.values().stream().filter(s -> s.owner == owner).count() >= Math.min(5, config.getMaxSessionsPerUser())) {
            throw new BusinessException(429, "会话数量已达上限，请清空旧会话或等待过期");
        }
        var session = new Session(owner, clock.millis());
        sessions.put(session.id, session);
        return snapshot(session);
        }
    }
    public synchronized Snapshot get(long owner, String id) { return snapshot(owned(owner, id)); }
    public void delete(long owner, String id) {
        Run active;
        synchronized (this) {
            var session = owned(owner, id);
            active = session.active;
            sessions.remove(id);
        }
        if (active != null) active.cancellation().cancel("cleared");
    }
    public synchronized Run begin(long owner, String id, String clientId, String text, String systemPrompt) {
        var session = owned(owner, id);
        if (text == null || text.isBlank() || text.length() > Math.min(4000, config.getMaxInputChars())) throw new BusinessException(400, "消息为空或过长");
        if (session.usedIds.contains(clientId)) throw new BusinessException(409, "该消息已处理，请读取会话历史；不会重复调用模型");
        if (sessions.values().stream().anyMatch(s -> s.owner == owner && s.active != null)) throw new BusinessException(409, "上一条回答仍在生成，请先停止或等待完成");
        if (session.usedIds.size() >= Math.min(50, config.getMaxTurns())) throw new BusinessException(429, "当前会话已达 50 轮，请清空后开始新会话");
        var context = new ArrayList<GuideModelClient.Message>();
        int size = text.length() + systemPrompt.length();
        // 只选完整问答对；取消、失败和截断的输出不进入可信对话上下文。
        for (int i = session.messages.size() - 2; i >= 0 && context.size() < 28; i -= 2) {
            var user = session.messages.get(i);
            var assistant = session.messages.get(i + 1);
            if (!"complete".equals(assistant.status())) continue;
            int pairSize = user.text().length() + assistant.text().length();
            if (size + pairSize > Math.min(24000, config.getMaxContextChars())) break;
            size += pairSize;
            context.addFirst(new GuideModelClient.Message("assistant", assistant.text(), null, null, List.of()));
            context.addFirst(GuideModelClient.Message.user(user.text()));
        }
        context.addFirst(new GuideModelClient.Message("system", systemPrompt, null, null, List.of()));
        context.add(GuideModelClient.Message.user(text.strip()));
        String assistantId = UUID.randomUUID().toString();
        var run = new Run(owner, id, clientId, assistantId, List.copyOf(context), new GuideCancellation());
        session.usedIds.add(clientId);
        session.messages.add(new Message(clientId, "user", text.strip(), "complete", null, null, List.of()));
        session.messages.add(new Message(assistantId, "assistant", "", "generating", null, null, List.of()));
        session.active = run;
        return run;
    }
    public synchronized boolean append(Run run, String delta) {
        var session = sessions.get(run.sessionId());
        if (session == null || session.active != run || run.cancellation().cancelled()) return false;
        int last = session.messages.size() - 1;
        var current = session.messages.get(last);
        if (current.text().length() + delta.length() > 32768) throw new BusinessException(429, "回答超过长度限制");
        session.messages.set(last, new Message(current.id(), current.role(), current.text() + delta, current.status(), null, null, current.references()));
        session.touched = clock.millis();
        return true;
    }
    public synchronized boolean finish(Run run, String status, String error, GuideModelClient.Usage usage) {
        var session = sessions.get(run.sessionId());
        if (session == null || session.active != run) return false;
        int last = session.messages.size() - 1;
        var current = session.messages.get(last);
        session.messages.set(last, new Message(current.id(), current.role(), current.text(), status, error, usage, current.references()));
        session.active = null;
        session.touched = clock.millis();
        return true;
    }
    public void cancel(long owner, String id, String clientId) {
        Run active;
        synchronized (this) { active = owned(owner, id).active; }
        if (active != null && active.clientMessageId().equals(clientId)) active.cancellation().cancel("cancelled");
    }
    public synchronized void references(Run run, List<GuideReference> references) {
        var session = sessions.get(run.sessionId());
        if (session == null || session.active != run || run.cancellation().cancelled()) return;
        if (references.size() > 30) throw new BusinessException(429, "引用数量超限");
        int last = session.messages.size() - 1;
        var message = session.messages.get(last);
        session.messages.set(last, new Message(message.id(), message.role(), message.text(), message.status(), message.error(), message.usage(), List.copyOf(references)));
    }
    public void cleanup() {
        List<Run> expired = new ArrayList<>();
        synchronized (this) {
        long cutoff = clock.millis() - ttl();
        sessions.values().removeIf(session -> {
            if (session.touched > cutoff) return false;
            if (session.active != null) expired.add(session.active);
            return true;
        });
        }
        expired.forEach(run -> run.cancellation().cancel("expired"));
    }
    public void shutdown() {
        List<Run> active;
        synchronized (this) {
            active = sessions.values().stream().map(s -> s.active).filter(Objects::nonNull).toList();
            sessions.clear();
        }
        active.forEach(run -> run.cancellation().cancel("shutdown"));
    }
    private Session owned(long owner, String id) {
        var session = sessions.get(id);
        if (session == null || session.owner != owner || session.touched <= clock.millis() - ttl()) throw new BusinessException(404, "会话不存在或已过期");
        session.touched = clock.millis();
        return session;
    }
    private long ttl() { return Math.max(1, Math.min(1440, config.getSessionTtlMinutes())) * 60_000L; }
    private Snapshot snapshot(Session session) { return new Snapshot(session.id, List.copyOf(session.messages), session.active == null ? null : session.active.clientMessageId(), session.touched + ttl()); }
}

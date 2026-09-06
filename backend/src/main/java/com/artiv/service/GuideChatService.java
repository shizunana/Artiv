package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.config.GuideConfig;
import com.artiv.dto.guide.GuideMessageRequest;
import com.artiv.guide.client.*;
import com.artiv.guide.session.GuideSessionStore;
import com.artiv.guide.tool.GuideAnswerRunner;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class GuideChatService {
    private final GuideConfig config;
    private final GuideModelRouter router;
    private final GuideSessionStore store;
    private final GuideAnswerRunner answers;
    // 无等待队列：最多四个真实运行；会话存储另限制每个管理员同时一个运行。
    private final ExecutorService workers = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), Thread.ofPlatform().daemon().name("guide-run-", 0).factory(), new ThreadPoolExecutor.AbortPolicy());
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("guide-timer").factory());

    public GuideChatService(GuideConfig config, GuideModelRouter router, GuideSessionStore store, GuideAnswerRunner answers) {
        this.config = config; this.router = router; this.store = store;
        this.answers = answers;
        timer.scheduleWithFixedDelay(store::cleanup, 1, 1, TimeUnit.MINUTES);
    }
    public void requireReady() {
        if (!config.isEnabled()) throw new BusinessException(503, "艺绘向导已关闭");
        if (!router.supported() || config.getApiKey() == null || config.getApiKey().isBlank()) throw new BusinessException(503, "当前模型尚未接入或未配置密钥");
    }

    public SseEmitter stream(long owner, String sessionId, GuideMessageRequest request) {
        requireReady();
        // 在返回 SSE 前完成参数、归属、去重和并发检查，失败仍返回原 JSON 错误协议。
        var model = router.selected();
        var run = store.begin(owner, sessionId, request.clientMessageId(), request.text(), GuideAnswerRunner.SYSTEM);
        var emitter = new SseEmitter((long) Math.max(1, Math.min(300, config.getTimeoutSeconds())) * 1000 + 10000);
        var terminal = new AtomicBoolean();
        class Lifecycle {
            synchronized void send(String name, Object data) {
                if (terminal.get()) return;
                try { emitter.send(SseEmitter.event().name(name).data(data)); }
                catch (Exception e) { run.cancellation().cancel("disconnected"); }
            }
            synchronized void finish(String status, String error, GuideModelClient.Usage usage) {
                if (!terminal.compareAndSet(false, true)) return;
                store.finish(run, status, error, usage);
                try {
                    if (usage != null) emitter.send(SseEmitter.event().name("usage").data(usage));
                    if (error != null) emitter.send(SseEmitter.event().name("error").data(Map.of("message", error, "status", status)));
                    else emitter.send(SseEmitter.event().name("done").data(Map.of("status", status)));
                } catch (Exception ignored) { }
                emitter.complete();
            }
        }
        var lifecycle = new Lifecycle();
        run.cancellation().onCancel(() -> {
            boolean timeout = "timeout".equals(run.cancellation().reason());
            lifecycle.finish(timeout ? "error" : "cancelled", timeout ? "回答超时，已停止；不会自动重发" : null, null);
        });
        emitter.onCompletion(() -> { if (!terminal.get()) run.cancellation().cancel("disconnected"); });
        emitter.onTimeout(() -> run.cancellation().cancel("timeout"));
        emitter.onError(error -> run.cancellation().cancel("disconnected"));
        lifecycle.send("meta", Map.of("sessionId", sessionId, "clientMessageId", request.clientMessageId(),
                "messageId", run.assistantId(), "provider", config.getProvider(), "model", config.getTextModel()));
        var heartbeat = timer.scheduleAtFixedRate(() -> lifecycle.send("heartbeat", Map.of("time", System.currentTimeMillis())), 10, 10, TimeUnit.SECONDS);
        var deadline = timer.schedule(() -> run.cancellation().cancel("timeout"), Math.max(1, Math.min(300, config.getTimeoutSeconds())), TimeUnit.SECONDS);
        run.cancellation().onCancel(() -> { heartbeat.cancel(false); deadline.cancel(false); });
        try {
            var task = workers.submit(() -> {
                long started = System.nanoTime();
                String status = "error";
                GuideModelClient.Usage usage = null;
                try {
                    run.cancellation().check();
                    var reply = answers.answer(model, run.context(), delta -> {
                        run.cancellation().check();
                        if (store.append(run, delta)) lifecycle.send("delta", Map.of("text", delta));
                    }, lifecycle::send, refs -> store.references(run, refs), run.cancellation());
                    run.cancellation().check();
                    status = "length".equals(reply.finishReason()) ? "truncated" : "complete";
                    usage = reply.usage();
                    lifecycle.finish(status, null, usage);
                } catch (Exception e) {
                    status = run.cancellation().cancelled() ? "cancelled" : "error";
                    lifecycle.finish(status, e instanceof GuideModelException ? e.getMessage() : "回答中断，请手动重试；不会自动重发", null);
                } finally {
                    heartbeat.cancel(false); deadline.cancel(false);
                    log.info("guide_run owner={} session={} run={} provider={} model={} status={} durationMs={} inputTokens={} outputTokens={}",
                            owner, sessionId, request.clientMessageId(), config.getProvider(), config.getTextModel(), status,
                            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), usage == null ? null : usage.inputTokens(), usage == null ? null : usage.outputTokens());
                }
            });
            run.cancellation().onCancel(() -> task.cancel(true));
        } catch (RejectedExecutionException e) {
            lifecycle.finish("error", "向导当前繁忙，请稍后重新发送", null);
            heartbeat.cancel(false); deadline.cancel(false);
        }
        return emitter;
    }

    @PreDestroy public void close() { store.shutdown(); workers.shutdownNow(); timer.shutdownNow(); }
}

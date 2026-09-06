package com.artiv.guide.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

/** 每次运行独立取消句柄；显式跨线程传递，不依赖 ThreadLocal。 */
public final class GuideCancellation {
    private String reason;
    private final List<Runnable> hooks = new ArrayList<>();
    public synchronized boolean cancelled() { return reason != null; }
    public synchronized String reason() { return reason; }
    public void cancel(String reason) {
        List<Runnable> pending;
        synchronized (this) {
            if (this.reason != null) return;
            this.reason = reason;
            pending = List.copyOf(hooks);
            hooks.clear();
        }
        pending.forEach(hook -> { try { hook.run(); } catch (Exception ignored) { } });
    }
    public void onCancel(Runnable hook) {
        synchronized (this) { if (reason == null) { hooks.add(hook); return; } }
        hook.run();
    }
    public void check() { if (cancelled()) throw new CancellationException(reason()); }
}

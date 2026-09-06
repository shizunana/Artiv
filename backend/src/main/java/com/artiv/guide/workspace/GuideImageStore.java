package com.artiv.guide.workspace;

import com.artiv.common.BusinessException;
import com.artiv.config.GuideConfig;
import com.artiv.guide.GuidePrompts;
import com.artiv.guide.client.*;
import com.artiv.guide.session.GuideSessionStore;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.security.MessageDigest;
import java.util.*;

/** 聊天私有附件：不写公开文件目录；会话清空、过期或重启时一起释放。 */
@Component
public class GuideImageStore {
    public record Uploaded(String id, int width, int height) { }
    public record Observation(String text, GuideModelClient.Usage usage) { }
    private static class Stored {
        final String id = UUID.randomUUID().toString();
        final byte[] bytes;
        final String digest;
        final int width, height;
        String cacheKey, observation;
        Stored(GuideImageCodec.Image image) {
            bytes = image.bytes(); width = image.width(); height = image.height();
            try { digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
            catch (Exception e) { throw new IllegalStateException(e); }
        }
    }
    private final GuideSessionStore sessions;
    private final GuideConfig config;
    private final Map<String, List<Stored>> images = new HashMap<>();
    private final java.util.concurrent.Semaphore decoder = new java.util.concurrent.Semaphore(1);
    public GuideImageStore(GuideSessionStore sessions, GuideConfig config) {
        this.sessions = sessions; this.config = config;
        sessions.onRemoval(this::removeSession);
    }
    private synchronized void removeSession(String id) { images.remove(id); }
    public void requireVision() {
        if (!config.isEnabled() || !"deepseek".equals(config.getProvider())
                || !"deepseek-v4-flash-vision-exp".equals(config.getVisionModel()))
            throw new BusinessException(503, "当前模型尚未开启图片分析");
    }
    public Uploaded upload(long owner, String sessionId, MultipartFile file) {
        requireVision();
        sessions.get(owner, sessionId);
        if (!decoder.tryAcquire()) throw new BusinessException(429, "正在处理其他参考图，请稍后再试");
        GuideImageCodec.Image decoded;
        try { decoded = GuideImageCodec.decode(file); }
        finally { decoder.release(); }
        synchronized (this) {
            sessions.get(owner, sessionId);
            var list = images.computeIfAbsent(sessionId, key -> new ArrayList<>());
            var incoming = new Stored(decoded);
            var same = list.stream().filter(i -> i.digest.equals(incoming.digest)).findFirst();
            if (same.isPresent()) { var i = same.get(); return new Uploaded(i.id, i.width, i.height); }
            if (list.size() >= 3) throw new BusinessException(429, "每个会话最多 3 张参考图，请清空会话后再上传");
            long total = images.values().stream().flatMap(Collection::stream).mapToLong(i -> i.bytes.length).sum();
            if (total + incoming.bytes.length > 48L * 1024 * 1024) throw new BusinessException(429, "参考图容量已满，请稍后再试");
            list.add(incoming);
            return new Uploaded(incoming.id, incoming.width, incoming.height);
        }
    }
    private Stored owned(long owner, String sessionId, String imageId) {
        sessions.get(owner, sessionId);
        return images.getOrDefault(sessionId, List.of()).stream().filter(i -> i.id.equals(imageId)).findFirst()
                .orElseThrow(() -> new BusinessException(404, "参考图不存在或已过期"));
    }
    public synchronized void validate(long owner, String sessionId, String imageId) { requireVision(); owned(owner, sessionId, imageId); }
    public synchronized byte[] read(long owner, String sessionId, String imageId) { return owned(owner, sessionId, imageId).bytes.clone(); }
    /** 只移除尚未发出的附件；已发送附件随整个会话清空，避免聊天依据与记录不一致。 */
    public synchronized void delete(long owner, String sessionId, String imageId) {
        var image = owned(owner, sessionId, imageId);
        if (sessions.get(owner, sessionId).messages().stream().anyMatch(m -> imageId.equals(m.imageId())))
            throw new BusinessException(409, "参考图已用于聊天，请清空会话以删除图片及对应记录");
        images.get(sessionId).remove(image);
    }
    public Observation observe(long owner, String sessionId, String imageId, GuideModelClient model, GuideCancellation cancellation) {
        Stored image;
        String key;
        synchronized (this) {
            image = owned(owner, sessionId, imageId);
            key = image.digest + ':' + config.getProvider() + ':' + config.getVisionModel() + ':' + GuidePrompts.VERSION;
            if (key.equals(image.cacheKey)) return new Observation(image.observation, new GuideModelClient.Usage(0L, 0L));
        }
        cancellation.check();
        var reply = model.complete(new GuideModelClient.Request(List.of(
                new GuideModelClient.Message("system", GuidePrompts.IMAGE_OBSERVATION, null, null, List.of()),
                GuideModelClient.Message.image("请整理这张参考图中能看清的角色信息，含明确可辨的文字；不能确定的内容不要猜。",
                        "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(image.bytes))), true, List.of(), false));
        cancellation.check();
        if (!"stop".equals(reply.finishReason()) || !reply.toolCalls().isEmpty() || reply.text() == null
                || reply.text().isBlank() || reply.text().length() > 6000) throw new GuideModelException("图片识别未完整返回，请手动重试；不会自动重发");
        synchronized (this) {
            cancellation.check();
            if (owned(owner, sessionId, imageId) != image) throw new BusinessException(404, "参考图已失效");
            image.cacheKey = key; image.observation = reply.text();
        }
        return new Observation(reply.text(), reply.usage());
    }
}

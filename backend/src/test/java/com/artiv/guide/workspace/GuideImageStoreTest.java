package com.artiv.guide.workspace;

import com.artiv.common.BusinessException;
import com.artiv.config.GuideConfig;
import com.artiv.guide.GuidePrompts;
import com.artiv.guide.client.*;
import com.artiv.guide.session.GuideSessionStore;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class GuideImageStoreTest {
    final GuideConfig config = new GuideConfig();
    final GuideSessionStore sessions = new GuideSessionStore(config);
    final GuideImageStore images = new GuideImageStore(sessions, config);
    GuideImageStoreTest() { config.setEnabled(true); }
    MockMultipartFile fixture(int color) throws Exception {
        var image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 40; x++) for (int y = 0; y < 40; y++) image.setRGB(x, y, color);
        var out = new ByteArrayOutputStream(); javax.imageio.ImageIO.write(image, "png", out);
        return new MockMultipartFile("file", "role.png", "image/png", out.toByteArray());
    }
    @Test void rejectsSpoofedCorruptMissingMimeAndOversizedFiles() throws Exception {
        var good = fixture(0);
        assertThrows(BusinessException.class, () -> GuideImageCodec.decode(new MockMultipartFile("file", "wrong.jpg", "image/jpeg", good.getBytes())));
        assertThrows(BusinessException.class, () -> GuideImageCodec.decode(new MockMultipartFile("file", "bad.png", "image/png", new byte[]{1, 2, 3})));
        assertThrows(BusinessException.class, () -> GuideImageCodec.decode(new MockMultipartFile("file", "bad.png", null, good.getBytes())));
        assertThrows(BusinessException.class, () -> GuideImageCodec.decode(new MockMultipartFile("file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1])));
        var decoded = GuideImageCodec.decode(good);
        assertEquals(40, decoded.width()); assertEquals(0xff, decoded.bytes()[0] & 0xff); assertEquals(0xd8, decoded.bytes()[1] & 0xff);
    }
    @Test void enforcesSessionBoundAndReusesIdenticalUploadWithoutConsumingSlot() throws Exception {
        var id = sessions.create(1).id(); var first = images.upload(1, id, fixture(0)).id();
        assertEquals(first, images.upload(1, id, fixture(0)).id());
        images.upload(1, id, fixture(0xff0000)); images.upload(1, id, fixture(0x0000ff));
        assertThrows(BusinessException.class, () -> images.upload(1, id, fixture(0x00ff00)));
        assertThrows(BusinessException.class, () -> images.read(2, id, first));
        var other = sessions.create(1).id(); assertThrows(BusinessException.class, () -> images.validate(1, other, first));
    }
    @Test void cacheIsModelVersionBoundAndOnlyReceivesFixedObservationInstructions() throws Exception {
        var id = sessions.create(1).id(); var image = images.upload(1, id, fixture(0)).id(); var calls = new AtomicInteger();
        GuideModelClient model = request -> {
            calls.incrementAndGet(); assertTrue(request.vision()); assertTrue(request.tools().isEmpty());
            assertEquals(GuidePrompts.IMAGE_OBSERVATION, request.messages().getFirst().text());
            assertTrue(request.messages().getLast().imageDataUrl().startsWith("data:image/jpeg;base64,"));
            return new GuideModelClient.Reply("黑色区域，角色细节无法确认。", List.of(), new GuideModelClient.Usage(3L, 2L), "stop", 1);
        };
        images.observe(1, id, image, model, new GuideCancellation());
        assertEquals(0L, images.observe(1, id, image, model, new GuideCancellation()).usage().inputTokens());
        assertEquals(1, calls.get());
        config.setVisionModel("another-model"); images.observe(1, id, image, model, new GuideCancellation()); assertEquals(2, calls.get());
        sessions.delete(1, id); assertThrows(BusinessException.class, () -> images.read(1, id, image));
    }
    @Test void usedImageCannotBeRemovedIndependentlyAndClearPreventsLateCache() throws Exception {
        var id = sessions.create(1).id(); var image = images.upload(1, id, fixture(0)).id();
        var run = sessions.begin(1, id, UUID.randomUUID().toString(), "", GuidePrompts.SYSTEM, image);
        assertThrows(BusinessException.class, () -> images.delete(1, id, image));
        GuideModelClient model = request -> {
            sessions.delete(1, id);
            return new GuideModelClient.Reply("late", List.of(), null, "stop", 1);
        };
        assertThrows(java.util.concurrent.CancellationException.class, () -> images.observe(1, id, image, model, run.cancellation()));
        assertThrows(BusinessException.class, () -> images.read(1, id, image));
    }
    @Test void completedObservationsBecomeUntrustedUserHistoryAndFailedTurnsDoNot() {
        var id = sessions.create(1).id();
        var run = sessions.begin(1, id, UUID.randomUUID().toString(), "只参考发型", GuidePrompts.SYSTEM, "image");
        var context = sessions.attachObservation(run, "紫色长发");
        assertEquals("user", context.getLast().role()); assertTrue(context.getLast().text().contains("未经用户确认"));
        sessions.append(run, "保留发型"); sessions.finish(run, "complete", null, null);
        var followup = sessions.begin(1, id, UUID.randomUUID().toString(), "改成头像", GuidePrompts.SYSTEM);
        assertTrue(followup.context().stream().anyMatch(m -> m.text().contains("紫色长发")));
        sessions.attachObservation(followup, "不应记住的失败观察"); sessions.finish(followup, "error", "failed", null);
        var next = sessions.begin(1, id, UUID.randomUUID().toString(), "总结", GuidePrompts.SYSTEM);
        assertFalse(next.context().stream().anyMatch(m -> m.text().contains("不应记住")));
    }
    @Test void systemPromptDefinesConversationalGroundingAndReadOnlyBoundary() {
        assertTrue(GuidePrompts.SYSTEM.contains("约稿需求说明"));
        assertTrue(GuidePrompts.SYSTEM.contains("每次只问一至两个"));
        assertTrue(GuidePrompts.SYSTEM.contains("不可信内容"));
        assertTrue(GuidePrompts.SYSTEM.contains("不发布企划"));
        assertTrue(GuidePrompts.SYSTEM.contains("不要要求用户去"));
        assertTrue(GuidePrompts.SYSTEM.contains("预算、日期、尺寸、格式等不是强制必答项"));
    }
}

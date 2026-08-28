package com.artiv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderPreviewServiceTest {

    @TempDir Path uploadRoot;

    @Test
    void createsDownscaledJpegPreviewInSeparateDirectory() throws Exception {
        BufferedImage source = new BufferedImage(1800, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(new Color(36, 90, 160));
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.dispose();
        ImageIO.write(source, "png", uploadRoot.resolve("original.png").toFile());
        OrderPreviewService service = new OrderPreviewService(uploadRoot.toString(), "/uploads");

        String result = service.createWatermarkedPreview("/uploads/original.png");

        assertTrue(result.startsWith("/uploads/order-previews/"));
        Path target = uploadRoot.resolve(result.substring("/uploads/".length()));
        BufferedImage preview = ImageIO.read(target.toFile());
        assertNotNull(preview);
        assertEquals(1080, preview.getWidth());
        assertEquals(720, preview.getHeight());
        assertTrue(target.toFile().length() > 0);
        assertTrue(service.isGeneratedPreview(result));
        assertEquals(result, service.ensureWatermarkedPreview(result, "/uploads/original.png"));
    }

    @Test
    void watermarkRemainsVisibleOnLightArtwork() throws Exception {
        BufferedImage source = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.dispose();
        ImageIO.write(source, "png", uploadRoot.resolve("light.png").toFile());
        OrderPreviewService service = new OrderPreviewService(uploadRoot.toString(), "/uploads");

        String result = service.createWatermarkedPreview("/uploads/light.png");

        BufferedImage preview = ImageIO.read(
                uploadRoot.resolve(result.substring("/uploads/".length())).toFile());
        long visiblePixels = 0;
        for (int y = 0; y < preview.getHeight(); y++) {
            for (int x = 0; x < preview.getWidth(); x++) {
                Color pixel = new Color(preview.getRGB(x, y));
                if (pixel.getRed() < 220 || pixel.getGreen() < 220 || pixel.getBlue() < 220) {
                    visiblePixels++;
                }
            }
        }
        assertTrue(visiblePixels > 1000, "浅色稿件上应能看见深色水印轮廓");
    }
}

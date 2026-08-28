package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

/** 将订单原图转换为低分辨率、带重复水印的验收预览图。 */
@Service
public class OrderPreviewService {

    private static final int MAX_EDGE = 1080;
    private static final String PREVIEW_DIR = "order-previews";
    private static final String PREVIEW_FILENAME_PREFIX = "v4-";

    private final Path uploadRoot;
    private final String uploadUrlPrefix;

    public OrderPreviewService(@Value("${artiv.upload.dir}") String uploadDir,
                               @Value("${artiv.upload.url-prefix}") String uploadUrlPrefix) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.uploadUrlPrefix = uploadUrlPrefix;
    }

    public String createWatermarkedPreview(String originalUrl) {
        return createWatermarkedPreview(originalUrl, "画师");
    }

    public String createWatermarkedPreview(String originalUrl, String artistName) {
        Path source = resolveSource(originalUrl);
        try {
            BufferedImage original = ImageIO.read(source.toFile());
            if (original == null || original.getWidth() <= 0 || original.getHeight() <= 0) {
                throw new BusinessException(ResultCode.UPLOAD_INVALID);
            }
            double scale = Math.min(1D, (double) MAX_EDGE / Math.max(original.getWidth(), original.getHeight()));
            int width = Math.max(1, (int) Math.round(original.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(original.getHeight() * scale));
            BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = preview.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
                graphics.drawImage(original, 0, 0, width, height, null);
                paintWatermark(graphics, width, height, artistName);
            } finally {
                graphics.dispose();
            }
            Path targetDir = uploadRoot.resolve(PREVIEW_DIR);
            Files.createDirectories(targetDir);
            String filename = PREVIEW_FILENAME_PREFIX
                    + UUID.randomUUID().toString().replace("-", "") + ".jpg";
            writeJpeg(preview, targetDir.resolve(filename));
            return uploadUrlPrefix + "/" + PREVIEW_DIR + "/" + filename;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.UPLOAD_INVALID);
        }
    }

    /**
     * 兼容早期订单数据：旧记录可能把原图地址直接写进 attachments。
     * 已存在且可读取的标准预览图直接复用，否则从原图补生成一次。
     */
    public synchronized String ensureWatermarkedPreview(String currentPreviewUrl, String originalUrl) {
        return ensureWatermarkedPreview(currentPreviewUrl, originalUrl, "画师");
    }

    public synchronized String ensureWatermarkedPreview(String currentPreviewUrl, String originalUrl,
                                                         String artistName) {
        if (isGeneratedPreview(currentPreviewUrl)) {
            return currentPreviewUrl;
        }
        return createWatermarkedPreview(originalUrl, artistName);
    }

    public boolean isGeneratedPreview(String previewUrl) {
        String previewPrefix = uploadUrlPrefix + "/" + PREVIEW_DIR + "/";
        if (previewUrl == null || !previewUrl.startsWith(previewPrefix)) {
            return false;
        }
        String relative = previewUrl.substring(uploadUrlPrefix.length() + 1);
        Path preview = uploadRoot.resolve(relative).normalize();
        return preview.startsWith(uploadRoot.resolve(PREVIEW_DIR))
                && preview.getFileName().toString().startsWith(PREVIEW_FILENAME_PREFIX)
                && Files.isRegularFile(preview);
    }

    private Path resolveSource(String originalUrl) {
        if (originalUrl == null || !originalUrl.startsWith(uploadUrlPrefix + "/")) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        String relative = originalUrl.substring(uploadUrlPrefix.length() + 1);
        Path source = uploadRoot.resolve(relative).normalize();
        if (!source.startsWith(uploadRoot) || !Files.isRegularFile(source)) {
            throw new BusinessException(ResultCode.UPLOAD_INVALID);
        }
        return source;
    }

    private void paintWatermark(Graphics2D graphics, int width, int height, String artistName) {
        String displayName = artistName == null ? "画师" : artistName.trim().replaceAll("[\\r\\n]+", " ");
        if (displayName.isEmpty()) displayName = "画师";
        if (displayName.length() > 24) displayName = displayName.substring(0, 24) + "...";
        String authorLine = "@" + displayName + " 进行创作";
        String noticeLine = "仅供预览，请勿使用";
        int maxTextWidth = Math.max(1, (int) (width * 0.82));
        int fontSize = Math.max(18, Math.min(52, width / 16));
        Font authorFont = fitFont(graphics, authorLine, fontSize, maxTextWidth);
        Font noticeFont = fitFont(graphics, noticeLine, Math.max(16, fontSize - 4), maxTextWidth);
        FontMetrics authorMetrics = graphics.getFontMetrics(authorFont);
        FontMetrics noticeMetrics = graphics.getFontMetrics(noticeFont);
        int gap = Math.max(8, fontSize / 3);
        int contentHeight = authorMetrics.getHeight() + gap + noticeMetrics.getHeight();
        int authorBaseline = (height - contentHeight) / 2 + authorMetrics.getAscent();
        int noticeBaseline = authorBaseline + authorMetrics.getDescent() + gap + noticeMetrics.getAscent();
        int ruleMargin = Math.max(12, fontSize / 2);
        int ruleStart = Math.max(10, (int) (width * 0.10));
        int ruleEnd = Math.min(width - 10, (int) (width * 0.90));

        graphics.setColor(Color.WHITE);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42F));
        graphics.setStroke(new BasicStroke(Math.max(1.5F, width / 420F), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        graphics.drawLine(ruleStart, authorBaseline - authorMetrics.getAscent() - ruleMargin,
                ruleEnd, authorBaseline - authorMetrics.getAscent() - ruleMargin);
        graphics.drawLine(ruleStart, noticeBaseline + noticeMetrics.getDescent() + ruleMargin,
                ruleEnd, noticeBaseline + noticeMetrics.getDescent() + ruleMargin);
        drawCenteredText(graphics, authorLine, authorFont, authorBaseline, width);
        drawCenteredText(graphics, noticeLine, noticeFont, noticeBaseline, width);
        graphics.setComposite(AlphaComposite.SrcOver);
    }

    private Font fitFont(Graphics2D graphics, String text, int preferredSize, int maxWidth) {
        int size = preferredSize;
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, size);
        while (size > 14 && graphics.getFontMetrics(font).stringWidth(text) > maxWidth) {
            font = font.deriveFont((float) --size);
        }
        return font;
    }

    private void drawCenteredText(Graphics2D graphics, String text, Font font, int baseline, int width) {
        graphics.setFont(font);
        int x = (width - graphics.getFontMetrics(font).stringWidth(text)) / 2;
        graphics.setColor(new Color(22, 35, 55));
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28F));
        graphics.drawString(text, x + 2, baseline + 2);
        graphics.setColor(Color.WHITE);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.68F));
        graphics.drawString(text, x, baseline);
    }

    private void writeJpeg(BufferedImage image, Path target) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IOException("JPEG writer unavailable");
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(target.toFile())) {
            writer.setOutput(output);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.72F);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}

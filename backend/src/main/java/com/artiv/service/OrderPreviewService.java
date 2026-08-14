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
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
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
    private static final String WATERMARK = "ARTIV PREVIEW";

    private final Path uploadRoot;
    private final String uploadUrlPrefix;

    public OrderPreviewService(@Value("${artiv.upload.dir}") String uploadDir,
                               @Value("${artiv.upload.url-prefix}") String uploadUrlPrefix) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.uploadUrlPrefix = uploadUrlPrefix;
    }

    public String createWatermarkedPreview(String originalUrl) {
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
                paintWatermark(graphics, width, height);
            } finally {
                graphics.dispose();
            }
            Path targetDir = uploadRoot.resolve(PREVIEW_DIR);
            Files.createDirectories(targetDir);
            String filename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
            writeJpeg(preview, targetDir.resolve(filename));
            return uploadUrlPrefix + "/" + PREVIEW_DIR + "/" + filename;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.UPLOAD_INVALID);
        }
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

    private void paintWatermark(Graphics2D graphics, int width, int height) {
        int fontSize = Math.max(18, Math.min(42, width / 20));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
        graphics.setColor(Color.WHITE);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.34F));
        AffineTransform originalTransform = graphics.getTransform();
        graphics.rotate(Math.toRadians(-28), width / 2D, height / 2D);
        int xStep = Math.max(240, fontSize * 10);
        int yStep = Math.max(150, fontSize * 5);
        for (int y = -height; y < height * 2; y += yStep) {
            for (int x = -width; x < width * 2; x += xStep) {
                graphics.drawString(WATERMARK, x, y);
            }
        }
        graphics.setTransform(originalTransform);
        graphics.setComposite(AlphaComposite.SrcOver);
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

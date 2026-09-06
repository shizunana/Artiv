package com.artiv.guide.workspace;

import com.artiv.common.BusinessException;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;

/** 不信任名称/MIME：头部读取尺寸后再解码，单帧 PNG/JPEG，去元数据并缩小为私有 JPEG。 */
public final class GuideImageCodec {
    private GuideImageCodec() { }
    public record Image(byte[] bytes, int width, int height) { }
    public static Image decode(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 5L * 1024 * 1024) throw bad("参考图不能为空且不得超过 5 MB");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.matches("[^/\\\\]+\\.(png|jpg|jpeg)") || !("image/png".equals(file.getContentType()) || "image/jpeg".equals(file.getContentType()))) throw bad("仅接受 PNG 或 JPEG 参考图");
        try (var raw = file.getInputStream(); var input = new MemoryCacheImageInputStream(raw)) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw bad("文件不是可解码图片");
            var reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                boolean png = "png".equals(format), jpeg = "jpeg".equals(format) || "jpg".equals(format);
                if ((!png && !jpeg) || png != name.endsWith(".png") || png != "image/png".equals(file.getContentType())) throw bad("图片实际格式与文件名或类型不一致");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 8192 || height > 8192 || (long)width * height > 16_000_000) throw bad("图片尺寸过大：边长最多 8192，像素总量最多 1600 万");
                var original = reader.read(0); double ratio = Math.min(1d, 1024d / Math.max(width, height));
                int w = Math.max(1, (int)(width * ratio)), h = Math.max(1, (int)(height * ratio));
                var normalized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                var graphics = normalized.createGraphics();
                try { graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, w, h); graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC); graphics.drawImage(original, 0, 0, w, h, null); }
                finally { graphics.dispose(); original.flush(); }
                var output = new ByteArrayOutputStream();
                var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                try (var encoded = new MemoryCacheImageOutputStream(output)) {
                    writer.setOutput(encoded); writer.write(null, new javax.imageio.IIOImage(normalized, null, null), writer.getDefaultWriteParam());
                } finally { writer.dispose(); normalized.flush(); }
                if (output.size() > 1024 * 1024) throw bad("标准化图片超过 1 MB，请缩小图片后重试");
                return new Image(output.toByteArray(), w, h);
            } finally { reader.dispose(); }
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw bad("图片损坏或无法解码"); }
    }
    private static BusinessException bad(String message) { return new BusinessException(400, message); }
}

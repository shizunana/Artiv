package com.artiv.controller;

import com.artiv.common.BusinessException;
import com.artiv.common.Result;
import com.artiv.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/**
 * 文件上传：返回可访问的相对路径（/uploads/xxx）
 */
@RestController
@RequestMapping("/api/v1/upload")
@Slf4j
public class UploadController {

    private static final Set<String> ALLOWED = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    private final String uploadDir;
    private final String uploadUrlPrefix;

    public UploadController(@Value("${artiv.upload.dir}") String uploadDir,
                            @Value("${artiv.upload.url-prefix}") String uploadUrlPrefix) {
        this.uploadDir = uploadDir;
        this.uploadUrlPrefix = uploadUrlPrefix;
    }

    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.UPLOAD_INVALID);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.UPLOAD_INVALID);
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ALLOWED.contains(ext)) {
            throw new BusinessException(ResultCode.UPLOAD_INVALID);
        }
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            byte[] content = file.getBytes();
            String normalizedExt = "jpeg".equals(ext) ? "jpg" : ext;
            String filename = sha256(content) + "." + normalizedExt;
            Path target = dir.resolve(filename);
            if (Files.notExists(target)) {
                try {
                    Files.write(target, content, StandardOpenOption.CREATE_NEW);
                } catch (java.nio.file.FileAlreadyExistsException ignored) {
                    // 并发上传相同内容时复用另一请求已写入的文件。
                }
            }
            return Result.ok(uploadUrlPrefix + "/" + filename);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.ERROR);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}

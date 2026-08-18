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
import java.util.Set;
import java.util.UUID;

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
            // 每次上传保留独立文件，便于后续按业务记录单独替换、授权或清理。
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(dir.resolve(filename).toFile());
            return Result.ok(uploadUrlPrefix + "/" + filename);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.ERROR);
        }
    }

}

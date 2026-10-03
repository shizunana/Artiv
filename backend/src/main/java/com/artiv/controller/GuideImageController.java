package com.artiv.controller;

import com.artiv.common.Result;
import com.artiv.guide.workspace.GuideImageStore;
import com.artiv.security.UserContext;
import com.artiv.service.GuideChatService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/guide/sessions/{sessionId}/images", "/api/v1/admin/guide/sessions/{sessionId}/images"})
public class GuideImageController {
    private final GuideImageStore images;
    private final GuideChatService chat;
    public GuideImageController(GuideImageStore images, GuideChatService chat) { this.images = images; this.chat = chat; }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<GuideImageStore.Uploaded> upload(@PathVariable String sessionId, @RequestPart("file") MultipartFile file) {
        chat.requireReady();
        return Result.ok(images.upload(UserContext.getUserId(), sessionId, file));
    }
    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> image(@PathVariable String sessionId, @PathVariable String imageId) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").body(images.read(UserContext.getUserId(), sessionId, imageId));
    }
    @DeleteMapping("/{imageId}")
    public Result<Void> remove(@PathVariable String sessionId, @PathVariable String imageId) {
        images.delete(UserContext.getUserId(), sessionId, imageId);
        return Result.ok();
    }
}

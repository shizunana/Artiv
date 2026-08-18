package com.artiv.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UploadControllerTest {

    @TempDir Path uploadRoot;

    @Test
    void identicalUploadsKeepIndependentBusinessFiles() throws Exception {
        UploadController controller = new UploadController(uploadRoot.toString(), "/uploads");
        byte[] content = "same-image-content".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        String first = controller.upload(new MockMultipartFile(
                "file", "first.png", "image/png", content)).getData();
        String second = controller.upload(new MockMultipartFile(
                "file", "second.png", "image/png", content)).getData();

        org.junit.jupiter.api.Assertions.assertNotEquals(first, second);
        try (var files = Files.list(uploadRoot)) {
            assertEquals(2, files.count());
        }
    }
}

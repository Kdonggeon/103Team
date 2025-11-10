// src/main/java/com/team103/controller/FileUploadController.java
package com.team103.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    public FileUploadController() throws IOException {
        Files.createDirectories(uploadDir); // ./uploads 보장
    }

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("images") List<MultipartFile> images) throws IOException {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : images) {
            if (file.isEmpty()) continue;

            String original = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "image"));
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);

            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // ★ 정적 매핑(/files/**)과 맞춰 공개 URL 생성
            String publicUrl = ServletUriComponentsBuilder
                    .fromCurrentContextPath() // ex) http://localhost:9090
                    .path("/files/")
                    .path(filename)
                    .toUriString();

            urls.add(publicUrl);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("urls", urls);
        return ResponseEntity.ok(body);
    }
}

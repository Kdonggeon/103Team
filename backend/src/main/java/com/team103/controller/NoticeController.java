// backend/src/main/java/com/team103/controller/NoticeController.java
package com.team103.controller;

import com.team103.model.Notice;
import com.team103.repository.NoticeRepository;
import com.team103.repository.TeacherRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeRepository noticeRepo;

    @Autowired
    private TeacherRepository teacherRepo;

    /* ================= 파일 저장 유틸 ================= */

    /** 로컬 업로드 경로: ./uploads/notices */
    private static final Path UPLOAD_DIR = Paths.get("uploads", "notices");

    private static void ensureUploadDir() {
        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload dir: " + UPLOAD_DIR, e);
        }
    }

    /** 무작위 파일명 부여 (원본 확장자 유지) */
    private static String randomName(String original) {
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            ext = original.substring(dot);
        }
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    /** 저장 후 노출용 URL: /files/notices/<fname> */
    private static String publicUrl(String fileName) {
        return "/files/notices/" + fileName;
    }

    /** 이미지 저장 → 공개 URL 리스트 반환 */
    private static List<String> saveImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return Collections.emptyList();
        ensureUploadDir();
        List<String> urls = new ArrayList<>();
        for (MultipartFile mf : images) {
            if (mf == null || mf.isEmpty()) continue;
            String oname = Optional.ofNullable(mf.getOriginalFilename()).orElse("image");
            String fname = randomName(oname);
            Path dest = UPLOAD_DIR.resolve(fname).normalize().toAbsolutePath();
            try {
                Files.copy(mf.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                urls.add(publicUrl(fname));
            } catch (IOException e) {
                // 실패 파일은 스킵 (필요시 로깅)
            }
        }
        return urls;
    }

    /* ================= 내부 헬퍼 ================= */

    /** author(교사ID)로 teacherName 채워넣기 */
    private void fillTeacherName(Notice n) {
        if (n == null) return;
        String teacherId = n.getAuthor();
        if (teacherId != null) {
            teacherRepo.findById(teacherId).ifPresent(t -> n.setTeacherName(t.getTeacherName()));
        }
    }

    private List<Notice> fillTeacherNames(List<Notice> list) {
        if (list == null) return Collections.emptyList();
        for (Notice n : list) fillTeacherName(n);
        return list;
    }

    /* ================= 조회 ================= */

    // 전체 목록 (최신순)
    @GetMapping
    public List<Notice> listAll() {
        return fillTeacherNames(noticeRepo.findAllByOrderByCreatedAtDesc());
    }

    // 단건
    @GetMapping("/{id}")
    public ResponseEntity<Notice> getOne(@PathVariable String id) {
        return noticeRepo.findById(id)
                .map(n -> {
                    fillTeacherName(n);
                    return ResponseEntity.ok(n);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // 학원별 (최신순)
    @GetMapping("/academy/{academyNumber}")
    public List<Notice> getByAcademy(@PathVariable int academyNumber) {
        return fillTeacherNames(noticeRepo.findByAcademyNumberOrderByCreatedAtDesc(academyNumber));
    }

    // 반(과목)별 (최신순)
    @GetMapping("/class/{classId}")
    public List<Notice> getByClass(@PathVariable String classId) {
        return fillTeacherNames(noticeRepo.findByClassIdOrderByCreatedAtDesc(classId));
    }

    // 학원+반(과목)별 (최신순)
    @GetMapping("/academy/{academyNumber}/class/{classId}")
    public List<Notice> getByAcademyAndClass(@PathVariable int academyNumber, @PathVariable String classId) {
        return fillTeacherNames(
                noticeRepo.findByAcademyNumberAndClassIdOrderByCreatedAtDesc(academyNumber, classId)
        );
    }

    /* ================= 생성 ================= */

    // JSON 생성
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Notice> create(@RequestBody Notice notice) {
        // createdAt 기본값 보정
        if (notice.getCreatedAt() == null) notice.setCreatedAt(new Date());
        // teacherName은 저장하지 말고 응답에서만 채움
        notice.setTeacherName(null);

        Notice saved = noticeRepo.save(notice);
        fillTeacherName(saved);
        return ResponseEntity.ok(saved);
    }

    // Multipart 생성 (텍스트 + images[])
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Notice> createMultipart(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "author", required = false) String author,          // 교사ID
            // ✅ 프론트에서 FormData로 보내는 academyNumbers (복수형)
            @RequestParam(value = "academyNumbers", required = false) List<Integer> academyNumbers,
            // ✅ 혹시 단일 academyNumber로만 보내는 클라이언트가 있다면 fallback
            @RequestParam(value = "academyNumber", required = false) Integer legacyAcademyNumber,
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "className", required = false) String className,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        Notice n = new Notice();
        n.setTitle(title);
        n.setContent(content);
        n.setAuthor(author);

        // 🔹 다중 학원번호 우선
        if (academyNumbers != null && !academyNumbers.isEmpty()) {
            try {
                n.setAcademyNumbers(academyNumbers);
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
                // Notice에 academyNumbers 필드가 없다면 무시
            }
        } else if (legacyAcademyNumber != null) {
            // 단일 학원번호 fallback
            try {
                n.setAcademyNumbers(Collections.singletonList(legacyAcademyNumber));
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
            }
            try {
                n.setAcademyNumber(legacyAcademyNumber);
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
            }
        }

        if (classId != null && !classId.isBlank()) n.setClassId(classId.trim());
        if (className != null && !className.isBlank()) n.setClassName(className.trim());
        n.setCreatedAt(new Date());

        List<String> urls = saveImages(images);
        if (!urls.isEmpty()) n.setImageUrls(urls);

        Notice saved = noticeRepo.save(n);
        fillTeacherName(saved);
        return ResponseEntity.ok(saved);
    }

    /* ================= 수정 ================= */

    // JSON 수정 (이미지 리스트 통째로 교체하고 싶을 때 imageUrls 포함해서 보내기)
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Notice> updateNotice(
            @PathVariable String id,
            @RequestBody Notice notice
    ) {
        return noticeRepo.findById(id)
                .map(ex -> {
                    ex.setTitle(notice.getTitle());
                    ex.setContent(notice.getContent());
                    // 선택적 필드들 업데이트
                    if (notice.getAcademyNumber() != 0) ex.setAcademyNumber(notice.getAcademyNumber());
                    if (notice.getClassId() != null) ex.setClassId(notice.getClassId());
                    if (notice.getClassName() != null) ex.setClassName(notice.getClassName());
                    if (notice.getImageUrls() != null) ex.setImageUrls(notice.getImageUrls()); // 교체

                    // author/createdAt은 유지
                    Notice updated = noticeRepo.save(ex);
                    fillTeacherName(updated);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Multipart 수정 (텍스트 변경 + 새 이미지 추가(append))
    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Notice> updateNoticeMultipart(
            @PathVariable String id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            // ✅ 생성과 동일하게 academyNumbers/academyNumber 둘 다 지원
            @RequestParam(value = "academyNumbers", required = false) List<Integer> academyNumbers,
            @RequestParam(value = "academyNumber", required = false) Integer legacyAcademyNumber,
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "className", required = false) String className,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return noticeRepo.findById(id)
                .map(ex -> {
                    ex.setTitle(title);
                    ex.setContent(content);

                    if (academyNumbers != null && !academyNumbers.isEmpty()) {
                        try {
                            ex.setAcademyNumbers(academyNumbers);
                        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
                        }
                    } else if (legacyAcademyNumber != null) {
                        try {
                            ex.setAcademyNumbers(Collections.singletonList(legacyAcademyNumber));
                        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
                        }
                        try {
                            ex.setAcademyNumber(legacyAcademyNumber);
                        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
                        }
                    }

                    if (classId != null) ex.setClassId(classId);
                    if (className != null) ex.setClassName(className);

                    List<String> add = saveImages(images);
                    if (!add.isEmpty()) {
                        List<String> merged = new ArrayList<>();
                        if (ex.getImageUrls() != null) merged.addAll(ex.getImageUrls());
                        merged.addAll(add);
                        ex.setImageUrls(merged);
                    }

                    Notice updated = noticeRepo.save(ex);
                    fillTeacherName(updated);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /* ================= 삭제 ================= */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable String id) {
        if (!noticeRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        noticeRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

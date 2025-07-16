package com.team103.controller;

import com.team103.service.FcmTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/firebase")
public class FirebaseController {

    private final FcmTokenService fcmTokenService;

    // ✅ 명시적 생성자 작성 (롬복 없어도 100% 주입됨)
    public FirebaseController(FcmTokenService fcmTokenService) {
        this.fcmTokenService = fcmTokenService;
    }

    /**
     * 클라이언트(모바일)에서 FCM 토큰을 서버로 등록하는 엔드포인트
     * 예: POST /firebase/register-token?userId=stu123&role=student&fcmToken=abcdefg123
     */
    @PostMapping("/register-token")
    public ResponseEntity<String> registerFcmToken(
            @RequestParam String userId,
            @RequestParam String role,
            @RequestParam String fcmToken
    ) {
        fcmTokenService.saveFcmToken(userId, role, fcmToken);
        return ResponseEntity.ok("FCM 토큰이 성공적으로 등록되었습니다.");
    }
}

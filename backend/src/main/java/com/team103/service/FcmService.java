package com.team103.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.team103.model.Parent;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.ParentRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class); // ✅ log 해결

    // ✅ final 필드 생성자 주입
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository  parentRepository;

    // ✅ 생성자 추가(스프링이 자동 주입)
    public FcmService(StudentRepository studentRepository,
                      TeacherRepository teacherRepository,
                      ParentRepository parentRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.parentRepository  = parentRepository;
    }

    public void sendMessageTo(String userId, String fcmToken, String title, String body) {
        // 대상/토큰 프리픽스 확인 로그 (#2)
        log.info("[FCM] target userId={}, tokenPrefix={}",
                userId, (fcmToken == null ? "null" : fcmToken.substring(0, Math.min(12, fcmToken.length()))));

        // 기본 가드
        if (fcmToken == null || fcmToken.isBlank()) {
            log.info("[FCM] 토큰 없음 → 발송 스킵 (userId={})", userId);
            return;
        }
        if (fcmToken.startsWith("eyJ")) { // JWT 오발송 방지
            log.warn("[FCM] JWT 의심 토큰 → 발송 스킵 (userId={})", userId);
            return;
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();

        try {
            String resp = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] 전송 성공: {}", resp);

        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.warn("[FCM] UNREGISTERED → DB 토큰 제거 (userId={})", userId);

                // 학생
                Student s = studentRepository.findByStudentId(userId);
                if (s != null) { s.setFcmToken(null); studentRepository.save(s); return; }

                // 교사
                Teacher t = teacherRepository.findByTeacherId(userId);
                if (t != null) { t.setFcmToken(null); teacherRepository.save(t); return; }

                // 부모
                Parent p = parentRepository.findByParentsId(userId);
                if (p != null) { p.setFcmToken(null); parentRepository.save(p); return; }

                log.warn("[FCM] userId={} 에 해당하는 엔티티를 찾지 못해 토큰 정리 실패", userId);
                return;
            }
            throw new RuntimeException("[FCM] 전송 실패: " + e.getMessage(), e);
        }
    }
}

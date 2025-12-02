package com.team103.service;

import com.team103.model.AcademyRequest;
import com.team103.model.Parent;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.AcademyRequestRepository;
import com.team103.repository.ParentRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class AcademyRequestService {

    @Autowired
    private AcademyRequestRepository reqRepo;
    @Autowired
    private StudentRepository studentRepo;
    @Autowired
    private ParentRepository parentRepo;
    @Autowired
    private TeacherRepository teacherRepo;

    public AcademyRequest create(AcademyRequest req) {
        if (req.getAcademyNumber() == null || req.getAcademyNumber() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "academyNumber is required");
        }
        if (req.getRequesterId() == null || req.getRequesterRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requesterId / requesterRole required");
        }
        req.setStatus("PENDING");
        Date now = new Date();
        req.setCreatedAt(now);
        req.setUpdatedAt(now);
        return reqRepo.save(req);
    }

    public List<AcademyRequest> listMine(String requesterId, String role) {
        if (requesterId == null) return List.of();
        if (role != null) return reqRepo.findByRequesterIdAndRequesterRole(requesterId, role);
        return reqRepo.findByRequesterId(requesterId);
    }

    public List<AcademyRequest> listByAcademy(Integer academyNumber, String status) {
        if (academyNumber == null) return List.of();
        if (status != null && !status.isBlank()) {
            return reqRepo.findByAcademyNumberAndStatus(academyNumber, status);
        }
        return reqRepo.findByAcademyNumber(academyNumber);
    }

    public AcademyRequest approve(String id, String directorId, String memo) {
        AcademyRequest req = reqRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "request not found"));
        if (!Objects.equals(req.getStatus(), "PENDING")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "already processed");
        }
        Integer academyNumber = req.getAcademyNumber();
        if (academyNumber == null || academyNumber <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "academyNumber missing");
        }

        String role = req.getRequesterRole() == null ? "" : req.getRequesterRole().toLowerCase();
        String rid = req.getRequesterId();
        String targetStudentId = req.getTargetStudentId();

        if ("student".equals(role)) {
            Student s = studentRepo.findByStudentId(targetStudentId != null && !targetStudentId.isBlank() ? targetStudentId : rid);
            if (s == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "student not found");
            s.setAcademyNumbers(appendUnique(s.getAcademyNumbers(), academyNumber));
            studentRepo.save(s);
        } else if ("parent".equals(role)) {
            Parent p = parentRepo.findByParentsId(rid);
            if (p == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "parent not found");
            p.setAcademyNumbers(appendUnique(p.getAcademyNumbers(), academyNumber));
            parentRepo.save(p);
            // ▽ parent 요청: 대상 자녀 ID가 있으면 해당 학생, 없으면 memo나 자녀 목록에서 추론
            List<String> targetIds = new ArrayList<>();
            if (targetStudentId != null && !targetStudentId.isBlank()) {
                targetIds.add(targetStudentId);
            } else {
                // memo에서 학생 ID 추론 (공백 단위 단어 중 영숫자/밑줄/하이픈 포함)
                if (req.getMemo() != null) {
                    String[] parts = req.getMemo().split("\\s+");
                    for (String part : parts) {
                        if (part.matches("[A-Za-z0-9_-]+")) {
                            targetIds.add(part);
                            break;
                        }
                    }
                }
                // 그래도 없으면 부모의 자녀 리스트 전체를 대상으로 처리
                if (targetIds.isEmpty() && p.getStudentIds() != null) {
                    targetIds.addAll(p.getStudentIds());
                }
            }
            for (String sid : targetIds) {
                if (sid == null || sid.isBlank()) continue;
                Student child = studentRepo.findByStudentId(sid);
                if (child != null) {
                    child.setAcademyNumbers(appendUnique(child.getAcademyNumbers(), academyNumber));
                    studentRepo.save(child);
                }
            }
        } else if ("teacher".equals(role)) {
            Teacher t = teacherRepo.findByTeacherId(rid);
            if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "teacher not found");
            t.setAcademyNumbers(appendUnique(t.getAcademyNumbers(), academyNumber));
            teacherRepo.save(t);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported role: " + role);
        }

        req.setStatus("APPROVED");
        req.setProcessedBy(directorId);
        req.setProcessedMemo(memo);
        req.setUpdatedAt(new Date());
        return reqRepo.save(req);
    }

    public AcademyRequest reject(String id, String directorId, String memo) {
        AcademyRequest req = reqRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "request not found"));
        if (!Objects.equals(req.getStatus(), "PENDING")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "already processed");
        }
        req.setStatus("REJECTED");
        req.setProcessedBy(directorId);
        req.setProcessedMemo(memo);
        req.setUpdatedAt(new Date());
        return reqRepo.save(req);
    }

    private List<Integer> appendUnique(List<Integer> src, Integer n) {
        List<Integer> out = src == null ? new ArrayList<>() : new ArrayList<>(src);
        if (n != null && n > 0 && !out.contains(n)) out.add(n);
        return out;
    }
}

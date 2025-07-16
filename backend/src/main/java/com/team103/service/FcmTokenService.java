package com.team103.service;

import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.model.Parent;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import com.team103.repository.ParentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FcmTokenService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;

    @Autowired
    public FcmTokenService(StudentRepository studentRepository,
                           TeacherRepository teacherRepository,
                           ParentRepository parentRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.parentRepository = parentRepository;
    }

    @Transactional
    public void saveFcmToken(String userId, String role, String fcmToken) {
        switch (role.toLowerCase()) {
            case "student":
                studentRepository.findById(userId).ifPresent(student -> {
                    student.setFcmToken(fcmToken);
                    studentRepository.save(student);
                });
                break;

            case "teacher":
                teacherRepository.findById(userId).ifPresent(teacher -> {
                    teacher.setFcmToken(fcmToken);
                    teacherRepository.save(teacher);
                });
                break;

            case "parent":
                parentRepository.findById(userId).ifPresent(parent -> {
                    parent.setFcmToken(fcmToken);
                    parentRepository.save(parent);
                });
                break;

            default:
                throw new IllegalArgumentException("지원하지 않는 역할: " + role);
        }
    }
}

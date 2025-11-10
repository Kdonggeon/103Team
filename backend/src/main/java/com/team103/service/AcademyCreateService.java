package com.team103.service;

import com.team103.model.Academy;
import com.team103.model.Director;
import com.team103.repository.AcademyRepository;
import com.team103.repository.DirectorRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AcademyCreateService {

    private final AcademyRepository academyRepo;
    private final DirectorRepository directorRepo;

    public AcademyCreateService(AcademyRepository academyRepo, DirectorRepository directorRepo) {
        this.academyRepo = academyRepo;
        this.directorRepo = directorRepo;
    }

    /** 4자리 랜덤 번호 생성(1000~9999), 유니크 보장: 유니크 인덱스 + 중복 재시도 */
    private int generateUniqueAcademyNumber() {
        final int maxTries = 50;
        for (int i = 0; i < maxTries; i++) {
            int n = ThreadLocalRandom.current().nextInt(1000, 10000);
            if (academyRepo.findByAcademyNumber(n) == null) {
                return n;
            }
        }
        // 매우 드문 경우: 저장 시점 레이스를 대비해, 최종적으로 인덱스 예외 캐치로도 2차 방어
        return ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    public Academy createForDirectorUsername(String directorUsername, String name, String phone, String address) {
        if (directorUsername == null || directorUsername.isBlank()) {
            throw new IllegalArgumentException("directorUsername is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("academy name is required");
        }

        Director director = directorRepo.findByUsername(directorUsername);
        if (director == null) {
            throw new IllegalArgumentException("director not found: " + directorUsername);
        }

        // 1) 학원번호 생성
        Integer number = generateUniqueAcademyNumber();

        // 2) 저장 시도 (유니크 인덱스 레이스까지 커버)
        Academy saved;
        for (int tries = 0; ; tries++) {
            try {
                Academy a = new Academy();
                a.setAcademyNumber(number);        // @Field("academyNumber")
                a.setName(name);                    // @Field("Academy_Name")
                a.setPhone(phone);                  // @Field("Academy_Phone_Number")
                a.setAddress(address);              // @Field("Academy_Address")
                // 원장 번호가 모델에 있으면 세팅(있지 않으면 생략해도 무방)
                // a.setDirectorNumber( ... 필요 시 director의 번호 )

                saved = academyRepo.save(a);
                break;
            } catch (DuplicateKeyException dup) {
                // 중복 충돌 시 새 번호로 재시도
                if (tries > 20) throw dup;
                number = generateUniqueAcademyNumber();
            }
        }

        // 3) 원장 문서에 academyNumbers에 방금 번호 추가(중복 방지)
        List<Integer> nums = director.getAcademyNumbers();
        if (nums == null) nums = new ArrayList<>();
        if (!nums.contains(saved.getAcademyNumber())) {
            nums.add(saved.getAcademyNumber());
            director.setAcademyNumbers(nums);
            directorRepo.save(director);
        }

        return saved;
    }
}

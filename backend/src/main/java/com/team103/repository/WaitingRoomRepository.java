package com.team103.repository;

import com.team103.model.WaitingRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;


  //waiting_room 컬렉션 접근용

public interface WaitingRoomRepository extends MongoRepository<WaitingRoom, String> {

    /** 학원별 대기 중 학생 전체 조회 */
    List<WaitingRoom> findByAcademyNumber(int academyNumber);

    /** 학원+학생으로 단건 조회 (중복 등원 방지에 사용) */
    Optional<WaitingRoom> findByAcademyNumberAndStudentId(int academyNumber, String studentId);

    /** 학원+학생 존재 여부 */
    boolean existsByAcademyNumberAndStudentId(int academyNumber, String studentId);

    /** 좌석 배정 완료 시 대기실에서 제거 */
    void deleteByAcademyNumberAndStudentId(int academyNumber, String studentId);
}

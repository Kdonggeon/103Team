// src/main/java/com/team103/repository/RoomRepository.java
package com.team103.repository;

import com.team103.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends MongoRepository<Room, String> {

    // 학원 내 모든 강의실 (@Query 버전)
    @Query("{ 'Academy_Number': ?0 }")
    List<Room> findByAcademyNumber(int academyNumber);

    // 학원 + 방번호로 단건 조회 (@Query 버전)
    @Query("{ 'Room_Number': ?0, 'Academy_Number': ?1 }")
    Optional<Room> findByRoomNumberAndAcademyNumber(int roomNumber, int academyNumber);

    /* ── 편의 메서드 (선택) ───────────────────────── */

    // 스프링 데이터 네이밍 기반 – 동일 기능(취향대로 둘 중 하나 사용)
    List<Room> findAllByAcademyNumber(int academyNumber);

    Optional<Room> findOneByRoomNumberAndAcademyNumber(int roomNumber, int academyNumber);

    boolean existsByAcademyNumberAndRoomNumber(int academyNumber, int roomNumber);
    
    // ✅ 추가: 방번호로 전부 가져오기 (fallback용)
    @Query("{ 'Room_Number': ?0 }")
    List<Room> findByRoomNumber(int roomNumber);
    
}

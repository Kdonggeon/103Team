package com.team103.repository;

import com.team103.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;


public interface RoomRepository extends MongoRepository<Room, Integer> {

    /** 학원+강의실 번호로 단건 조회 (좌석 배정의 기본 키) */
    Optional<Room> findByAcademyNumberAndRoomNumber(int academyNumber, int roomNumber);

    /** 학원 내 모든 강의실 조회 */
    List<Room> findByAcademyNumber(int academyNumber);

    /** 현재 수업 ID로 진행 중인 강의실 찾기 (대시보드 등에서 사용) */
    @Query("{ 'Current_Class.Class_ID': ?0 }")
    List<Room> findByCurrentClassId(String classId);

    /** 특정 학생이 앉아있는 강의실(좌석 배열 포함) 조회 (필요시) */
    @Query("{ 'Academy_Number': ?0, 'Seats': { $elemMatch: { 'Student_ID': ?1 } } }")
    Optional<Room> findByAcademyNumberAndSeatStudentId(int academyNumber, String studentId);
}


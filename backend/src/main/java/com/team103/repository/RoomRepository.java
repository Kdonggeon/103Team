package com.team103.repository;

import com.team103.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.team103.model.Room;

public interface RoomRepository extends MongoRepository<Room, String> {

    // 학원 내 모든 강의실
    @Query("{ 'Academy_Number': ?0 }")
    List<Room> findByAcademyNumber(int academyNumber);

    // 학원 + 방번호로 단건 조회
    @Query("{ 'Room_Number': ?0, 'Academy_Number': ?1 }")
    Optional<Room> findByRoomNumberAndAcademyNumber(int roomNumber, int academyNumber);

}

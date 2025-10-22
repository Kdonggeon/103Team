// src/main/java/com/team103/repository/RoomRepository.java
package com.team103.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.team103.model.Room;

public interface RoomRepository extends MongoRepository<Room, String> {
    List<Room> findByAcademyNumber(int academyNumber);
    Optional<Room> findByRoomNumberAndAcademyNumber(int roomNumber, int academyNumber);
}

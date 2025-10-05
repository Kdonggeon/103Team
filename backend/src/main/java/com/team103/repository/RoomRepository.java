package com.team103.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.team103.model.Room;

public interface RoomRepository extends MongoRepository<Room, Integer> {

    Room findByRoomNumber(int roomNumber);

    List<Room> findByAcademyNumber(int academyNumber);

    Room findByRoomNumberAndAcademyNumber(int roomNumber, int academyNumber);
    
    Optional<Room> findByRoomNumberAndAcademyNumber(Integer roomNumber, Integer academyNumber);
}


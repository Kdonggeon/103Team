package com.team103.repository;

import com.team103.model.SeatMap;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SeatMapRepository extends MongoRepository<SeatMap, String> {
    Optional<SeatMap> findByClassIdAndRoomNumber(String classId, Integer roomNumber);
}

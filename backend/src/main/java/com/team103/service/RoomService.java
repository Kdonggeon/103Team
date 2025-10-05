package com.team103.service;

import com.team103.dto.RoomLayoutRequest;
import com.team103.model.Room;
import com.team103.repository.RoomRepository;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final RoomRepository roomRepo;

    // 명시적 생성자 주입
    public RoomService(RoomRepository roomRepo) {
        this.roomRepo = roomRepo;
    }

    public Room getOrCreate(Integer roomNumber, Integer academyNumber) {
        return roomRepo.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber)
                .orElseGet(() -> {
                    Room r = new Room();
                    r.setRoomNumber(roomNumber);
                    r.setAcademyNumber(academyNumber);
                    return roomRepo.save(r);
                });
    }

    public Room saveLayout(Integer roomNumber, RoomLayoutRequest req) {
        if (req.getAcademyNumber() == null) {
            throw new IllegalArgumentException("academyNumber is required");
        }
        Room room = getOrCreate(roomNumber, req.getAcademyNumber());
        room.setRows(req.getRows());
        room.setCols(req.getCols());
        room.setLayout(req.getLayout());
        return roomRepo.save(room);
    }
}

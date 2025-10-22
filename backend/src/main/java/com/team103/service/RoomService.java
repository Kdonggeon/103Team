package com.team103.service;

import com.team103.dto.RoomLayoutRequest;
import com.team103.dto.RoomUpdateRequest;
import com.team103.model.Room;
import com.team103.repository.RoomRepository;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final RoomRepository roomRepo;

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
        if (req.getAcademyNumber() == null)
            throw new IllegalArgumentException("academyNumber is required");

        Room room = roomRepo.findByRoomNumberAndAcademyNumber(roomNumber, req.getAcademyNumber())
            .orElseGet(() -> {
                Room r = new Room();
                r.setRoomNumber(roomNumber);
                r.setAcademyNumber(req.getAcademyNumber());
                return r;
            });

        room.setRows(req.getRows());
        room.setCols(req.getCols());
        room.setLayout(req.getLayout());

        return roomRepo.save(room);
    }

    /** ✅ 부분 수정 (PATCH) */
    public Room patchRoom(Integer roomNumber, RoomUpdateRequest req) {
        if (req.getAcademyNumber() == null)
            throw new IllegalArgumentException("academyNumber is required");

        Room room = roomRepo.findByRoomNumberAndAcademyNumber(roomNumber, req.getAcademyNumber())
            .orElseThrow(() -> new IllegalArgumentException("Room not found for academyNumber=" +
                    req.getAcademyNumber() + ", roomNumber=" + roomNumber));

        if (req.getRows() != null) room.setRows(req.getRows());
        if (req.getCols() != null) room.setCols(req.getCols());
        if (req.getLayout() != null) room.setLayout(req.getLayout());
        if (req.getCurrentClass() != null) room.setCurrentClass(req.getCurrentClass());
        if (req.getSeats() != null) room.setSeats(req.getSeats());

        return roomRepo.save(room);
    }
}

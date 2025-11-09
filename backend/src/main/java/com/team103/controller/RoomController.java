package com.team103.controller;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import com.team103.model.Room;
import com.team103.repository.RoomRepository;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    private final RoomRepository roomRepository;
    private final MongoTemplate mongoTemplate; // waiting_room 조회/삭제 및 vectorLayout 갱신용

    public RoomController(RoomRepository roomRepository, MongoTemplate mongoTemplate) {
        this.roomRepository = roomRepository;
        this.mongoTemplate  = mongoTemplate;
    }

    /** 수업 시작 시 현재 수업 정보 등록 */
    @PutMapping("/{roomNumber}/start-class")
    public ResponseEntity<?> startClass(
            @PathVariable int roomNumber,
            @RequestParam int academyNumber,
            @RequestBody Room.CurrentClass currentClass) {

        Optional<Room> opt = roomRepository.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 강의실을 찾을 수 없습니다");
        }

        Room room = opt.get();
        room.setCurrentClass(currentClass);
        roomRepository.save(room);
        return ResponseEntity.ok("수업이 등록되었습니다");
    }

    /** (선택) 입구 QR: 웨이팅룸 입장 or 갱신 */
    @PostMapping("/{roomNumber}/enter-lobby")
    public ResponseEntity<?> enterLobby(
            @PathVariable int roomNumber,
            @RequestParam int academyNumber,
            @RequestParam String studentId
    ) {
        String now = OffsetDateTime.now().toString();
        // upsert
        Update update = new Update()
                .set("Student_ID", studentId)
                .set("Academy_Number", academyNumber)
                .set("Checked_In_At", now)
                .set("Status", "LOBBY");
        mongoTemplate.upsert(
                new Query(new Criteria().andOperator(
                        anyStudentId(studentId),
                        anyAcademyNumber(academyNumber)
                )),
                update,
                "waiting_room"
        );
        return ResponseEntity.ok("로비 입장 기록됨");
    }

    /**
     * QR 스캔 시 학생 출석 및 좌석 배치(웨이팅룸 → 좌석)
     * - "waiting_room(academyNumber+studentId)"가 존재할 때만 배치 (사전 검증)
     * - 좌석 배치 성공 후, 방금 조회한 waiting_room 문서(동일 _id)만 삭제
     * - 이미 점유된 좌석이면 409(CONFLICT)
     * - waiting_room 미존재면 412(PRECONDITION_FAILED)
     */
    @PutMapping("/{roomNumber}/check-in")
    public ResponseEntity<?> checkIn(
            @PathVariable int roomNumber,
            @RequestParam int academyNumber,
            @RequestParam int seatNumber,      // 1..N 가정
            @RequestParam String studentId) {

        // 0) 강의실 존재 확인
        Optional<Room> opt = roomRepository.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("강의실 없음");
        }
        Room room = opt.get();

        // 좌석 번호 기본 검증 (vectorLayout 크기 기반)
        if (room.getVectorLayout() == null || seatNumber < 1 || seatNumber > room.getVectorLayout().size()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 좌석번호");
        }
        final int seatIndex = seatNumber - 1;

        // 1) waiting_room 사전 검증 (academyNumber + studentId 일치 문서가 있어야만 진행)
        Document wr = findWaitingRoomDoc(academyNumber, studentId);
        if (wr == null) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("대기실에 동일 정보가 없습니다.");
        }

        // 2) 좌석 배정 (원자적 업데이트: '비어있는 경우'에만 set → modifiedCount로 성공 판정)
        String seatStudentField    = "vectorLayout." + seatIndex + ".Student_ID";
        String seatOccupiedAtField = "vectorLayout." + seatIndex + ".occupiedAt";

        Query seatQuery = new Query(new Criteria().andOperator(
                Criteria.where("Academy_Number").is(academyNumber),
                Criteria.where("Room_Number").is(roomNumber),
                // 비어 있을 때만 업데이트(null/미존재/빈문자열 허용)
                new Criteria().orOperator(
                        Criteria.where(seatStudentField).exists(false),
                        Criteria.where(seatStudentField).is(null),
                        Criteria.where(seatStudentField).is("")
                )
        ));

        Update seatUpdate = new Update()
                .set(seatStudentField, studentId)
                .set(seatOccupiedAtField, new Date());

        UpdateResult ur = mongoTemplate.updateFirst(seatQuery, seatUpdate, "rooms");
        if (ur.getModifiedCount() == 0) {
            // 좌석 점유 중이면 대기실 삭제도 하지 않음
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 점유된 좌석입니다");
        }

        // 3) 방금 확인한 waiting_room 문서만 정확히 삭제(_id 기준)
        ObjectId wrId = wr.getObjectId("_id");
        mongoTemplate.remove(new Query(Criteria.where("_id").is(wrId)), "waiting_room");

        return ResponseEntity.ok("출석 체크 및 좌석 배치 완료");
    }

    /** waiting_room에서 academyNumber+studentId 일치 문서 1건 조회(필드/타입 혼재 대응) */
    private Document findWaitingRoomDoc(int academyNumber, String studentId) {
        Query q = new Query(new Criteria().andOperator(
                anyAcademyNumber(academyNumber),
                anyStudentId(studentId)
        )).limit(1);
        return mongoTemplate.findOne(q, Document.class, "waiting_room");
    }

    /* --------- criteria helpers --------- */

    private Criteria anyStudentId(String studentId) {
        List<Criteria> ors = new ArrayList<>();
        ors.add(Criteria.where("studentId").is(studentId));
        ors.add(Criteria.where("Student_ID").is(studentId));
        ors.add(Criteria.where("Student_Id").is(studentId));
        ors.add(Criteria.where("student_id").is(studentId));
        try {
            int sidNum = Integer.parseInt(studentId);
            ors.add(Criteria.where("studentId").is(sidNum));
            ors.add(Criteria.where("Student_ID").is(sidNum));
            ors.add(Criteria.where("Student_Id").is(sidNum));
            ors.add(Criteria.where("student_id").is(sidNum));
        } catch (NumberFormatException ignore) { }
        return new Criteria().orOperator(ors.toArray(new Criteria[0]));
    }

    private Criteria anyAcademyNumber(int academyNumber) {
        String anStr = String.valueOf(academyNumber);
        List<Criteria> ors = new ArrayList<>();
        ors.add(Criteria.where("academyNumber").is(academyNumber));
        ors.add(Criteria.where("academyNumber").is(anStr));
        ors.add(Criteria.where("Academy_Number").is(academyNumber));
        ors.add(Criteria.where("Academy_Number").is(anStr));
        ors.add(Criteria.where("academy_numbers").in(academyNumber, anStr));
        ors.add(Criteria.where("Academy_Numbers").in(academyNumber, anStr));
        return new Criteria().orOperator(ors.toArray(new Criteria[0]));
    }
}

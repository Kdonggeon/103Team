// src/main/java/com/team103/controller/RoomController.java
package com.team103.controller;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
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
import com.team103.service.SeatBoardService;

// 로깅
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    private final RoomRepository roomRepository;
    private final MongoTemplate mongoTemplate;
    private final SeatBoardService seatBoardService;

    public RoomController(RoomRepository roomRepository,
                          MongoTemplate mongoTemplate,
                          SeatBoardService seatBoardService) {
        this.roomRepository   = roomRepository;
        this.mongoTemplate    = mongoTemplate;
        this.seatBoardService = seatBoardService;
    }

    private String today() {
        return LocalDate.now(ZoneId.of("Asia/Seoul")).toString();
    }

    /* ============================================================
       1) 수업 등록
       ============================================================ */
    @PutMapping("/{roomNumber}/start-class")
    public ResponseEntity<?> startClass(
            @PathVariable int roomNumber,
            @RequestParam int academyNumber,
            @RequestBody Room.CurrentClass currentClass) {

        Optional<Room> opt =
                roomRepository.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber);

        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 강의실을 찾을 수 없습니다");
        }

        Room room = opt.get();
        room.setCurrentClass(currentClass);
        roomRepository.save(room);

        return ResponseEntity.ok("수업이 등록되었습니다");
    }

    /* ============================================================
       2) 입구 QR (waiting_room + entrance 출석 기록)
       ============================================================ */
    @PostMapping("/{roomNumber}/enter-lobby")
    public ResponseEntity<?> enterLobby(
            @PathVariable int roomNumber,
            @RequestParam int academyNumber,
            @RequestParam String studentId
    ) {
        log.info("[ENTER-LOBBY] room={}, academy={}, student={}",
                roomNumber, academyNumber, studentId);

        if (studentId == null || studentId.isBlank()) {
            return ResponseEntity.badRequest().body("studentId 필수");
        }

        String now = OffsetDateTime.now().toString();
        String ymd = today();

        /* -------------------------------
           waiting_room upsert
        --------------------------------*/
        Update wrUpdate = new Update()
                .set("Student_ID", studentId)
                .set("Academy_Number", academyNumber)
                .set("Checked_In_At", now)
                .set("Status", "LOBBY");

        mongoTemplate.upsert(
            new Query(new Criteria().andOperator(
                    anyStudentId(studentId),
                    anyAcademyNumber(academyNumber)
            )),
            wrUpdate,
            "waiting_room"
        );

        /* -------------------------------
           entrance 출석 문서 생성/갱신
           (🔥 핵심: Academy_Number 포함)
        --------------------------------*/
        Update entUpdate = new Update()
                .set("Type", "entrance")
                .set("Date", ymd)
                .set("Academy_Number", academyNumber)
                .set("updatedAt", now)
                .push("Attendance_List",
                        new Document()
                                .append("Student_ID", studentId)
                                .append("Status", "입구 출석")
                                .append("Source", "tablet")
                                .append("CheckIn_Time", now)
                );

        mongoTemplate.upsert(
                new Query(new Criteria().andOperator(
                        Criteria.where("Type").is("entrance"),
                        Criteria.where("Date").is(ymd),
                        Criteria.where("Academy_Number").is(academyNumber)   // 🔥 필터 추가
                )),
                entUpdate,
                "attendances"   // ← 이거
        );


        /* -------------------------------
           현재 반이 있으면 출석 상태 "이동"
        --------------------------------*/
        Optional<Room> opt =
                roomRepository.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber);

        if (opt.isPresent()) {
            Room room = opt.get();
            Room.CurrentClass cc = room.getCurrentClass();
            if (cc != null && cc.getClassId() != null) {
                try {
                    seatBoardService.moveOrBreak(cc.getClassId(), ymd, studentId, "이동");
                } catch (Exception e) {
                    log.error("moveOrBreak 실패", e);
                }
            }

            /* -------------------------------
               vectorLayout에서 기존 자리 비우기
            --------------------------------*/
            if (room.getVectorLayout() != null) {
                boolean changed = false;
                for (Room.VectorSeat s : room.getVectorLayout()) {
                    if (s != null && studentId.equals(s.getStudentId())) {
                        s.setStudentId(null);
                        changed = true;
                    }
                }
                if (changed) roomRepository.save(room);
            }
        }

        return ResponseEntity.ok("로비 입장 처리됨");
    }

    /* ============================================================
       3) 좌석 체크인
       ============================================================ */
    @PutMapping("/{roomNumber}/check-in")
    public ResponseEntity<?> checkIn(
            @PathVariable int roomNumber,
            @RequestParam int academyNumber,
            @RequestParam(name="seatNumber", required=false) Integer seatNumber,
            @RequestParam(name="seat", required=false) Integer seatParam,
            @RequestParam(name="studentId") String studentId
    ) {
        log.info("[CHECK-IN] room={}, academy={}, seatNum={}, seat={}, student={}",
                roomNumber, academyNumber, seatNumber, seatParam, studentId);

        try {
            if (studentId == null || studentId.isBlank()) {
                return ResponseEntity.badRequest().body("studentId 없음");
            }

            int resolvedSeat =
                    (seatNumber != null) ? seatNumber :
                    (seatParam   != null) ? seatParam   : -1;

            if (resolvedSeat <= 0)
                return ResponseEntity.badRequest().body("seatNumber 필요");

            Optional<Room> opt =
                    roomRepository.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber);

            if (opt.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("강의실 없음");

            Room room = opt.get();

            if (room.getVectorLayout() == null ||
                resolvedSeat < 1 ||
                resolvedSeat > room.getVectorLayout().size()) {
                return ResponseEntity.badRequest().body("잘못된 좌석번호");
            }

            Document wr = findWaitingRoomDoc(academyNumber, studentId);
            if (wr == null)
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                        .body("waiting_room 없음");

            /* -------------------------------
               좌석 점유 (rooms 업데이트)
            --------------------------------*/
            int idx = resolvedSeat - 1;

            String seatField = "vectorLayout." + idx + ".Student_ID";
            String occField  = "vectorLayout." + idx + ".occupiedAt";

            Query q = new Query(new Criteria().andOperator(
                    Criteria.where("Academy_Number").is(academyNumber),
                    Criteria.where("Room_Number").is(roomNumber),
                    new Criteria().orOperator(
                            Criteria.where(seatField).exists(false),
                            Criteria.where(seatField).is(null),
                            Criteria.where(seatField).is("")
                    )
            ));

            Update u = new Update()
                    .set(seatField, studentId)
                    .set(occField, new Date());

            UpdateResult ur = mongoTemplate.updateFirst(q, u, "rooms");
            if (ur.getModifiedCount() == 0)
                return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 점유된 좌석");

            /* -------------------------------
               현재 반이 있으면 좌석 + 출석 연동
            --------------------------------*/
            Room.CurrentClass cc = room.getCurrentClass();
            if (cc != null && cc.getClassId() != null) {
                try {
                    seatBoardService.assignSeat(cc.getClassId(), today(),
                            String.valueOf(resolvedSeat), studentId);

                    updateCourseSeatMap(cc.getClassId(),
                                        roomNumber,
                                        resolvedSeat,
                                        studentId);

                } catch (Exception e) {
                    log.error("assignSeat 실패", e);
                }
            }

            /* -------------------------------
               waiting_room 삭제
            --------------------------------*/
            Object wrId = wr.get("_id");
            if (wrId != null) {
                Query rq = new Query(Criteria.where("_id").is(wrId));
                mongoTemplate.remove(rq, "waiting_room");
            }

            return ResponseEntity.ok("출석 + 좌석 배치 완료");

        } catch (Exception e) {
            log.error("CHECK-IN 예외", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 오류");
        }
    }

    /* ============================================================
       공통 메서드
       ============================================================ */
    private Document findWaitingRoomDoc(int academyNumber, String studentId){
        Query q = new Query(new Criteria().andOperator(
                anyAcademyNumber(academyNumber),
                anyStudentId(studentId)
        )).limit(1);
        return mongoTemplate.findOne(q, Document.class, "waiting_room");
    }

    private void updateCourseSeatMap(String classId,
                                     int roomNumber,
                                     int seatNumber,
                                     String studentId)
    {
        String path = "Seat_Map." + roomNumber + "." + seatNumber;

        List<Criteria> ors = new ArrayList<>();
        ors.add(Criteria.where("Class_ID").is(classId));

        try {
            ObjectId oid = new ObjectId(classId);
            ors.add(Criteria.where("_id").is(oid));
        } catch (Exception ignore) {
            ors.add(Criteria.where("_id").is(classId));
        }

        Query q = new Query(new Criteria().orOperator(ors.toArray(new Criteria[0])));
        Update u = new Update().set(path, studentId);

        mongoTemplate.updateFirst(q, u, "classes");
    }

    private Criteria anyStudentId(String sid){
        List<Criteria> ors = new ArrayList<>();
        ors.add(Criteria.where("Student_ID").is(sid));
        ors.add(Criteria.where("studentId").is(sid));
        ors.add(Criteria.where("Student_Id").is(sid));
        ors.add(Criteria.where("student_id").is(sid));
        return new Criteria().orOperator(ors.toArray(new Criteria[0]));
    }

    private Criteria anyAcademyNumber(int an){
        String s = String.valueOf(an);
        List<Criteria> ors = new ArrayList<>();
        ors.add(Criteria.where("Academy_Number").is(an));
        ors.add(Criteria.where("Academy_Number").is(s));
        ors.add(Criteria.where("academyNumber").is(an));
        ors.add(Criteria.where("academyNumber").is(s));
        return new Criteria().orOperator(ors.toArray(new Criteria[0]));
    }
}

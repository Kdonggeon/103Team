// src/main/java/com/team103/controller/RoomController.java
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
import com.team103.service.SeatBoardService;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    private final RoomRepository roomRepository;
    private final MongoTemplate mongoTemplate; // waiting_room 조회/삭제 및 vectorLayout/Seat_Map 갱신용
    private final SeatBoardService seatBoardService; // 출석/좌석판 연동

    public RoomController(RoomRepository roomRepository,
                          MongoTemplate mongoTemplate,
                          SeatBoardService seatBoardService) {
        this.roomRepository   = roomRepository;
        this.mongoTemplate    = mongoTemplate;
        this.seatBoardService = seatBoardService;
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

    /**
     * 입구 QR: 웨이팅룸 입장 or 갱신 + (좌석에 앉아 있던 학생이면) 좌석 해제 + 상태 '이동' 처리
     *
     * - 언제 호출됨?
     *   → 입구/학원 QR 스캔 시 (학생이 다시 로비/복도 쪽으로 나갈 때)
     *
     * - 동작
     *   1) waiting_room 에 (academyNumber + studentId) 기준으로 upsert (Status="LOBBY")
     *   2) 해당 강의실에 currentClass 가 설정되어 있으면
     *      - SeatBoardService.moveOrBreak(...) 호출 → 해당 수업의 출석 상태를 "이동"으로 변경
     *      - rooms.vectorLayout 에서 이 학생이 앉아 있던 자리를 찾아 Student_ID 제거
     */
    @PostMapping("/{roomNumber}/enter-lobby")
    public ResponseEntity<?> enterLobby(
            @PathVariable int roomNumber,
            @RequestParam int academyNumber,
            @RequestParam String studentId
    ) {
        if (studentId == null || studentId.isBlank()) {
            return ResponseEntity.badRequest().body("studentId가 필요합니다.");
        }

        String now = OffsetDateTime.now().toString();

        // 1) waiting_room upsert (학원+학생 기준)
        Update update = new Update()
                .set("Student_ID", studentId)
                .set("Academy_Number", academyNumber)
                .set("Checked_In_At", now)
                .set("Status", "LOBBY"); // Director 화면에서는 "대기/이동/휴식"으로 묶어 보여줌
        mongoTemplate.upsert(
            new Query(new Criteria().andOperator(
                    anyStudentId(studentId),
                    anyAcademyNumber(academyNumber)
            )),
            update,
            "waiting_room"
        );

        // 2) 현재 강의실/수업 기준으로 좌석/출석 상태도 이동 처리
        Optional<Room> opt = roomRepository.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber);
        if (opt.isPresent()) {
            Room room = opt.get();

            // 2-1) 이 강의실에 현재 등록된 반(currentClass)이 있으면 출석 상태를 "이동"으로
            Room.CurrentClass cc = room.getCurrentClass();
            if (cc != null && cc.getClassId() != null && !cc.getClassId().isBlank()) {
                try {
                    // date=null → SeatBoardService 내부에서 todayYmd() 사용
                    seatBoardService.moveOrBreak(cc.getClassId(), null, studentId, "이동");
                } catch (Exception e) {
                    // 출석 쪽 오류가 나도 waiting_room 기록은 살아 있어야 하므로 silent 처리
                    e.printStackTrace();
                }
            }

            // 2-2) 이 강의실 vectorLayout 에서 이 학생이 앉아 있던 좌석을 찾아서 비워준다
            if (room.getVectorLayout() != null && !room.getVectorLayout().isEmpty()) {
                boolean changed = false;
                for (Room.VectorSeat seat : room.getVectorLayout()) {
                    if (seat == null) continue;
                    String sid = seat.getStudentId();
                    if (sid != null && sid.equals(studentId)) {
                        seat.setStudentId(null);  // 자리 비우기
                        changed = true;
                    }
                }
                if (changed) {
                    roomRepository.save(room);
                }
            }
        }

        return ResponseEntity.ok("로비 입장 및 이동 처리됨");
    }

    /**
     * QR 스캔 시 학생 출석 및 좌석 배치(웨이팅룸 → 좌석)
     * - "waiting_room(academyNumber+studentId)"가 존재할 때만 배치 (사전 검증)
     * - 좌석 배치 성공 후, 방금 조회한 waiting_room 문서(동일 _id)만 삭제
     * - 현재 강의실에 currentClass가 있으면 SeatBoardService.assignSeat() 호출 → attendances에도 출석 반영
     * - 이미 점유된 좌석이면 409(CONFLICT)
     * - waiting_room 미존재면 412(PRECONDITION_FAILED)
     *
     * ✅ 의도된 최종 구조:
     *   Seat_Map.<roomNumber>.<seatNumber> = studentId
     *   + rooms.vectorLayout[seatIndex].Student_ID = studentId
     */
    @PutMapping("/{roomNumber}/check-in")
    public ResponseEntity<?> checkIn(
            @PathVariable int roomNumber,
            @RequestParam int academyNumber,
            // QR에서는 seat=2 로 들어오므로 seatNumber/seat 둘 다 받도록
            @RequestParam(name = "seatNumber", required = false) Integer seatNumber,
            @RequestParam(name = "seat",       required = false) Integer seatParam,
            // QR 자체에는 없고, 앱에서 붙여서 호출해야 함
            @RequestParam(name = "studentId",  required = false) String studentId) {

        // 1) studentId 필수 체크
        if (studentId == null || studentId.isBlank()) {
            return ResponseEntity.badRequest().body("studentId가 없습니다. (로그인된 앱에서 호출해야 합니다)");
        }

        // 2) seatNumber / seat 둘 중 하나 선택
        int resolvedSeatNumber =
                (seatNumber != null) ? seatNumber :
                (seatParam   != null) ? seatParam   : -1;
        if (resolvedSeatNumber <= 0) {
            return ResponseEntity.badRequest().body("seatNumber 또는 seat 파라미터가 필요합니다.");
        }

        // 3) 강의실 조회, 좌석 번호 유효성 체크
        Optional<Room> opt = roomRepository.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("강의실 없음");
        }
        Room room = opt.get();

        if (room.getVectorLayout() == null
                || resolvedSeatNumber < 1
                || resolvedSeatNumber > room.getVectorLayout().size()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 좌석번호");
        }
        final int seatIndex = resolvedSeatNumber - 1;

        // 4) waiting_room 사전 검증
        Document wr = findWaitingRoomDoc(academyNumber, studentId);
        if (wr == null) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("대기실에 동일 정보가 없습니다.");
        }

        // 5) rooms.vectorLayout[seatIndex].Student_ID = studentId (비어있을 때만)
        String seatStudentField    = "vectorLayout." + seatIndex + ".Student_ID";
        String seatOccupiedAtField = "vectorLayout." + seatIndex + ".occupiedAt";

        Query seatQuery = new Query(new Criteria().andOperator(
            Criteria.where("Academy_Number").is(academyNumber),
            Criteria.where("Room_Number").is(roomNumber),
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
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 점유된 좌석입니다");
        }

        // 6) 현재 반이 있으면 출석/Seat_Map 연동
        Room.CurrentClass cc = room.getCurrentClass();
        if (cc != null && cc.getClassId() != null && !cc.getClassId().isBlank()) {
            try {
                String classId   = cc.getClassId();
                String seatLabel = String.valueOf(resolvedSeatNumber);
                seatBoardService.assignSeat(classId, null, seatLabel, studentId);
                updateCourseSeatMap(classId, roomNumber, resolvedSeatNumber, studentId);
            } catch (Exception e) {
                e.printStackTrace(); // 연동 실패해도 좌석/waiting_room은 유지
            }
        }

        // 7) 방금 사용한 waiting_room 문서만 삭제
        ObjectId wrId = wr.getObjectId("_id");
        mongoTemplate.remove(new Query(Criteria.where("_id").is(wrId)), "waiting_room");

        return ResponseEntity.ok("출석 체크 및 좌석 배치 완료");
    }

    /** waiting_room에서 academyNumber+studentId 일치 문서 1건 조회(필드/타입 혼재 대응) */
    private Document findWaitingRoomDoc(int academyNumber, String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return null;
        }
        Query q = new Query(new Criteria().andOperator(
                anyAcademyNumber(academyNumber),
                anyStudentId(studentId)
        )).limit(1);
        return mongoTemplate.findOne(q, Document.class, "waiting_room");
    }

    /**
     * ✅ Seat_Map.<roomNumber>.<seatNumber> = studentId 구조로 classes 컬렉션 업데이트
     */
    private void updateCourseSeatMap(String classId, int roomNumber, int seatNumber, String studentId) {
        if (classId == null || classId.isBlank()) return;

        String roomKey = String.valueOf(roomNumber);   // "2063"
        String seatKey = String.valueOf(seatNumber);   // "2"
        String path    = "Seat_Map." + roomKey + "." + seatKey;

        List<Criteria> ors = new ArrayList<>();
        // 1) Class_ID == classId
        ors.add(Criteria.where("Class_ID").is(classId));

        // 2) _id == classId (ObjectId 가능성 + String 가능성 둘 다)
        try {
            ObjectId oid = new ObjectId(classId);
            ors.add(Criteria.where("_id").is(oid));
        } catch (Exception ignore) {
            // classId가 ObjectId 형식이 아니면 String 비교도 한 번 시도
            ors.add(Criteria.where("_id").is(classId));
        }

        Query q = new Query(new Criteria().orOperator(ors.toArray(new Criteria[0])));
        Update u = new Update().set(path, studentId);

        // 컬렉션 이름: "classes"
        mongoTemplate.updateFirst(q, u, "classes");
    }

    /* --------- criteria helpers --------- */

    private Criteria anyStudentId(String studentId) {
        if (studentId == null) {
            // null이면 절대 매칭되지 않게 만들어서 NPE 방지
            return Criteria.where("Student_ID").is("__never_match__");
        }

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
        } catch (Exception ignore) { }
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

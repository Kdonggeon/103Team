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
        // upsert: academyNumber + studentId 기준으로 LOBBY 상태 기록/갱신
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
     * 좌석 QR 스캔 시:
     *  - 1순위: waiting_room(academyNumber+studentId)이 있으면 → 대기실에서 꺼내 좌석 배정 후 삭제
     *  - 2순위: 없으면 → 이미 다른 강의실/좌석에 앉아 있다고 보고, 해당 학원 내 모든 강의실에서 기존 좌석 비우고 새 좌석으로 이동
     *
     * 규칙:
     *  - 좌석 번호는 1..N (vectorLayout 길이 기준)
     *  - 이미 점유된 좌석이면 409(CONFLICT)
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

        // 1) waiting_room 사전 조회 (academyNumber + studentId)
        Document wr = findWaitingRoomDoc(academyNumber, studentId);
        boolean fromLobby = (wr != null);

        // 2) 대기실에서 온 게 아니더라도,
        //    같은 학원(academyNumber) 내 다른 강의실/좌석에 앉아있을 수 있으므로
        //    모든 강의실 vectorLayout에서 이 학생을 먼저 제거(좌석 이동 지원)
        clearStudentSeatsInAcademy(academyNumber, studentId);

        // 3) 새 좌석 배정 (원자적 업데이트: '비어있는 경우'에만 set → modifiedCount로 성공 판정)
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
            // 좌석 점유 중이면 대기실 삭제도 하지 않음 (다른 사람이 앉아 있는 자리)
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 점유된 좌석입니다");
        }

        // 4) 대기실에서 온 경우에만, 방금 확인한 waiting_room 문서 정확히 삭제(_id 기준)
        if (fromLobby) {
            ObjectId wrId = wr.getObjectId("_id");
            mongoTemplate.remove(new Query(Criteria.where("_id").is(wrId)), "waiting_room");
        }

        return ResponseEntity.ok("출석 체크 및 좌석 배치 완료");
    }

    /**
     * 같은 학원(academyNumber) 내 모든 강의실의 vectorLayout에서
     * 해당 학생(studentId)이 앉아 있던 좌석을 찾아 비운다.
     * - Student_ID == studentId 인 모든 좌석의 Student_ID를 "" 로, occupiedAt 제거
     * - 좌석 '이동' 시 기존 자리 비우기용
     */
    private void clearStudentSeatsInAcademy(int academyNumber, String studentId) {
        if (studentId == null || studentId.isBlank()) return;

        // Student_ID 가 studentId 인 vectorLayout 원소가 있는 모든 Room 문서 대상
        Query q = new Query(new Criteria().andOperator(
                Criteria.where("Academy_Number").is(academyNumber),
                Criteria.where("vectorLayout.Student_ID").is(studentId)
        ));

        Update u = new Update()
                .set("vectorLayout.$[s].Student_ID", "")
                .unset("vectorLayout.$[s].occupiedAt")
                .filterArray(Criteria.where("s.Student_ID").is(studentId));

        mongoTemplate.updateMulti(q, u, "rooms");
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

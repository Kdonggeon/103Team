package com.team103.repository;

import com.team103.model.WaitingRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * waiting_room 컬렉션 접근용 Repository
 * - Academy_Number / Student_ID 필드 혼재 대응
 * - SeatBoardService / RoomController 에서 모두 사용됨
 */
public interface WaitingRoomRepository extends MongoRepository<WaitingRoom, String> {

    /** 학원별 대기 중 학생 전체 조회 (Academy_Number, academyNumber 둘 다 지원) */
    @Query("""
           {
             "$or": [
               { "Academy_Number": ?0 },
               { "academyNumber": ?0 },
               { "academy_numbers": ?0 }
             ]
           }
           """)
    List<WaitingRoom> findByAcademyNumber(int academyNumber);

    /** 학원 + 학생 단건 조회 */
    @Query("""
           {
             "$and": [
               {
                 "$or": [
                   { "Academy_Number": ?0 },
                   { "academyNumber": ?0 },
                   { "academy_numbers": ?0 }
                 ]
               },
               {
                 "$or": [
                   { "Student_ID": ?1 },
                   { "studentId": ?1 },
                   { "student_id": ?1 }
                 ]
               }
             ]
           }
           """)
    Optional<WaitingRoom> findByAcademyNumberAndStudentId(int academyNumber, String studentId);

    /** 존재 여부 체크 */
    @Query("""
           {
             "$and": [
               {
                 "$or": [
                   { "Academy_Number": ?0 },
                   { "academyNumber": ?0 },
                   { "academy_numbers": ?0 }
                 ]
               },
               {
                 "$or": [
                   { "Student_ID": ?1 },
                   { "studentId": ?1 },
                   { "student_id": ?1 }
                 ]
               }
             ]
           }
           """)
    boolean existsByAcademyNumberAndStudentId(int academyNumber, String studentId);

    /** 좌석 배정 완료 → 대기실 제거 */
    @Query("""
           {
             "$and": [
               {
                 "$or": [
                   { "Academy_Number": ?0 },
                   { "academyNumber": ?0 },
                   { "academy_numbers": ?0 }
                 ]
               },
               {
                 "$or": [
                   { "Student_ID": ?1 },
                   { "studentId": ?1 },
                   { "student_id": ?1 }
                 ]
               }
             ]
           }
           """)
    void deleteByAcademyNumberAndStudentId(int academyNumber, String studentId);

    /** 해당 학원의 roster(학생 목록) 중 대기 중인 학생들 조회 */
    @Query("""
           {
             "$and": [
               {
                 "$or": [
                   { "Academy_Number": ?0 },
                   { "academyNumber": ?0 },
                   { "academy_numbers": ?0 }
                 ]
               },
               { "Student_ID": { "$in": ?1 } }
             ]
           }
           """)
    List<WaitingRoom> findByAcademyNumberAndStudentIdIn(int academyNumber, List<String> studentIds);
}

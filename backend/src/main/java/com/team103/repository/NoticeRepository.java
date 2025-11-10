package com.team103.repository;

import com.team103.model.Notice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends MongoRepository<Notice, String> {

    /* 기존: 학원별 */
    List<Notice> findByAcademyNumber(int academyNumber);

    /* ✅ 과목(반) 연결용 */
    List<Notice> findByClassId(String classId);
    List<Notice> findByAcademyNumberAndClassId(int academyNumber, String classId);

    /* ✅ 작성자(교사 ID)별 */
    List<Notice> findByAuthor(String author);

    /* ✅ 여러 학원 한 번에 */
    List<Notice> findByAcademyNumberIn(List<Integer> academyNumbers);

    /* ✅ 최신순(createdAt desc) 정렬 버전들 */
    List<Notice> findAllByOrderByCreatedAtDesc();
    List<Notice> findByAcademyNumberOrderByCreatedAtDesc(int academyNumber);
    List<Notice> findByClassIdOrderByCreatedAtDesc(String classId);
    List<Notice> findByAcademyNumberAndClassIdOrderByCreatedAtDesc(int academyNumber, String classId);
}

package com.team103.repository;

import com.team103.model.Notice;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends MongoRepository<Notice, String> {
    // 필요시 추가 쿼리메서드 정의 가능
	List<Notice> findByAcademyNumber(int academyNumber);
}

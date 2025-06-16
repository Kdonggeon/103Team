package com.team103.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.team103.model.Qa;

public interface QaRepository extends MongoRepository<Qa, String> {
	// 추가적인 쿼리 메서드가 필요하면 여기 선언
}
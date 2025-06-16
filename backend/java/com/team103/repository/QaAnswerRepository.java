package com.team103.repository;

import com.team103.model.QaAnswer;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface QaAnswerRepository extends MongoRepository<QaAnswer, String> {
	QaAnswer findByQaId(String qaId);  // 단일
	List<QaAnswer> findAllByQaId(String qaId); // 다수
}
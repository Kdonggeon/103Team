package com.team103.repository;

import com.team103.model.AcademyRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademyRequestRepository extends MongoRepository<AcademyRequest, String> {
    List<AcademyRequest> findByRequesterId(String requesterId);
    List<AcademyRequest> findByRequesterIdAndRequesterRole(String requesterId, String requesterRole);
    List<AcademyRequest> findByAcademyNumber(Integer academyNumber);
    List<AcademyRequest> findByAcademyNumberAndStatus(Integer academyNumber, String status);
}

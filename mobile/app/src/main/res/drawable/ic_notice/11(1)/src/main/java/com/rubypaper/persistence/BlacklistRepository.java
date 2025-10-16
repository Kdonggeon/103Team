package com.rubypaper.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.rubypaper.domain.Blacklist;

public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {
    List<Blacklist> findByBlockerId(String blockerId);  //사용자가 차단한 사용자 목록 조회
}
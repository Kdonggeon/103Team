package com.rubypaper.persistence;

import com.rubypaper.domain.Hyperlink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HyperlinkRepository extends JpaRepository<Hyperlink, Long> {
    // 그룹별 링크 조회
    List<Hyperlink> findByGroupId(Long groupId);
}
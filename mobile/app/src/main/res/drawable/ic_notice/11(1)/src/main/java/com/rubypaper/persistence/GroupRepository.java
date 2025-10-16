package com.rubypaper.persistence;

import com.rubypaper.domain.ProjectGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<ProjectGroup, Long> {
    // 그룹 이름으로 조회 (중복 체크나 로그인 시 활용 가능)
    ProjectGroup findByName(String name);
    
}

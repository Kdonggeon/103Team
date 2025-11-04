package com.rubypaper.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.rubypaper.domain.Member;

public interface MemberRepository extends JpaRepository<Member, String> {
	// :Member의 ID 타입은  string
}
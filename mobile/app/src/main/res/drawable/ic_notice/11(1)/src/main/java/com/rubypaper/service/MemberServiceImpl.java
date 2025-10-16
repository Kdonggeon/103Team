package com.rubypaper.service;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.rubypaper.domain.Member;
import com.rubypaper.persistence.MemberRepository;

@Service
public class MemberServiceImpl implements MemberService {
	@Autowired
	private MemberRepository memberRepo;

	@Override
	public Member getMember(Member member) {
		Optional<Member> findMember=memberRepo.findById(member.getId());
		
		if(findMember.isPresent())   //검색결과가 있다면
			return findMember.get();
		else
			return null;
	}
}

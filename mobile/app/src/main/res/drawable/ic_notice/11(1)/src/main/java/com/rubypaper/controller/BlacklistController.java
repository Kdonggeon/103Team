package com.rubypaper.controller;

import java.time.LocalDateTime;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.rubypaper.domain.Blacklist;
import com.rubypaper.domain.Member;
import com.rubypaper.persistence.BlacklistRepository;
import com.rubypaper.persistence.MemberRepository;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/blacklist")
public class BlacklistController {

    @Autowired
    private BlacklistRepository blacklistRepository; // 블랙리스트 관련 DB 접근용 레포지토리

    @Autowired
    private MemberRepository memberRepository; // 회원 정보 관련 DB 접근용 레포지토리
    
    
     //현재 로그인한 사용자의 블랙리스트 목록을 조회하여 뷰에 전달하는 메서드
     
    @GetMapping
    public String viewBlacklist(Model model, HttpSession session) {
        Member loginUser = (Member) session.getAttribute("user"); // 로그인 사용자 정보 얻기
        String currentUserId = loginUser.getId(); // 로그인한 사용자의 ID
        
        List<Blacklist> list = blacklistRepository.findByBlockerId(currentUserId); // 차단한 사용자 목록 조회
        model.addAttribute("blacklist", list); // 뷰에 블랙리스트 데이터 추가
        return "blacklist"; // blacklist.html 뷰 반환
    }

    
     //블랙리스트에 사용자를 추가하는 메서드

    @PostMapping("/add")
    public String addBlacklist(@RequestParam String blockedId, HttpSession session) {
        Member loginUser = (Member) session.getAttribute("user"); // 로그인 사용자 정보 획득

        Blacklist bl = new Blacklist(); // 새로운 블랙리스트 엔티티 객체 생성
        bl.setBlockerId(loginUser.getId()); // 차단하는 사용자 ID 설정
        bl.setBlockedId(blockedId); // 차단당할 사용자 ID 설정
        bl.setCreatedAt(LocalDateTime.now()); // 차단 시각 기록

        blacklistRepository.save(bl); // 블랙리스트 저장
        return "redirect:/blacklist"; // 블랙리스트 목록으로 리다이렉트
    }

    
    //블랙리스트에서 사용자를 제거하는 메서드

    @PostMapping("/remove")
    public String removeBlacklist(@RequestParam Long id) {
        blacklistRepository.deleteById(id); // 해당 ID 블랙리스트 삭제
        return "redirect:/blacklist"; // 블랙리스트 목록으로 리다이렉트
    }

    
     //중복된 매핑(필요시 제거 권장)

    @GetMapping("/blacklist")
    public String showBlacklist(HttpSession session, Model model) {
        // 차단 목록 조회 및 모델 추가 (미구현)
        return "blacklist"; // blacklist.html 뷰 반환
    }
}

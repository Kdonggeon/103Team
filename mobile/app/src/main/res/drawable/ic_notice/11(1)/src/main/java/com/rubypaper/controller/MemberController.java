package com.rubypaper.controller;

import com.rubypaper.domain.Member;
import com.rubypaper.persistence.MemberRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class MemberController {

    @Autowired
    private MemberRepository memberRepository;

    // 로그인 폼
    @GetMapping("/login")
    public String loginForm() {
        return "login"; // login.html
    }

    // 로그인 처리
    @PostMapping("/login")
    public String processLogin(@RequestParam String id,
                                @RequestParam String password,
                                HttpSession session,
                                Model model) {
        Member member = memberRepository.findById(id).orElse(null);
        if (member != null && member.getPassword().equals(password)) {
            session.setAttribute("user", member); // 로그인 성공 → 세션에 저장
            return "redirect:/"; // index.html로 이동
        } else {
            model.addAttribute("error", "아이디 또는 비밀번호가 틀렸습니다.");
            return "login"; // 다시 로그인 페이지
        }
    }

    // 로그아웃 처리
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // 세션 삭제
        return "redirect:/login"; // 로그인 페이지로 이동
    }

    // 회원가입 폼
    @GetMapping("/signup")
    public String signupForm() {
        return "signup"; // signup.html
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String processSignup(@RequestParam String id,
                                 @RequestParam String password,
                                 @RequestParam String name,
                                 Model model) {
        if (memberRepository.existsById(id)) {
            model.addAttribute("error", "이미 존재하는 아이디입니다.");
            return "signup";
        }

        Member member = new Member();
        member.setId(id);
        member.setPassword(password);
        member.setName(name);
        member.setRole("USER");

        memberRepository.save(member);

        return "redirect:/login";
    }
    

}

package com.rubypaper.controller;

import com.rubypaper.domain.Member;
import com.rubypaper.persistence.GroupRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @Autowired
    private GroupRepository groupRepository;
    @GetMapping("/")
    public String showIndex(Model model, HttpSession session) {
        // 로그인 세션 확인
        Member loginUser = (Member) session.getAttribute("user");
        if (loginUser == null) {
            return "redirect:/login"; // 로그인 페이지로 리디렉션
        }

        model.addAttribute("groups", groupRepository.findAll());
        return "index";
    }
}

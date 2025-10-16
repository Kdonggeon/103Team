package com.rubypaper.controller;

import com.rubypaper.domain.Hyperlink;
import com.rubypaper.domain.ProjectGroup;
import com.rubypaper.persistence.GroupRepository;
import com.rubypaper.persistence.HyperlinkRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class GroupController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private HyperlinkRepository hyperlinkRepository; // 하이퍼링크 저장소

    /* 그룹 생성 화면 (폼만) */
    @GetMapping("/groups")
    public String showGroupCreatePage() {
        return "groupCreate"; // 템플릿: groupCreate.html
    }

    /** 그룹 생성 처리 후 게시판 입장 */
    @PostMapping("/groups")
    public String createGroup(@RequestParam String name,
                              @RequestParam String password) {
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setName(name);
        projectGroup.setPassword(password);
        projectGroup.setCreatedAt(LocalDateTime.now());
        groupRepository.save(projectGroup);
        return "redirect:/groupBoard/" + projectGroup.getId();
    }

    /** 그룹 게시판 진입 */
    @GetMapping("/groupBoard/{id}")
    public String viewGroupBoard(@PathVariable Long id, Model model) {
        Optional<ProjectGroup> groupOpt = groupRepository.findById(id);
        if (groupOpt.isEmpty()) {
            return "redirect:/";
        }
        ProjectGroup group = groupOpt.get();
        model.addAttribute("group", group);

        // 해당 그룹의 하이퍼링크 목록 조회
        List<Hyperlink> links = hyperlinkRepository.findByGroupId(id);
        model.addAttribute("links", links);

        return "groupBoard"; // 템플릿: groupBoard.html
    }

    /* 하이퍼링크 생성 */
    @PostMapping("/groupBoard/{id}/links")
    public String addLink(@PathVariable Long id,
                          @RequestParam String url,
                          @RequestParam String title) {
        Hyperlink link = new Hyperlink();
        link.setGroupId(id);
        link.setUrl(url);
        link.setTitle(title);
        hyperlinkRepository.save(link);
        return "redirect:/groupBoard/" + id;
    }

    /* 하이퍼링크 삭제 */
    @PostMapping("/groupBoard/{groupId}/links/{linkId}/delete")
    public String deleteLink(@PathVariable Long groupId,
                             @PathVariable Long linkId) {
        hyperlinkRepository.deleteById(linkId);
        return "redirect:/groupBoard/" + groupId;
    }

    /* 그룹 비밀번호 입력 화면 */
    @GetMapping("/groupLogin/{id}")
    public String showGroupLoginPage(@PathVariable Long id, Model model) {
        Optional<ProjectGroup> groupOpt = groupRepository.findById(id);
        if (groupOpt.isPresent()) {
            model.addAttribute("group", groupOpt.get());
            return "groupLogin"; // 템플릿: groupLogin.html
        } else {
            return "redirect:/";
        }
    }

    /* 비밀번호 확인 후 게시판 진입 */
    @PostMapping("/groupLogin/{id}")
    public String checkGroupPassword(@PathVariable Long id,
                                     @RequestParam String password,
                                     Model model,
                                     HttpSession session) {
        Optional<ProjectGroup> groupOpt = groupRepository.findById(id);
        if (groupOpt.isPresent()) {
            ProjectGroup projectGroup = groupOpt.get();
            if (projectGroup.getPassword().equals(password)) {
                session.setAttribute("accessedGroup_" + id, true);
                return "redirect:/groupBoard/" + id;
            } else {
                model.addAttribute("group", projectGroup);
                model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
                return "groupLogin";
            }
        } else {
            return "redirect:/";
        }
    }

    /* 그룹 삭제 */
    @PostMapping("/group/delete/{id}")
    public String deleteGroup(@PathVariable Long id) {
        groupRepository.deleteById(id);
        return "redirect:/";
    }
}

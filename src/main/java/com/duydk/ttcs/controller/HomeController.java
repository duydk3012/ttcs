package com.duydk.ttcs.controller;

import com.duydk.ttcs.dto.UserResponseDTO;
import com.duydk.ttcs.entity.Chapter;
import com.duydk.ttcs.entity.Comment;
import com.duydk.ttcs.entity.Story;
import com.duydk.ttcs.entity.User;
import com.duydk.ttcs.service.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
public class HomeController {


    private final UserService userService;
    private final StoryService storyService;
    private final ChapterService chapterService;
    private final GenreService genreService;
    private final CommentService commentService;

    public HomeController(UserService userService, StoryService storyService, ChapterService chapterService, GenreService genreService, CommentService commentService) {
        this.userService = userService;
        this.storyService = storyService;
        this.chapterService = chapterService;
        this.genreService = genreService;
        this.commentService = commentService;
    }

    @GetMapping("/home")
    public String showHome(Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAuthenticated", currentUser != null);
        // Thêm danh sách truyện hot
        model.addAttribute("hotStories", storyService.getHotStories());
        // Thêm danh sách chapter mới cập nhật
        model.addAttribute("recentUpdatedStories", storyService.getRecentUpdatedStories());
        // Thêm danh sách truyện mới
        model.addAttribute("newStories", storyService.getNewStories());
        // Thêm danh sách truyện đã hoàn thành
        model.addAttribute("completedStories", storyService.getCompletedStories());
        return "home";
    }

    @GetMapping("/")
    public String redirectToHome(Model model) {
        return "redirect:/home";
    }



}

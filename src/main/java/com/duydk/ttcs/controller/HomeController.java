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
        model.addAttribute("recentChapters", chapterService.getRecentChapters());
        // Thêm danh sách truyện mới
        model.addAttribute("newStories", storyService.getNewStories());
        // Thêm danh sách truyện đã hoàn thành
        model.addAttribute("completedStories", storyService.getCompletedStories());
        return "home";
    }

    // Trang chi tiết truyện
    @GetMapping("/story/{storyId}")
    public String getStory(@PathVariable Long storyId,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           Model model) {
        // Lấy thông tin truyện
        Story story = storyService.findStoryById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));
        model.addAttribute("story", story);
        model.addAttribute("firstChapterId", chapterService.getChapterIdWithMinChapterNumber(storyId));
        model.addAttribute("lastChapterId", chapterService.getChapterIdWithMaxChapterNumber(storyId));

        // Lấy danh sách chương với phân trang
        PageRequest pageable = PageRequest.of(page, size, Sort.by("chapterNumber").ascending());
        model.addAttribute("chapters", chapterService.findAllChaptersByStoryId(storyId, pageable));

        // Lấy danh sách bình luận
        model.addAttribute("comments", commentService.getCommentsByStoryId(storyId));

        // Lấy thông tin người dùng hiện tại
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAuthenticated", currentUser != null);

        return "story";
    }

    // Trang chi tiết chương
    @GetMapping("/story/{storyId}/chapter/{chapterId}")
    public String getChapter(@PathVariable Long storyId,
                             @PathVariable Long chapterId,
                             Model model) {
        // Lấy thông tin truyện
        Story story = storyService.findStoryById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));
        model.addAttribute("story", story);

        // Lấy thông tin chương
        Chapter chapter = chapterService.findChapterById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        model.addAttribute("chapter", chapter);
        model.addAttribute("chapterList", chapterService.getChapterRepository().findAllByStoryIdOrderByChapterNumberAsc(storyId));

        // Lấy chương trước và sau
        model.addAttribute("previousChapter", chapterService.getPreviousChapter(storyId, chapter.getChapterNumber()));
        model.addAttribute("nextChapter", chapterService.getNextChapter(storyId, chapter.getChapterNumber()));

        // Lấy danh sách bình luận
        model.addAttribute("comments", commentService.getCommentsByChapterId(chapterId));

        // Lấy thông tin người dùng hiện tại
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAuthenticated", currentUser != null);

        return "chapter";
    }

    // Gửi bình luận cho truyện
    @PostMapping("/story/{storyId}/comment")
    public String postStoryComment(@PathVariable Long storyId,
                                   @RequestParam String content,
                                   Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        Comment comment = new Comment();
        comment.setStory(storyService.findStoryById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found")));
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        if (currentUser == null) {
            return "redirect:/login";
        }
        comment.setUser(userService.getUserRepository().findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")));
        comment.setContent(content);
        commentService.saveComment(comment);
        return "redirect:/story/" + storyId;
    }

    // Gửi bình luận cho chương
    @PostMapping("/story/{storyId}/chapter/{chapterId}/comment")
    public String postChapterComment(@PathVariable Long storyId,
                                     @PathVariable Long chapterId,
                                     @RequestParam String content,
                                     Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        Comment comment = new Comment();
        comment.setStory(storyService.findStoryById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found")));
        comment.setChapter(chapterService.findChapterById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found")));
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        if (currentUser == null) {
            return "redirect:/login";
        }
        comment.setUser(userService.getUserRepository().findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")));
        comment.setContent(content);
        commentService.saveComment(comment);
        return "redirect:/story/" + storyId + "/chapter/" + chapterId;
    }

    @GetMapping("/")
    public String redirectToHome(Model model) {
        return "redirect:/home";
    }



}

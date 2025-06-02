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
    private final BookmarkService bookmarkService;
    private final ReadingHistoryService readingHistoryService;

    public HomeController(UserService userService, StoryService storyService, ChapterService chapterService,
                          GenreService genreService, CommentService commentService,
                          BookmarkService bookmarkService, ReadingHistoryService readingHistoryService) {
        this.userService = userService;
        this.storyService = storyService;
        this.chapterService = chapterService;
        this.genreService = genreService;
        this.commentService = commentService;
        this.bookmarkService = bookmarkService;
        this.readingHistoryService = readingHistoryService;
    }

    @GetMapping("/home")
    public String showHome(Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAuthenticated", currentUser != null);
        model.addAttribute("genreList", genreService.findAllGenres(Pageable.unpaged()).getContent());
        model.addAttribute("hotStories", storyService.getHotStories());
        model.addAttribute("recentUpdatedStories", storyService.getRecentUpdatedStories());
        model.addAttribute("newStories", storyService.getNewStories());
        model.addAttribute("completedStories", storyService.getCompletedStories());

        if (currentUser != null) {
            Pageable pageableBookmark = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Pageable pageableHistory = PageRequest.of(0, 10, Sort.by("lastReadAt").descending()); // Sửa createdAt thành lastReadAt
            model.addAttribute("bookmarkedStories", bookmarkService.getBookmarkRepository()
                    .findByUserId(userService.getUserRepository()
                            .findByUsername(currentUser.getUsername()).get().getId(), pageableBookmark)
                    .getContent());
            model.addAttribute("readingHistories", readingHistoryService.getReadingHistoryRepository()
                    .findByUserId(userService.getUserRepository()
                            .findByUsername(currentUser.getUsername()).get().getId(), pageableHistory)
                    .getContent());
        }

        return "home";
    }

    @GetMapping("/")
    public String redirectToHome(Model model) {
        return "redirect:/home";
    }



}

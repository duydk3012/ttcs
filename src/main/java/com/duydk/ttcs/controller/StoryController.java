package com.duydk.ttcs.controller;


import com.duydk.ttcs.dto.UserResponseDTO;
import com.duydk.ttcs.entity.Chapter;
import com.duydk.ttcs.entity.Comment;
import com.duydk.ttcs.entity.Story;
import com.duydk.ttcs.entity.StoryStatus;
import com.duydk.ttcs.service.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/story")
public class StoryController {

    private final UserService userService;
    private final StoryService storyService;
    private final ChapterService chapterService;
    private final GenreService genreService;
    private final CommentService commentService;

    public StoryController(UserService userService, StoryService storyService, ChapterService chapterService, GenreService genreService, CommentService commentService) {
        this.userService = userService;
        this.storyService = storyService;
        this.chapterService = chapterService;
        this.genreService = genreService;
        this.commentService = commentService;
    }


    // Trang chi tiết truyện
    @GetMapping("/{storyId}")
    public String getStory(@PathVariable Long storyId,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           Model model) {
        // Lấy thông tin truyện
        Story story = storyService.findStoryById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));
        story.setViews(story.getViews() + 1);
        model.addAttribute("story", story);
        model.addAttribute("firstChapterId", chapterService.getChapterIdWithMinChapterNumber(storyId));
        model.addAttribute("lastChapterId", chapterService.getChapterIdWithMaxChapterNumber(storyId));

        // Lấy danh sách chương với phân trang
        PageRequest pageable = PageRequest.of(page, size, Sort.by("chapterNumber").ascending());
        model.addAttribute("chapters", chapterService.findAllChaptersByStoryId(storyId, pageable));
        model.addAttribute("chaptersCount", chapterService.findAllChaptersByStoryId(storyId, Pageable.unpaged()).getContent().size());


        // Lấy danh sách bình luận
        model.addAttribute("comments", commentService.getCommentsByStoryId(storyId));

        // Lấy thông tin người dùng hiện tại
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAuthenticated", currentUser != null);

        model.addAttribute("pageSize", size);

        return "story";
    }

    // Trang chi tiết chương
    @GetMapping("/{storyId}/chapter/{chapterId}")
    public String getChapter(@PathVariable Long storyId,
                             @PathVariable Long chapterId,
                             Model model) {
        // Lấy thông tin truyện
        Story story = storyService.findStoryById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));
        model.addAttribute("story", story);
        story.setViews(story.getViews() + 1);


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
    @PostMapping("/{storyId}/comment")
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
    @PostMapping("/{storyId}/chapter/{chapterId}/comment")
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

    // Trang search
    @GetMapping("/search")
    public String search(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String chapters,
            @RequestParam(required = false, defaultValue = "title") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder,
            Model model) {

        Pageable pageable = createPageable(page, size, sortBy, sortOrder);
        Page<Story> storyPage = applyFilters(search, status, chapters, pageable);

        model.addAttribute("stories", storyPage);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", storyPage.getTotalPages());
        model.addAttribute("pageSize", size);

        model.addAttribute("status", status);
        model.addAttribute("chapters", chapters);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortOrder", sortOrder);

        return "search";
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortOrder) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

        switch (sortBy) {
            case "createdAt":
                return PageRequest.of(page, size, Sort.by(direction, "createdAt"));
            case "updatedAt":
                return PageRequest.of(page, size, Sort.by(direction, "updatedAt"));
            case "views":
                return PageRequest.of(page, size, Sort.by(direction, "views"));
            case "chapterCount":
                return PageRequest.of(page, size, Sort.by(direction, "chapters.size"));
            default:
                return PageRequest.of(page, size, Sort.by(direction, "title"));
        }
    }

    private Page<Story> applyFilters(String search, String status, String chapters, Pageable pageable) {
        Page<Story> stories = storyService.getStoryRepository().findByTitleContaining(search, pageable);

        if (status != null && !status.isEmpty()) {
            try {
                StoryStatus storyStatus = StoryStatus.valueOf(status);
                stories = storyService.getStoryRepository().findByTitleContainingAndStatus(search, storyStatus, pageable);
            } catch (IllegalArgumentException e) {
            }
        }

        if (chapters != null && !chapters.isEmpty()) {
            stories = filterByChapterCount(stories, chapters);
        }

        return stories;
    }

    private Page<Story> filterByChapterCount(Page<Story> stories, String chapters) {
        List<Story> filtered = stories.getContent().stream()
                .filter(story -> {
                    int count = story.getChapters().size();
                    switch (chapters) {
                        case "0-100": return count < 100;
                        case "100-500": return count >= 100 && count < 500;
                        case "500-1000": return count >= 500 && count < 1000;
                        case "1000+": return count >= 1000;
                        default: return true;
                    }
                })
                .collect(Collectors.toList());

        return new PageImpl<>(filtered, stories.getPageable(), filtered.size());
    }

}

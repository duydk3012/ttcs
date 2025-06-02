package com.duydk.ttcs.controller;


import com.duydk.ttcs.dto.UserResponseDTO;
import com.duydk.ttcs.entity.*;
import com.duydk.ttcs.service.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    private final BookmarkService bookmarkService;
    private final ReadingHistoryService readingHistoryService;


    public StoryController(UserService userService, StoryService storyService, ChapterService chapterService,
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


    // Trang chi tiết truyện
    @GetMapping("/{storyId}")
    public String getStory(@PathVariable Long storyId,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           Model model) {
        Story story = storyService.findStoryById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));
        story.setViews(story.getViews() + 1);
        model.addAttribute("story", story);
        model.addAttribute("firstChapterId", chapterService.getChapterIdWithMinChapterNumber(storyId));
        model.addAttribute("lastChapterId", chapterService.getChapterIdWithMaxChapterNumber(storyId));

        PageRequest pageable = PageRequest.of(page, size, Sort.by("chapterNumber").ascending());
        model.addAttribute("chapters", chapterService.findAllChaptersByStoryId(storyId, pageable));
        model.addAttribute("chaptersCount", chapterService.findAllChaptersByStoryId(storyId, Pageable.unpaged()).getContent().size());

        model.addAttribute("comments", commentService.getCommentsByStoryId(storyId));

        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAuthenticated", currentUser != null);
        // Kiểm tra trạng thái bookmark
        boolean isBookmarked = currentUser != null && bookmarkService.isBookmarked(
                userService.getUserRepository().findByUsername(currentUser.getUsername()).get().getId(), storyId);
        model.addAttribute("isBookmarked", isBookmarked);
        model.addAttribute("pageSize", size);
        model.addAttribute("genreList", genreService.findAllGenres(Pageable.unpaged()).getContent());

        return "story";
    }

    // Xử lý hành động theo dõi/xóa theo dõi
    @PostMapping("/{storyId}/bookmark")
    public String toggleBookmark(@PathVariable Long storyId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }
        User user = userService.getUserRepository().findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Story story = storyService.findStoryById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));

        if (bookmarkService.isBookmarked(user.getId(), storyId)) {
            bookmarkService.removeBookmark(user.getId(), storyId);
        } else {
            bookmarkService.addBookmark(user, story);
        }
        return "redirect:/story/" + storyId;
    }

    // Trang chi tiết chương
    @GetMapping("/{storyId}/chapter/{chapterId}")
    public String getChapter(@PathVariable Long storyId,
                             @PathVariable Long chapterId,
                             Model model) {
        Story story = storyService.findStoryById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));
        model.addAttribute("story", story);
        story.setViews(story.getViews() + 1);


        Chapter chapter = chapterService.findChapterById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        model.addAttribute("chapter", chapter);
        model.addAttribute("chapterList", chapterService.getChapterRepository().findAllByStoryIdOrderByChapterNumberAsc(storyId));

        model.addAttribute("previousChapter", chapterService.getPreviousChapter(storyId, chapter.getChapterNumber()));
        model.addAttribute("nextChapter", chapterService.getNextChapter(storyId, chapter.getChapterNumber()));
        model.addAttribute("comments", commentService.getCommentsByChapterId(chapterId));

        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAuthenticated", currentUser != null);

        // Tạo bản ghi ReadingHistory nếu đã đăng nhập
        if (currentUser != null) {
            User user = userService.getUserRepository().findByUsername(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            ReadingHistory history = new ReadingHistory();
            history.setUser(user);
            history.setStory(story);
            history.setChapter(chapter);
            history.setLastReadAt(LocalDateTime.now());
            readingHistoryService.getReadingHistoryRepository().save(history);
        }

        model.addAttribute("genreList", genreService.findAllGenres(Pageable.unpaged()).getContent());

        return "chapter";
    }

    // Trang danh sách truyện đang theo dõi
    @GetMapping("/bookmark")
    public String getBookmarks(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Long userId = userService.getUserRepository()
                .findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Bookmark> bookmarks = bookmarkService.getBookmarkRepository().findByUserId(userId, pageable);

        model.addAttribute("stories", bookmarks);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("genreList", genreService.findAllGenres(Pageable.unpaged()).getContent());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAuthenticated", true);

        return "bookmark";
    }

    // Trang lịch sử đọc truyện
    @GetMapping("/readinghistory")
    public String getReadingHistory(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Long userId = userService.getUserRepository()
                .findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        Pageable pageable = PageRequest.of(page, size, Sort.by("lastReadAt").descending());
        Page<ReadingHistory> histories = readingHistoryService.getReadingHistoryRepository().findByUserId(userId, pageable);

        model.addAttribute("stories", histories);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("genreList", genreService.findAllGenres(Pageable.unpaged()).getContent());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAuthenticated", true);

        return "readinghistory";
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
        model.addAttribute("genreList", genreService.findAllGenres(Pageable.unpaged()).getContent());

        // Đặt giá trị mặc định nếu tham số không hợp lệ
        model.addAttribute("status", status != null && isValidStatus(status) ? status : "");
        model.addAttribute("chapters", chapters != null && isValidChapters(chapters) ? chapters : "");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortOrder", sortOrder);

        return "search";
    }

    private boolean isValidStatus(String status) {
        try {
            StoryStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isValidChapters(String chapters) {
        return chapters != null && List.of("0-100", "100-500", "500-1000", "1000+").contains(chapters);
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
            default:
                return PageRequest.of(page, size, Sort.by(direction, "title"));
        }
    }

    private Page<Story> applyFilters(String search, String status, String chapters, Pageable pageable) {
        String title = search != null ? search : "";
        StoryStatus storyStatus = null;
        Long minChapters = null;
        Long maxChapters = null;

        // Xử lý status
        if (status != null && !status.isEmpty()) {
            try {
                storyStatus = StoryStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Bỏ qua nếu status không hợp lệ
            }
        }

        // Xử lý chapters
        if (chapters != null && !chapters.isEmpty()) {
            switch (chapters) {
                case "0-100":
                    minChapters = 0L;
                    maxChapters = 99L;
                    break;
                case "100-500":
                    minChapters = 100L;
                    maxChapters = 499L;
                    break;
                case "500-1000":
                    minChapters = 500L;
                    maxChapters = 999L;
                    break;
                case "1000+":
                    minChapters = 1000L;
                    maxChapters = Long.MAX_VALUE;
                    break;
            }
        }

        if (minChapters == null || maxChapters == null) {
            if (storyStatus != null) {
                return storyService.getStoryRepository().findByTitleContainingAndStatus(title, storyStatus, pageable);
            }
            return storyService.getStoryRepository().findByTitleContaining(title, pageable);
        }

        // Lọc theo title, status và số chương
        return storyService.getStoryRepository().findByTitleContainingAndStatusAndChapterCount(
                title, storyStatus, minChapters, maxChapters, pageable);
    }

    private Page<Story> filterByChapterCount(Page<Story> stories, String chapters) {
        List<Story> filtered = stories.getContent().stream()
                .filter(story -> {
                    int count = chapterService.findAllChaptersByStoryId(story.getId(), Pageable.unpaged()).getContent().size();
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

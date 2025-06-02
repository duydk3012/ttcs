package com.duydk.ttcs.controller;

import com.duydk.ttcs.entity.Story;
import com.duydk.ttcs.entity.StoryStatus;
import com.duydk.ttcs.service.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/genre")
public class GenreController {

    private final UserService userService;
    private final StoryService storyService;
    private final ChapterService chapterService;
    private final GenreService genreService;
    private final CommentService commentService;

    public GenreController(UserService userService, StoryService storyService, ChapterService chapterService, GenreService genreService, CommentService commentService) {
        this.userService = userService;
        this.storyService = storyService;
        this.chapterService = chapterService;
        this.genreService = genreService;
        this.commentService = commentService;
    }

    @GetMapping("")
    public String listStoryByGenre(
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

        return "genre";
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

        // Nếu không có bộ lọc chapters, chỉ lọc theo title và status
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

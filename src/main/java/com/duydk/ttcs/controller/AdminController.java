package com.duydk.ttcs.controller;

import com.duydk.ttcs.dto.CreateUserDTO;
import com.duydk.ttcs.dto.AdminUpdateUserDTO;
import com.duydk.ttcs.dto.UserResponseDTO;
import com.duydk.ttcs.entity.Chapter;
import com.duydk.ttcs.entity.Genre;
import com.duydk.ttcs.entity.Story;
import com.duydk.ttcs.entity.StoryStatus;
import com.duydk.ttcs.service.ChapterService;
import com.duydk.ttcs.service.GenreService;
import com.duydk.ttcs.service.StoryService;
import com.duydk.ttcs.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final StoryService storyService;
    private final ChapterService chapterService;
    private final GenreService genreService;

    public AdminController(UserService userService, StoryService storyService, ChapterService chapterService, GenreService genreService) {
        this.userService = userService;
        this.storyService = storyService;
        this.chapterService = chapterService;
        this.genreService = genreService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("totalUsers", userService.countAllUsers());
        model.addAttribute("activeUsers", userService.countActiveUsers());
        model.addAttribute("inactiveUsers", userService.countInactiveUsers());
        model.addAttribute("newUsersToday", userService.countNewUsersToday());
        model.addAttribute("recentUsers", userService.findRecentUsers());
        return "admin/dashboard";
    }

    // User Management

    @GetMapping("/users")
    public String listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        Page<UserResponseDTO> userPage;
        if (search != null && !search.isEmpty()) {
            if (filter != null && !filter.isEmpty()) {
                boolean enabled = filter.equals("active");
                userPage = (Page<UserResponseDTO>) userService.searchUsers(search, pageable)
                        .filter(u -> u.isEnabled() == enabled);
            } else {
                userPage = userService.searchUsers(search, pageable);
            }
        } else if (filter != null && !filter.isEmpty()) {
            boolean enabled = filter.equals("active");
            userPage = userService.findByEnabled(enabled, pageable);
        } else {
            userPage = userService.findAllUsers(pageable);
        }

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", userPage.getNumber());
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("currentFilter", filter);
        model.addAttribute("search", search);

        return "admin/user/users";
    }

    @GetMapping("/users/add")
    public String addUserForm(Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("createUserDTO", new CreateUserDTO());
        return "admin/user/user-add";
    }

    @PostMapping("/users/add")
    public String addUser(@Valid @ModelAttribute("createUserDTO") CreateUserDTO createUserDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            UserResponseDTO currentUser = userService.getCurrentUserProfile();
            model.addAttribute("currentUser", currentUser);
            return "admin/user/user-add";
        }

        try {
            userService.createUser(createUserDTO, bindingResult);

            if (bindingResult.hasErrors()) {
                UserResponseDTO currentUser = userService.getCurrentUserProfile();
                model.addAttribute("currentUser", currentUser);
                return "admin/user/user-add";
            }
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully!");
            return "redirect:/admin/users?success";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create user: " + e.getMessage());
            UserResponseDTO currentUser = userService.getCurrentUserProfile();
            model.addAttribute("currentUser", currentUser);
            return "admin/user/user-add";
        }
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        UserResponseDTO user = userService.getUserById(id);
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        AdminUpdateUserDTO adminUpdateUserDTO = new AdminUpdateUserDTO();
        adminUpdateUserDTO.setFullName(user.getFullName());
        adminUpdateUserDTO.setEmail(user.getEmail());
        adminUpdateUserDTO.setEnabled(user.isEnabled());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("user", user);
        model.addAttribute("adminUpdateUserDTO", adminUpdateUserDTO);
        return "admin/user/user-edit";
    }

    @PostMapping("/users/edit/{id}")
    public String editUser(@PathVariable Long id,
                           @Valid @ModelAttribute("adminUpdateUserDTO") AdminUpdateUserDTO adminUpdateUserDTO,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            UserResponseDTO user = userService.getUserById(id);
            UserResponseDTO currentUser = userService.getCurrentUserProfile();
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("user", user);
            return "admin/user/user-edit";
        }

        try {
            if (adminUpdateUserDTO.getNewPassword() != null && !adminUpdateUserDTO.getNewPassword().isEmpty() ||
                    adminUpdateUserDTO.getConfirmPassword() != null && !adminUpdateUserDTO.getConfirmPassword().isEmpty()) {
                if (!adminUpdateUserDTO.getNewPassword().equals(adminUpdateUserDTO.getConfirmPassword())) {
                    bindingResult.rejectValue("confirmPassword", "match", "Passwords do not match");
                    UserResponseDTO user = userService.getUserById(id);
                    UserResponseDTO currentUser = userService.getCurrentUserProfile();
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("user", user);
                    return "admin/user/user-edit";
                }
            }

            userService.updateUserProfile(id, adminUpdateUserDTO);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
            return "redirect:/admin/users?success";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to update user: " + e.getMessage());
            UserResponseDTO user = userService.getUserById(id);
            UserResponseDTO currentUser = userService.getCurrentUserProfile();
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("user", user);
            return "admin/user/user-edit";
        }
    }

    @GetMapping("/users/delete")
    public String deleteUser(@RequestParam("id") Long id) {
        try {
            userService.deleteUser(id);
            return "redirect:/admin/users?success";
        } catch (Exception e) {
            return "redirect:/admin/users?error";
        }
    }

    @PostMapping("/users/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id) {
        try {
            userService.toggleUserStatus(id);
            return "redirect:/admin/users?success";
        } catch (Exception e) {
            return "redirect:/admin/users?error";
        }
    }

    @PostMapping("/users/{id}/avatar")
    public String updateUserAvatar(@PathVariable Long id,
                                   @RequestParam("avatarFile") MultipartFile avatarFile,
                                   RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserAvatarById(id, avatarFile);
            redirectAttributes.addFlashAttribute("success", "Avatar updated successfully!");
            return "redirect:/admin/users/edit/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload avatar: " + e.getMessage());
            return "redirect:/admin/users/edit/" + id;
        }
    }


    // Story Management

    @GetMapping("/stories")
    public String listStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Story> storyPage;

        if (search != null && !search.isEmpty()) {
            storyPage = storyService.findByTitleContaining(search, pageable);
        } else if (filter != null && !filter.isEmpty()) {
            try {
                StoryStatus status = StoryStatus.valueOf(filter);
                storyPage = storyService.findAllByStatus(status, pageable);
            } catch (IllegalArgumentException e) {
                storyPage = storyService.findAllStories(pageable);
                model.addAttribute("error", "Invalid filter value.");
            }
        } else {
            storyPage = storyService.findAllStories(pageable);
        }

        model.addAttribute("stories", storyPage.getContent());
        model.addAttribute("currentPage", storyPage.getNumber());
        model.addAttribute("totalPages", storyPage.getTotalPages());
        model.addAttribute("totalItems", storyPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("currentFilter", filter);
        model.addAttribute("allGenres", genreService.findAllGenres(Pageable.unpaged()).getContent());
        model.addAttribute("statusOptions", StoryStatus.values());

        return "admin/story/story-list";
    }

    @GetMapping("/stories/add")
    public String addStoryForm(Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);

        Story story = new Story();
        model.addAttribute("story", story);
        model.addAttribute("allGenres", genreService.findAllGenres(Pageable.unpaged()).getContent());
        model.addAttribute("statusOptions", StoryStatus.values());

        return "admin/story/story-form";
    }

    @PostMapping("/stories/save")
    public String saveStory(@ModelAttribute("story") Story story) {
        try {
            if (story.getId() == null) {
                story.setCreatedAt(LocalDateTime.now());
            }
            story.setUpdatedAt(LocalDateTime.now());

            storyService.saveStory(story);
            return "redirect:/admin/stories?success=Story saved successfully!";
        } catch (Exception e) {
            if (story.getId() == null) {
                return "redirect:/admin/stories/add?error=Failed to save story!";
            } else {
                return "redirect:/admin/stories/add/"+story.getId()+"?error=Failed to save story!";
            }
        }
    }

    @GetMapping("/stories/add/{id}")
    public String editStoryForm(@PathVariable Long id, Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);

        Optional<Story> story = storyService.findStoryById(id);

        model.addAttribute("story", story.get());
        model.addAttribute("allGenres", genreService.getGenreRepository().findAll());
        model.addAttribute("statusOptions", StoryStatus.values());

        return "admin/story/story-form";
    }

    @GetMapping("/stories/delete/{id}")
    public String deleteStory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            storyService.deleteStory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Story deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete story: " + e.getMessage());
        }
        return "redirect:/admin/stories";
    }

    // Chapter Management

    @GetMapping("/stories/{storyId}/chapters")
    public String listChapters(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);

        Pageable pageable = PageRequest.of(page, size, Sort.by("chapterNumber").ascending());
        Page<Chapter> chapterPage = chapterService.findAllChaptersByStoryId(storyId, pageable);

        Story story = storyService.findStoryById(storyId).get();
        model.addAttribute("story", story);

        model.addAttribute("chapters", chapterPage.getContent());
        model.addAttribute("currentPage", chapterPage.getNumber());
        model.addAttribute("totalPages", chapterPage.getTotalPages());
        model.addAttribute("totalItems", chapterPage.getTotalElements());
        model.addAttribute("pageSize", size);

        return "/admin/chapter/chapter-list";

    }

    @GetMapping("/stories/{storyId}/chapters/add")
    public String addChapterForm(
            @PathVariable Long storyId,
            Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("story", storyService.findStoryById(storyId).get());

        model.addAttribute("chapter", new Chapter());

        return "admin/chapter/chapter-form";
    }

    @GetMapping("/stories/{storyId}/chapters/add/{id}")
    public String editChapterForm(
            @PathVariable Long storyId,
            @PathVariable Long id,
            Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("story", storyService.findStoryById(storyId).get());

        Optional<Chapter> chapterOpt = chapterService.findChapterById(id);
        if (chapterOpt.isEmpty()) {
            return "redirect:/admin/stories/"+storyId+"/chapters?error=Chapter not found";
        }
        model.addAttribute("chapter", chapterOpt.get());
        return "admin/chapter/chapter-form";

    }

    @PostMapping("/stories/{storyId}/chapters/save")
    public String saveChapter(
            @PathVariable Long storyId,
            @ModelAttribute("chapter") Chapter chapter) {
        if (chapter.getStory() == null) {
            chapter.setStory(storyService.findStoryById(storyId).get());
        }
        try {
            chapterService.saveChapter(chapter);
            if (chapter.getId() != null) {
                return "redirect:/admin/stories/"+storyId+"/chapters?success=Genre added successfully!";
            } else {
                return "redirect:/admin/stories/"+storyId+"/chapters?success=Genre updated successfully!";
            }
        } catch (Exception e) {
            if (chapter.getId() != null) {
                return "redirect:/admin/stories/"+storyId+"/chapters/add/"+chapter.getId()+"?error=An error occurred while adding the genre!";
            } else {
                return "redirect:/admin/stories/"+storyId+"/chapters?error=An error occurred while updating the genre!";
            }
        }
    }


    @GetMapping("/stories/{storyId}/chapters/delete/{id}")
    public String deleteChapter(
            @PathVariable Long storyId,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            chapterService.deleteChapter(id);
            redirectAttributes.addFlashAttribute("successMessage", "Chapter deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete story: " + e.getMessage());
        }
        return "redirect:/admin/stories/{storyId}/chapters";
    }


    // Genre Management

    @GetMapping("/genres")
    public String listGenres(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {

        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);

        Pageable pageable = PageRequest.of(page, size);
        Page<Genre> genrePage;

        // Xử lý tìm kiếm và lọc
        if (search != null && !search.isEmpty()) {
            // Tìm kiếm theo tên
            genrePage = genreService.findByNameContaining(search, pageable);
        } else {
            // Lấy tất cả thể loại
            genrePage = genreService.findAllGenres(pageable);
        }

        // Thêm các thuộc tính vào model
        model.addAttribute("genres", genrePage.getContent());
        model.addAttribute("currentPage", genrePage.getNumber());
        model.addAttribute("totalPages", genrePage.getTotalPages());
        model.addAttribute("totalItems", genrePage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);

        return "admin/genre/genre-list";
    }

    @GetMapping("/genres/add")
    public String addGenreForm(Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);

        model.addAttribute("genre", new Genre());

        return "admin/genre/genre-form";
    }

    @GetMapping("/genres/add/{id}")
    public String editGenreForm(@PathVariable Long id, Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);

        Optional<Genre> genreOpt = genreService.findGenreById(id);
        if (genreOpt.isEmpty()) {
            return "redirect:/admin/genres?error=Genre not found";
        }
        model.addAttribute("genre", genreOpt.get());
        return "admin/genre/genre-form";
    }

    // Lưu genre khi add/edit
    @PostMapping("/genres/save")
    public String saveGenre(@ModelAttribute("genre") Genre genre) {
        try {
            genreService.saveGenre(genre);
            if (genre.getId() != null) {
                return "redirect:/admin/genres?success=Genre added successfully!";
            } else {
                return "redirect:/admin/genres?success=Genre updated successfully!";
            }
        } catch (Exception e) {
            if (genre.getId() != null) {
                return "redirect:/admin/genres/add/"+genre.getId()+"?error=An error occurred while adding the genre!";
            } else {
                return "redirect:/admin/genres?error=An error occurred while updating the genre!";
            }
        }
    }

    @GetMapping("/genres/delete")
    public String deleteGenre(@RequestParam("id") Long id) {
        try {
            genreService.deleteGenre(id);
            return "redirect:/admin/genres?success=Genre deleted successfully!";
        } catch (Exception e) {
            return "redirect:/admin/genres?error";
        }
    }

    // Comment Management



}
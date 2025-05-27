package com.duydk.ttcs.controller;

import com.duydk.ttcs.dto.UpdateProfileDTO;
import com.duydk.ttcs.dto.UserResponseDTO;
import com.duydk.ttcs.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
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

    /* USER MANAGEMENT */
    @GetMapping("/users")
    public String listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            Model model) {

        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);

        List<UserResponseDTO> users;
        if (search != null && !search.isEmpty()) {
            if (filter != null && !filter.isEmpty()) {
                // Kết hợp cả search và filter
                boolean enabled = filter.equals("active");
                users = userService.searchUsers(search).stream()
                        .filter(u -> u.isEnabled() == enabled)
                        .collect(Collectors.toList());
                model.addAttribute("currentFilter", filter);
            } else {
                // Chỉ search
                users = userService.searchUsers(search);
            }
        } else if (filter != null && !filter.isEmpty()) {
            // Chỉ filter
            boolean enabled = filter.equals("active");
            users = userService.findByEnabled(enabled);
            model.addAttribute("currentFilter", filter);
        } else {
            // Không có search/filter
            users = userService.findAllUsers();
        }
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        UserResponseDTO user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "admin/edit-user";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return "redirect:/admin/users";
    }
}
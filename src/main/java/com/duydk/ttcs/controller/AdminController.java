package com.duydk.ttcs.controller;

import com.duydk.ttcs.dto.CreateUserDTO;
import com.duydk.ttcs.dto.AdminUpdateUserDTO;
import com.duydk.ttcs.dto.RegisterDTO;
import com.duydk.ttcs.dto.UserResponseDTO;
import com.duydk.ttcs.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // Hiển thị trang dashboard của admin
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

    // Hiển thị danh sách người dùng với tìm kiếm và lọc
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
                // Kết hợp tìm kiếm và lọc trạng thái
                boolean enabled = filter.equals("active");
                users = userService.searchUsers(search).stream()
                        .filter(u -> u.isEnabled() == enabled)
                        .collect(Collectors.toList());
                model.addAttribute("currentFilter", filter);
            } else {
                // Chỉ tìm kiếm
                users = userService.searchUsers(search);
            }
        } else if (filter != null && !filter.isEmpty()) {
            // Chỉ lọc trạng thái
            boolean enabled = filter.equals("active");
            users = userService.findByEnabled(enabled);
            model.addAttribute("currentFilter", filter);
        } else {
            // Lấy tất cả người dùng
            users = userService.findAllUsers();
        }
        model.addAttribute("users", users);
        return "admin/users";
    }

    // Hiển thị form thêm người dùng mới
    @GetMapping("/users/add")
    public String addUserForm(Model model) {
        UserResponseDTO currentUser = userService.getCurrentUserProfile();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("createUserDTO", new CreateUserDTO());
        return "admin/user-add";
    }

    // Xử lý thêm người dùng mới
    @PostMapping("/users/add")
    public String addUser(@Valid @ModelAttribute("createUserDTO") CreateUserDTO createUserDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        // Kiểm tra lỗi validate
        if (bindingResult.hasErrors()) {
            UserResponseDTO currentUser = userService.getCurrentUserProfile();
            model.addAttribute("currentUser", currentUser);
            return "admin/user-add";
        }

        try {
            // Tạo người dùng mới
            userService.createUser(createUserDTO, bindingResult);

            if (bindingResult.hasErrors()) {
                UserResponseDTO currentUser = userService.getCurrentUserProfile();
                model.addAttribute("currentUser", currentUser);
                return "admin/user-add";
            }
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully!");
            return "redirect:/admin/users?success";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create user: " + e.getMessage());
            UserResponseDTO currentUser = userService.getCurrentUserProfile();
            model.addAttribute("currentUser", currentUser);
            return "admin/user-add";
        }
    }

    // Hiển thị form chỉnh sửa người dùng
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
        return "admin/user-edit";
    }

    // Xử lý chỉnh sửa người dùng
    @PostMapping("/users/edit/{id}")
    public String editUser(@PathVariable Long id,
                           @Valid @ModelAttribute("adminUpdateUserDTO") AdminUpdateUserDTO adminUpdateUserDTO,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        // Kiểm tra lỗi validate từ các trường khác (trừ password nếu rỗng)
        if (bindingResult.hasErrors()) {
            UserResponseDTO user = userService.getUserById(id);
            UserResponseDTO currentUser = userService.getCurrentUserProfile();
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("user", user);
            return "admin/user-edit";
        }

        try {
            // Chỉ kiểm tra mật khẩu nếu một trong hai trường không rỗng
            if (adminUpdateUserDTO.getNewPassword() != null && !adminUpdateUserDTO.getNewPassword().isEmpty() ||
                    adminUpdateUserDTO.getConfirmPassword() != null && !adminUpdateUserDTO.getConfirmPassword().isEmpty()) {
                if (!adminUpdateUserDTO.getNewPassword().equals(adminUpdateUserDTO.getConfirmPassword())) {
                    bindingResult.rejectValue("confirmPassword", "match", "Passwords do not match");
                    UserResponseDTO user = userService.getUserById(id);
                    UserResponseDTO currentUser = userService.getCurrentUserProfile();
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("user", user);
                    return "admin/user-edit";
                }
            }

            // Cập nhật thông tin người dùng
            userService.updateUserProfile(id.toString(), adminUpdateUserDTO);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
            return "redirect:/admin/users?success";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to update user: " + e.getMessage());
            UserResponseDTO user = userService.getUserById(id);
            UserResponseDTO currentUser = userService.getCurrentUserProfile();
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("user", user);
            return "admin/user-edit";
        }
    }

    // Xóa người dùng
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return "redirect:/admin/users?success";
        } catch (Exception e) {
            return "redirect:/admin/users?error";
        }
    }

    // Bật/tắt trạng thái người dùng
    @PostMapping("/users/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id) {
        try {
            userService.toggleUserStatus(id);
            return "redirect:/admin/users?success";
        } catch (Exception e) {
            return "redirect:/admin/users?error";
        }
    }

    // Xử lý upload avatar
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
}
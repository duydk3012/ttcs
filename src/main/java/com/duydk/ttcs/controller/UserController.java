package com.duydk.ttcs.controller;

import com.duydk.ttcs.dto.UpdateProfileDTO;
import com.duydk.ttcs.dto.UserResponseDTO;
import com.duydk.ttcs.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
@RequestMapping("/user")
@PreAuthorize("hasRole('USER')")
public class UserController {

    private final UserService userService;

    @Value("${app.upload.dir:${user.home}}")
    private String uploadDir;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String userProfile(Model model) {
        UserResponseDTO user = userService.getCurrentUserProfile();
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("updateProfileDTO", new UpdateProfileDTO());
        return "user/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute UpdateProfileDTO updateProfileDTO,
                                BindingResult result,
                                Model model) {
        if (result.hasErrors()) {
            UserResponseDTO user = userService.getCurrentUserProfile();
            model.addAttribute("user", user);
            return "user/profile";
        }

        try {
            userService.updateCurrentUserProfile(updateProfileDTO);
            return "redirect:/user/profile?success";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            UserResponseDTO user = userService.getCurrentUserProfile();
            model.addAttribute("user", user);
            return "user/profile";
        }
    }

    @PostMapping("/profile/avatar")
    public String updateAvatar(@RequestParam("avatarFile") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserAvatar(file);
            return "redirect:/user/profile?success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload avatar: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new RuntimeException("New password and confirmation do not match");
            }

            userService.changePassword(currentPassword, newPassword);
            return "redirect:/user/profile?success";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/profile";
        }
    }
}
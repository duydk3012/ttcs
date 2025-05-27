package com.duydk.ttcs.controller;

import com.duydk.ttcs.dto.RegisterDTO;
import com.duydk.ttcs.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Hiển thị form login
    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "success", required = false) String success,
            Model model) {

        if (error != null) {
            model.addAttribute("error", "Invalid username or password!");
        }

        if (logout != null) {
            model.addAttribute("msg", "You have been logged out successfully.");
        }

        if (success != null) {
            model.addAttribute("msg", "Registration successful! You can now login.");
        }

        return "login";
    }

    // Hiển thị form đăng ký
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "register";
    }

    // Xử lý submit form đăng ký
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            // Gọi service và truyền bindingResult để nhận multiple errors
            userService.registerUser(registerDTO, bindingResult);

            // Nếu có lỗi thì trả về form đăng ký
            if (bindingResult.hasErrors()) {
                return "register";
            }

            return "redirect:/auth/login?success";
        } catch (Exception e) {
            bindingResult.reject("error.general", "Registration failed: " + e.getMessage());
            return "register";
        }
    }
}
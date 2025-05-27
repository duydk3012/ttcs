package com.duydk.ttcs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

// DTO dùng để tạo mới user
@Data
public class CreateUserDTO {

    // Username, bắt buộc, từ 3 đến 50 ký tự
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    // Tên đầy đủ, không bắt buộc, tối đa 100 ký tự
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    // Email, bắt buộc, đúng định dạng
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    // Mật khẩu, bắt buộc, từ 6 đến 100 ký tự
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    // Xác nhận mật khẩu, bắt buộc, từ 6 đến 100 ký tự
    @NotBlank(message = "Confirm password is required")
    @Size(min = 6, max = 100, message = "Confirm password must be between 6 and 100 characters")
    private String confirmPassword;

    // Trạng thái user, mặc định là active
    private boolean enabled = true;
}
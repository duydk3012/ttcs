package com.duydk.ttcs.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

// DTO dùng cho admin để cập nhật thông tin user
@Data
public class AdminUpdateUserDTO {

    // Tên đầy đủ, không bắt buộc, tối đa 100 ký tự
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    // Email, phải đúng định dạng
    @Email(message = "Invalid email format")
    private String email;

    // URL avatar, có thể null
    private String avatarUrl;

    // Mật khẩu mới, tối thiểu 6 ký tự, có thể null
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    // Xác nhận mật khẩu mới, dùng để validate
    @Size(min = 6, message = "Confirm password must be at least 6 characters")
    private String confirmPassword;

    // Trạng thái user (active/inactive)
    private boolean enabled;
}
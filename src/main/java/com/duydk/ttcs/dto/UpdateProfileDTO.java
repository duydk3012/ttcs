package com.duydk.ttcs.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateProfileDTO {
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    private String avatarUrl;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    private String currentPassword;
}
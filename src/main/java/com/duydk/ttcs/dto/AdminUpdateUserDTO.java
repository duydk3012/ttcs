package com.duydk.ttcs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateUserDTO {
    @Size(max = 100, message = "Full name must be less than 100 characters")
    private String fullName;

    @Email(message = "Email should be valid")
    private String email;

    private String newPassword;

    private String confirmPassword;

    private boolean enabled;
}
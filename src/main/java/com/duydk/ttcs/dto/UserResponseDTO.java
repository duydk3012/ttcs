package com.duydk.ttcs.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private boolean enabled;
    private LocalDateTime createdAt;
}
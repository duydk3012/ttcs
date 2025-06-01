package com.duydk.ttcs.service;

import com.duydk.ttcs.dto.*;
import com.duydk.ttcs.entity.User;
import com.duydk.ttcs.repository.UserRepository;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Đăng ký user mới
    public void registerUser(RegisterDTO registerDTO, BindingResult bindingResult) {
        // Kiểm tra password match
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "match", "Passwords do not match");
        }

        // Kiểm tra trùng username
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            bindingResult.rejectValue("username", "exists", "Username already exists");
        }

        // Kiểm tra trùng email
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            bindingResult.rejectValue("email", "exists", "Email already exists");
        }

        if (bindingResult.hasErrors()) {
            return;
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setFullName(registerDTO.getFullName());

        userRepository.save(user);
    }

    // Lấy thông tin user hiện tại
    public UserResponseDTO getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return null;
        }
        return getUserProfile(auth.getName());
    }

    // Lấy thông tin user bằng username
    public UserResponseDTO getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    // Cập nhật thông tin người dùng bởi admin
    public void updateUserProfile(Long id, AdminUpdateUserDTO updateDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updateDTO.getFullName() != null && !updateDTO.getFullName().isEmpty()) {
            user.setFullName(updateDTO.getFullName());
        }

        if (updateDTO.getEmail() != null && !updateDTO.getEmail().isEmpty()) {
            if (!updateDTO.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(updateDTO.getEmail())) {
                    throw new RuntimeException("Email already exists");
                }
                user.setEmail(updateDTO.getEmail());
            }
        }

        if (updateDTO.getNewPassword() != null && !updateDTO.getNewPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updateDTO.getNewPassword()));
        }

        user.setEnabled(updateDTO.isEnabled());

        userRepository.save(user);
    }

    // Cập nhật thông tin profile cho user đang đăng nhập
    public void updateCurrentUserProfile(UpdateProfileDTO updateDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updateDTO.getFullName() != null && !updateDTO.getFullName().isEmpty()) {
            user.setFullName(updateDTO.getFullName());
        }

        if (updateDTO.getEmail() != null && !updateDTO.getEmail().isEmpty()) {
            if (!updateDTO.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(updateDTO.getEmail())) {
                    throw new RuntimeException("Email already exists");
                }
                user.setEmail(updateDTO.getEmail());
            }
        }

        userRepository.save(user);
    }

    // Cập nhật avatar cho user hiện tại
    public void updateUserAvatar(MultipartFile avatarFile) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (avatarFile.isEmpty()) {
            throw new RuntimeException("Please select a file to upload");
        }

        String originalFilename = avatarFile.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));

        String filename = username + fileExtension;

        Path uploadPath = Paths.get("src/main/resources/static/images/avatar");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Files.list(uploadPath)
                .filter(path -> path.getFileName().toString().startsWith(username + "."))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete old avatar", e);
                    }
                });

        BufferedImage resizedImage = resizeImage(avatarFile, 800, 800);

        Path filePath = uploadPath.resolve(filename);
        ImageIO.write(resizedImage, fileExtension.substring(1), filePath.toFile());
        Files.copy(avatarFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        user.setAvatarUrl("/images/avatar/" + filename);
        userRepository.save(user);
    }

    // Cập nhật avatar cho người dùng theo ID
    public void updateUserAvatarById(Long userId, MultipartFile avatarFile) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (avatarFile.isEmpty()) {
            throw new RuntimeException("Please select a file to upload");
        }

        String originalFilename = avatarFile.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));

        String filename = user.getUsername() + fileExtension;

        Path uploadPath = Paths.get("src/main/resources/static/images/avatar");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Files.list(uploadPath)
                .filter(path -> path.getFileName().toString().startsWith(user.getUsername() + "."))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete old avatar", e);
                    }
                });

        BufferedImage resizedImage = resizeImage(avatarFile, 800, 800);

        Path filePath = uploadPath.resolve(filename);
        ImageIO.write(resizedImage, fileExtension.substring(1), filePath.toFile());
        Files.copy(avatarFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        user.setAvatarUrl("/images/avatar/" + filename);
        userRepository.save(user);
    }

    // Đổi mật khẩu cho user
    public void changePassword(String currentPassword, String newPassword) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private BufferedImage resizeImage(MultipartFile originalFile, int maxWidth, int maxHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(originalFile.getInputStream());

        if (originalImage.getWidth() <= maxWidth && originalImage.getHeight() <= maxHeight) {
            return originalImage;
        }

        double widthRatio = (double) maxWidth / originalImage.getWidth();
        double heightRatio = (double) maxHeight / originalImage.getHeight();
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalImage.getWidth() * ratio);
        int newHeight = (int) (originalImage.getHeight() * ratio);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resizedImage;
    }

    // Admin: Lấy tất cả users với phân trang
    public Page<UserResponseDTO> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    // Admin: Tìm kiếm user với phân trang
    public Page<UserResponseDTO> searchUsers(String keyword, Pageable pageable) {
        return userRepository.findByUsernameContainingOrFullNameContaining(keyword, keyword, pageable)
                .map(this::convertToDTO);
    }

    // Admin: Lấy user theo trạng thái enabled với phân trang
    public Page<UserResponseDTO> findByEnabled(boolean enabled, Pageable pageable) {
        return userRepository.findByEnabled(enabled, pageable)
                .map(this::convertToDTO);
    }

    // Admin: Lấy user bằng ID
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    // Admin: Xóa user
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    // Admin: Bật/tắt trạng thái user
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    // Admin: Tạo người dùng mới
    public void createUser(CreateUserDTO createUserDTO, BindingResult bindingResult) {
        if (!createUserDTO.getPassword().equals(createUserDTO.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "match", "Passwords do not match");
        }

        if (userRepository.existsByUsername(createUserDTO.getUsername())) {
            bindingResult.rejectValue("username", "exists", "Username already exists");
        }

        if (userRepository.existsByEmail(createUserDTO.getEmail())) {
            bindingResult.rejectValue("email", "exists", "Email already exists");
        }

        if (bindingResult.hasErrors()) {
            return;
        }

        User user = new User();
        user.setUsername(createUserDTO.getUsername());
        user.setEmail(createUserDTO.getEmail());
        user.setPassword(passwordEncoder.encode(createUserDTO.getPassword()));
        user.setFullName(createUserDTO.getFullName());
        user.setEnabled(true);

        userRepository.save(user);
    }

    private UserResponseDTO convertToDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public long countAllUsers() {
        return userRepository.count();
    }

    public long countActiveUsers() {
        return userRepository.countByEnabled(true);
    }

    public long countInactiveUsers() {
        return userRepository.countByEnabled(false);
    }

    public long countNewUsersToday() {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        return userRepository.countByCreatedAtGreaterThanEqual(startOfDay);
    }

    public List<UserResponseDTO> findRecentUsers() {
        return userRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
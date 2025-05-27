package com.duydk.ttcs.service;

import com.duydk.ttcs.dto.*;
import com.duydk.ttcs.model.User;
import com.duydk.ttcs.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

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

        // Nếu có lỗi thì dừng lại, không đăng ký
        if (bindingResult.hasErrors()) {
            return;
        }

        // Tiến hành đăng ký nếu không có lỗi
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setFullName(registerDTO.getFullName());

        userRepository.save(user);
    }

    // Lấy thông tin user hiện tại (cho profile)
    public UserResponseDTO getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getUserProfile(auth.getName());
    }

    // Lấy thông tin user bằng username
    public UserResponseDTO getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    // Cập nhật profile user
    public void updateUserProfile(String username, UpdateProfileDTO updateDTO) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updateDTO.getFullName() != null) {
            user.setFullName(updateDTO.getFullName());
        }

        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateDTO.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(updateDTO.getEmail());
        }

        if (updateDTO.getNewPassword() != null && !updateDTO.getNewPassword().isEmpty()) {
            if (!passwordEncoder.matches(updateDTO.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(updateDTO.getNewPassword()));
        }

        userRepository.save(user);
    }

    /**
     * Cập nhật thông tin profile cho user đang đăng nhập
     * @param updateDTO DTO chứa thông tin cập nhật
     */
    public void updateCurrentUserProfile(UpdateProfileDTO updateDTO) {
        // Lấy thông tin user đang đăng nhập từ SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Chỉ cập nhật fullName nếu có giá trị (không null và không rỗng)
        if (updateDTO.getFullName() != null && !updateDTO.getFullName().isEmpty()) {
            user.setFullName(updateDTO.getFullName());
        }

        // Chỉ cập nhật email nếu có giá trị (không null và không rỗng)
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().isEmpty()) {
            // Kiểm tra email mới có trùng với email hiện tại không
            if (!updateDTO.getEmail().equals(user.getEmail())) {
                // Kiểm tra email đã tồn tại chưa
                if (userRepository.existsByEmail(updateDTO.getEmail())) {
                    throw new RuntimeException("Email already exists");
                }
                user.setEmail(updateDTO.getEmail());
            }
        }

        userRepository.save(user);
    }

    /**
     * Cập nhật avatar cho user
     * @param avatarFile File avatar upload
     * @throws IOException nếu có lỗi khi lưu file
     */
    public void updateUserAvatar(MultipartFile avatarFile) throws IOException {
        // Lấy thông tin user đang đăng nhập
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate file
        if (avatarFile.isEmpty()) {
            throw new RuntimeException("Please select a file to upload");
        }

        // Lấy phần mở rộng của file
        String originalFilename = avatarFile.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));

        // Tạo filename theo username
        String filename = username + fileExtension;

        // Đường dẫn thư mục lưu avatar
        Path uploadPath = Paths.get("src/main/resources/static/images/avatar");

        // Tạo thư mục nếu chưa tồn tại
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Xóa avatar cũ nếu tồn tại
        Files.list(uploadPath)
                .filter(path -> path.getFileName().toString().startsWith(username + "."))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete old avatar", e);
                    }
                });

        // Resize ảnh nếu cần (giới hạn 800x800)
        BufferedImage resizedImage = resizeImage(avatarFile, 800, 800);

        // Lưu ảnh đã resize
        Path filePath = uploadPath.resolve(filename);
        ImageIO.write(resizedImage, fileExtension.substring(1), filePath.toFile());
        Files.copy(avatarFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Cập nhật đường dẫn avatar vào database
        user.setAvatarUrl("/images/avatar/" + filename);
        userRepository.save(user);
    }

    /**
     * Đổi mật khẩu cho user
     * @param currentPassword Mật khẩu hiện tại
     * @param newPassword Mật khẩu mới
     */
    public void changePassword(String currentPassword, String newPassword) {
        // Lấy thông tin user đang đăng nhập
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Validate mật khẩu mới
        if (newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        // Mã hóa và lưu mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Resize ảnh nếu kích thước quá lớn
     * @param originalFile Ảnh gốc
     * @param maxWidth Chiều rộng tối đa
     * @param maxHeight Chiều cao tối đa
     * @return File ảnh đã resize
     */
    private BufferedImage resizeImage(MultipartFile originalFile, int maxWidth, int maxHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(originalFile.getInputStream());

        // Kiểm tra nếu ảnh nhỏ hơn kích thước tối đa thì không resize
        if (originalImage.getWidth() <= maxWidth && originalImage.getHeight() <= maxHeight) {
            return originalImage;
        }

        // Tính toán kích thước mới
        double widthRatio = (double) maxWidth / originalImage.getWidth();
        double heightRatio = (double) maxHeight / originalImage.getHeight();
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalImage.getWidth() * ratio);
        int newHeight = (int) (originalImage.getHeight() * ratio);

        // Thực hiện resize
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resizedImage;
    }

    // Admin: Lấy tất cả users
    public List<UserResponseDTO> findAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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

    /**
     * Đếm tổng số user trong hệ thống
     * @return tổng số user
     */
    public long countAllUsers() {
        return userRepository.count();
    }

    /**
     * Đếm số user đang active (enabled = true)
     * @return số user active
     */
    public long countActiveUsers() {
        return userRepository.countByEnabled(true);
    }

    /**
     * Đếm số user đang inactive (enabled = false)
     * @return số user inactive
     */
    public long countInactiveUsers() {
        return userRepository.countByEnabled(false);
    }

    /**
     * Đếm số user mới tạo trong ngày hôm nay
     * @return số user mới tạo hôm nay
     */
    public long countNewUsersToday() {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        return userRepository.countByCreatedAtGreaterThanEqual(startOfDay);
    }

    /**
     * Lấy danh sách các user mới nhất (giới hạn 5 user)
     * @return danh sách user mới nhất
     */
    public List<UserResponseDTO> findRecentUsers() {
        return userRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    /**
     * Lấy danh sách tìm kiếm theo username hoặc full name
     * @return danh sách user tìm kiếm được
    * */
    public List<UserResponseDTO> searchUsers(String keyword) {
        return userRepository.findByUsernameContainingOrFullNameContaining(keyword, keyword)
                .stream().map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách theo enabled
     * @return danh sách user theo enabled
     * */
    public List<UserResponseDTO> findByEnabled(boolean enabled) {
        return userRepository.findByEnabled(enabled).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
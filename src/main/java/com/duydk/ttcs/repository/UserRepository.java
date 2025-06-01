package com.duydk.ttcs.repository;

import com.duydk.ttcs.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm user bằng username
    Optional<User> findByUsername(String username);

    // Tìm user bằng email
    Optional<User> findByEmail(String email);

    // Kiểm tra tồn tại username
    boolean existsByUsername(String username);

    // Kiểm tra tồn tại email
    boolean existsByEmail(String email);

    // Tìm user bằng username hoặc email
    Optional<User> findByUsernameOrEmail(String username, String email);

    // Đếm user theo trạng thái enabled
    long countByEnabled(boolean enabled);

    // Đếm user được tạo sau thời điểm nhất định
    long countByCreatedAtGreaterThanEqual(LocalDateTime dateTime);

    // Lấy top 5 user mới nhất
    List<User> findTop5ByOrderByCreatedAtDesc();

    // Tìm kiếm bằng từ khóa chứa trong username hoặc fullName với phân trang
    Page<User> findByUsernameContainingOrFullNameContaining(String username, String fullName, Pageable pageable);

    // Tìm kiếm active user với phân trang
    Page<User> findByEnabled(boolean enabled, Pageable pageable);

    // Lấy tất cả user với phân trang
    Page<User> findAll(Pageable pageable);
}
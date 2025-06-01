package com.duydk.ttcs.repository;

import com.duydk.ttcs.entity.Story;
import com.duydk.ttcs.entity.StoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {
    Page<Story> findAllByStatus(StoryStatus status, Pageable pageable);
    Page<Story> findByTitleContaining(String title, Pageable pageable);
    Page<Story> findAll(Pageable pageable);
    // Lấy top 10 truyện theo views (Truyện Hot)
    List<Story> findTop10ByOrderByViewsDesc();

    // Lấy top 10 truyện theo createdAt (Truyện Mới)
    List<Story> findTop10ByOrderByCreatedAtDesc();

    // Lấy top 10 truyện có status là completed (Truyện Đã Hoàn Thành)
    List<Story> findTop10ByStatusOrderByViewsDesc(StoryStatus status);
}

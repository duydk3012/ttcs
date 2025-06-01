package com.duydk.ttcs.repository;

import com.duydk.ttcs.entity.Chapter;
import com.duydk.ttcs.entity.Story;
import com.duydk.ttcs.entity.StoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {
    Page<Story> findAllByStatus(StoryStatus status, Pageable pageable);
    Page<Story> findByTitleContaining(String title, Pageable pageable);
    Page<Story> findAll(Pageable pageable);
    // Lấy top 10 truyện theo views (Truyện Hot)
    List<Story> findTop10ByOrderByViewsDesc();

    // Lấy top 10 truyện theo createdAt (Truyện Mới)
    List<Story> findTop10ByOrderByCreatedAtDesc();

    // Lấy top 10 truyện theo updatedAt (Truyện Mới Cập Nhật)
    List<Story> findTop10ByOrderByUpdatedAtDesc();
    @Query("SELECT c FROM Chapter c WHERE c.story = :story ORDER BY c.updatedAt DESC LIMIT 1")
    Chapter findLatestChapterByStory(@Param("story") Story story);

    // Lấy top 10 truyện có status là completed (Truyện Đã Hoàn Thành)
    List<Story> findTop10ByStatusOrderByViewsDesc(StoryStatus status);
}

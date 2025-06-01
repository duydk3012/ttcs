package com.duydk.ttcs.repository;

import com.duydk.ttcs.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    Page<Chapter> findAllByStoryId(Long storyId, Pageable pageable);
    // Lấy top 10 chapter theo updatedAt (Truyện Mới Cập Nhật)
    List<Chapter> findTop10ByOrderByUpdatedAtDesc();

    List<Chapter> findAllByStoryIdOrderByChapterNumberAsc(Long storyId);

    Optional<Chapter> findTopByStoryIdOrderByChapterNumberDesc(Long storyId);

    Optional<Chapter> findTopByStoryIdOrderByChapterNumberAsc(Long storyId);
}

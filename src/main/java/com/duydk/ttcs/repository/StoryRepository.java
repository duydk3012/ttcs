package com.duydk.ttcs.repository;

import com.duydk.ttcs.entity.Chapter;
import com.duydk.ttcs.entity.Genre;
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

    Page<Story> findByTitleContainingAndStatus(String title, StoryStatus status, Pageable pageable);

    @Query("SELECT s FROM Story s WHERE SIZE(s.chapters) BETWEEN :min AND :max")
    Page<Story> findByChapterCountBetween(@Param("min") int min, @Param("max") int max, Pageable pageable);

    @Query("SELECT s FROM Story s WHERE SIZE(s.chapters) >= :min")
    Page<Story> findByChapterCountGreaterThanEqual(@Param("min") int min, Pageable pageable);

    @Query("SELECT s FROM Story s WHERE (:title IS NULL OR s.title LIKE %:title%) " +
            "AND (:status IS NULL OR s.status = :status) " +
            "AND (SELECT COUNT(c) FROM Chapter c WHERE c.story.id = s.id) BETWEEN :minChapters AND :maxChapters")
    Page<Story> findByTitleContainingAndStatusAndChapterCount(
            @Param("title") String title,
            @Param("status") StoryStatus status,
            @Param("minChapters") long minChapters,
            @Param("maxChapters") long maxChapters,
            Pageable pageable);

    // Lấy danh sách Story theo Genre
    @Query("SELECT s FROM Story s JOIN s.genres g WHERE g = :genre")
    Page<Story> findByGenre(@Param("genre") Genre genre, Pageable pageable);

    // Lấy danh sách Story theo ID của Genre
    @Query("SELECT s FROM Story s JOIN s.genres g WHERE g.id = :genreId")
    Page<Story> findByGenreId(@Param("genreId") Long genreId, Pageable pageable);

}

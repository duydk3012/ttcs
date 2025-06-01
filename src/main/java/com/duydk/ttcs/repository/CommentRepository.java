package com.duydk.ttcs.repository;

import com.duydk.ttcs.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // Lấy danh sách bình luận theo storyId, sắp xếp theo updatedAt giảm dần
    List<Comment> findByStoryIdOrderByUpdatedAtDesc(Long storyId);

    // Lấy danh sách bình luận theo chapterId, sắp xếp theo updatedAt giảm dần
    List<Comment> findByChapterIdOrderByUpdatedAtDesc(Long chapterId);
}
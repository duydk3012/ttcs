package com.duydk.ttcs.repository;

import com.duydk.ttcs.entity.Bookmark;
import com.duydk.ttcs.entity.BookmarkId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {
    Page<Bookmark> findByUserId(Long userId, Pageable pageable);
    Optional<Bookmark> findByUserIdAndStoryId(Long userId, Long storyId);
}

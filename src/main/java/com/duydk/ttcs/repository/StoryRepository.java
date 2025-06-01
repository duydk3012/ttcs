package com.duydk.ttcs.repository;

import com.duydk.ttcs.entity.Story;
import com.duydk.ttcs.entity.StoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<Story, Long> {
    Page<Story> findAllByStatus(StoryStatus status, Pageable pageable);
    Page<Story> findByTitleContaining(String title, Pageable pageable);
    Page<Story> findAll(Pageable pageable);
}

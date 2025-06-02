package com.duydk.ttcs.repository;

import com.duydk.ttcs.entity.ReadingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Integer> {
    Page<ReadingHistory> findByUserId(Long userId, Pageable pageable);
}

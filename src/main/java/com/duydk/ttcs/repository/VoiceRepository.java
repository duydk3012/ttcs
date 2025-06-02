package com.duydk.ttcs.repository;

import com.duydk.ttcs.entity.Voice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceRepository extends JpaRepository<Voice, Long> {
    boolean existsByName(String name);
}

package com.duydk.ttcs.repository;

import com.duydk.ttcs.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    Page<Genre> findAll(Pageable pageable);
    Page<Genre> findByNameContaining(String name, Pageable pageable);

}

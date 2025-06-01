package com.duydk.ttcs.service;

import com.duydk.ttcs.entity.Genre;
import com.duydk.ttcs.repository.GenreRepository;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Data
@Service
@Transactional
public class GenreService {
    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    public Optional<Genre> findGenreById(Long id) {
        return genreRepository.findById(id);
    }

    public Genre saveGenre(Genre genre) {
        return genreRepository.save(genre);
    }

    public Page<Genre> findAllGenres(Pageable pageable) {
        return genreRepository.findAll(pageable);
    }
    public Page<Genre> findByNameContaining(String name, Pageable pageable) {
        return genreRepository.findByNameContaining(name, pageable);
    }

    public void deleteGenre(Long id) {
        if (!genreRepository.existsById(id)) {
            throw new RuntimeException("Genre not found");
        }
        genreRepository.deleteById(id);
    }

}

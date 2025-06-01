package com.duydk.ttcs.service;

import com.duydk.ttcs.entity.Story;
import com.duydk.ttcs.entity.StoryStatus;
import com.duydk.ttcs.repository.StoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class StoryService {

    private final StoryRepository storyRepository;
    private final GenreService genreService;

    public StoryService(StoryRepository storyRepository, GenreService genreService) {
        this.storyRepository = storyRepository;
        this.genreService = genreService;
    }

    public Page<Story> findAllStories(Pageable pageable) {
        return storyRepository.findAll(pageable);
    }

    public Page<Story> findAllByStatus(StoryStatus status, Pageable pageable) {
        return storyRepository.findAllByStatus(status, pageable);
    }

    public Page<Story> findByTitleContaining(String title, Pageable pageable) {
        return storyRepository.findByTitleContaining(title, pageable);
    }

    public Optional<Story> findStoryById(Long id) {
        return storyRepository.findById(id);
    }

    public Story saveStory(Story story) {
        return storyRepository.save(story);
    }

    public void deleteStory(Long id) {
        if (!storyRepository.existsById(id)) {
            throw new RuntimeException("Story not found");
        }
        storyRepository.deleteById(id);
    }
}

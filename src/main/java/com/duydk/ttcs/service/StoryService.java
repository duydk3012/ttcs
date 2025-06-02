package com.duydk.ttcs.service;

import com.duydk.ttcs.dto.StoryWithLatestChapterDTO;
import com.duydk.ttcs.entity.Chapter;
import com.duydk.ttcs.entity.Genre;
import com.duydk.ttcs.entity.Story;
import com.duydk.ttcs.entity.StoryStatus;
import com.duydk.ttcs.repository.StoryRepository;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
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

    // Lấy top 10 truyện hot
    public List<Story> getHotStories() {
        return storyRepository.findTop10ByOrderByViewsDesc();
    }

    // Lấy top 10 truyện mới
    public List<Story> getNewStories() {
        return storyRepository.findTop10ByOrderByCreatedAtDesc();
    }

    // Lấy top 10 truyện mới cập nhật
    public List<StoryWithLatestChapterDTO> getRecentUpdatedStories() {
        List<Story> stories = storyRepository.findTop10ByOrderByUpdatedAtDesc();

        return stories.stream()
                .map(story -> {
                    Chapter latestChapter = storyRepository.findLatestChapterByStory(story);
                    return new StoryWithLatestChapterDTO(story, latestChapter);
                })
                .collect(Collectors.toList());
    }

    // Lấy top 10 truyện đã hoàn thành
    public List<Story> getCompletedStories() {
        return storyRepository.findTop10ByStatusOrderByViewsDesc(StoryStatus.completed);
    }
    // Lấy ds Story theo GenreId
    public Page<Story> findStoriesByGenreId(Long genreId, Pageable pageable) {
        return storyRepository.findByGenreId(genreId, pageable);
    }

    // Lấy ds Story theo Genre
    public Page<Story> findStoriesByGenre(Genre genre, Pageable pageable) {
        return storyRepository.findByGenre(genre, pageable);
    }

}

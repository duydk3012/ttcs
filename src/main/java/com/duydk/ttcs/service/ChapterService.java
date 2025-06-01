package com.duydk.ttcs.service;

import com.duydk.ttcs.entity.Chapter;
import com.duydk.ttcs.entity.Genre;
import com.duydk.ttcs.repository.ChapterRepository;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Data
@Service
@Transactional
public class ChapterService {
    private final ChapterRepository chapterRepository;

    public ChapterService(ChapterRepository chapterRepository) {
        this.chapterRepository = chapterRepository;
    }

    public Page<Chapter> findAllChaptersByStoryId (Long storyId, Pageable pageable) {
        return chapterRepository.findAllByStoryId(storyId, pageable);
    }

    public Optional<Chapter> findChapterById(Long id) {
        return chapterRepository.findById(id);
    }

    public Chapter saveChapter(Chapter chapter) {
        return chapterRepository.save(chapter);
    }

    public void deleteChapter (Long chapterId) {
        chapterRepository.deleteById(chapterId);
    }

    // Lấy top 10 chapter mới cập nhật
    public List<Chapter> getRecentChapters() {
        return chapterRepository.findTop10ByOrderByUpdatedAtDesc();
    }

    // Lấy chương trước (chapterNumber nhỏ hơn lớn nhất so với chapterNumber hiện tại)
    public Chapter getPreviousChapter(Long storyId, Integer currentChapterNumber) {
        return chapterRepository.findAllByStoryId(storyId, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(chapter -> chapter.getChapterNumber() < currentChapterNumber)
                .max((c1, c2) -> Integer.compare(c1.getChapterNumber(), c2.getChapterNumber()))
                .orElse(null);
    }

    // Lấy chương sau (chapterNumber lớn hơn nhỏ nhất so với chapterNumber hiện tại)
    public Chapter getNextChapter(Long storyId, Integer currentChapterNumber) {
        return chapterRepository.findAllByStoryId(storyId, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(chapter -> chapter.getChapterNumber() > currentChapterNumber)
                .min((c1, c2) -> Integer.compare(c1.getChapterNumber(), c2.getChapterNumber()))
                .orElse(null);
    }

    // Lấy chapter.id của chương có chapterNumber lớn nhất dùng Long storyId
    public Long getChapterIdWithMaxChapterNumber(Long storyId) {
        if (storyId == null) {
            return null;
        }
        return chapterRepository.findTopByStoryIdOrderByChapterNumberDesc(storyId)
                .map(chapter -> chapter.getId())
                .orElse(null);
    }

    // Lấy chapter.id của chương có chapterNumber bé nhất dùng Long storyId
    public Long getChapterIdWithMinChapterNumber(Long storyId) {
        if (storyId == null) {
            return null;
        }
        return chapterRepository.findTopByStoryIdOrderByChapterNumberAsc(storyId)
                .map(chapter -> chapter.getId())
                .orElse(null);
    }
}

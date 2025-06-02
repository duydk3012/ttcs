package com.duydk.ttcs.dto;

import com.duydk.ttcs.entity.Chapter;
import com.duydk.ttcs.entity.Story;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoryWithLatestChapterDTO {
    private Long storyId;
    private String title;
    private String coverImage;
    private LocalDateTime storyUpdatedAt;

    private Long chapterId;
    private Integer chapterNumber;
    private String chapterTitle;
    private LocalDateTime chapterUpdatedAt;

    public StoryWithLatestChapterDTO(Story story, Chapter chapter) {
        this.storyId = story.getId();
        this.title = story.getTitle();
        this.coverImage = story.getCoverImage();
        this.storyUpdatedAt = story.getUpdatedAt();

        if (chapter != null) {
            this.chapterId = chapter.getId();
            this.chapterNumber = chapter.getChapterNumber();
            this.chapterTitle = chapter.getTitle();
            this.chapterUpdatedAt = chapter.getUpdatedAt();
        }
    }

}

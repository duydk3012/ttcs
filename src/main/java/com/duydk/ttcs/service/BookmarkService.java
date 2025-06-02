package com.duydk.ttcs.service;


import com.duydk.ttcs.entity.Bookmark;
import com.duydk.ttcs.entity.Story;
import com.duydk.ttcs.entity.User;
import com.duydk.ttcs.repository.BookmarkRepository;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Data
@Service
@Transactional
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository) {
        this.bookmarkRepository = bookmarkRepository;
    }

    public boolean isBookmarked(Long userId, Long storyId) {
        return bookmarkRepository.findByUserIdAndStoryId(userId, storyId).isPresent();
    }

    public void addBookmark(User user, Story story) {
        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setStory(story);
        bookmarkRepository.save(bookmark);
    }

    public void removeBookmark(Long userId, Long storyId) {
        bookmarkRepository.findByUserIdAndStoryId(userId, storyId)
                .ifPresent(bookmarkRepository::delete);
    }

}

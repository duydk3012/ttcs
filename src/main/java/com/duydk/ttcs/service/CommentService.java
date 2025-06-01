package com.duydk.ttcs.service;

import com.duydk.ttcs.entity.Comment;
import com.duydk.ttcs.entity.User;
import com.duydk.ttcs.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    // Lấy danh sách bình luận cho truyện
    public List<Comment> getCommentsByStoryId(Long storyId) {
        return commentRepository.findByStoryIdOrderByUpdatedAtDesc(storyId);
    }

    // Lấy danh sách bình luận cho chương
    public List<Comment> getCommentsByChapterId(Long chapterId) {
        return commentRepository.findByChapterIdOrderByUpdatedAtDesc(chapterId);
    }

    // Lưu bình luận mới
    public Comment saveComment(Comment comment) {
        return commentRepository.save(comment);
    }
}
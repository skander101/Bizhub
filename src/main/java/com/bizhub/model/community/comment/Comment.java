package com.bizhub.model.community.comment;

import java.time.LocalDateTime;

public class Comment {
    private int commentId;
    private int postId;
    private int userId;
    private String content;
    private LocalDateTime createdAt;
    private String authorName; // joined from user table, not a DB column

    public Comment() {}

    public Comment(int postId, int userId, String content) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
    }

    public int getCommentId() { return commentId; }
    public void setCommentId(int commentId) { this.commentId = commentId; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    @Override
    public String toString() {
        return "Comment{commentId=" + commentId + ", postId=" + postId + ", content='" + content + "'}";
    }
}


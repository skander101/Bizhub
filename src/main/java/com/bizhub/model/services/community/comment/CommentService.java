package com.bizhub.model.services.community.comment;

import com.bizhub.model.community.comment.Comment;
import com.bizhub.model.services.common.DB.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentService {

    private final Connection cnx;

    public CommentService() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    public void create(Comment comment) throws SQLException {
        String sql = "INSERT INTO commentaire (post_id, user_id, content) VALUES (?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, comment.getPostId());
            pst.setInt(2, comment.getUserId());
            pst.setString(3, comment.getContent());
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) comment.setCommentId(rs.getInt(1));
            }
        }
    }

    public List<Comment> findByPostId(int postId) throws SQLException {
        String sql = "SELECT c.*, u.full_name as author_name FROM commentaire c " +
                "LEFT JOIN `user` u ON c.user_id = u.user_id " +
                "WHERE c.post_id=? ORDER BY c.created_at ASC";
        List<Comment> comments = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, postId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) comments.add(mapRow(rs));
            }
        }
        return comments;
    }

    public void update(Comment comment) throws SQLException {
        String sql = "UPDATE commentaire SET content=? WHERE comment_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, comment.getContent());
            pst.setInt(2, comment.getCommentId());
            pst.executeUpdate();
        }
    }

    public void delete(int commentId) throws SQLException {
        String sql = "DELETE FROM commentaire WHERE comment_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, commentId);
            pst.executeUpdate();
        }
    }

    private Comment mapRow(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setCommentId(rs.getInt("comment_id"));
        c.setPostId(rs.getInt("post_id"));
        c.setUserId(rs.getInt("user_id"));
        c.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("created_at");
        c.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
        try { c.setAuthorName(rs.getString("author_name")); } catch (SQLException ignored) {}
        return c;
    }
}


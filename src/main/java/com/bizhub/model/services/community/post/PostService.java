package com.bizhub.model.services.community.post;

import com.bizhub.model.community.post.Post;
import com.bizhub.model.services.common.DB.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostService {

    private final Connection cnx;

    public PostService() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    public void create(Post post) throws SQLException {
        String sql = "INSERT INTO post (user_id, title, content, category) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, post.getUserId());
            pst.setString(2, post.getTitle());
            pst.setString(3, post.getContent());
            pst.setString(4, post.getCategory());
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) post.setPostId(rs.getInt(1));
            }
        }
    }

    public List<Post> findAll() throws SQLException {
        String sql = "SELECT * FROM post ORDER BY created_at DESC";
        List<Post> posts = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) posts.add(mapRow(rs));
        }
        return posts;
    }

    public Optional<Post> findById(int postId) throws SQLException {
        String sql = "SELECT * FROM post WHERE post_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, postId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    public void update(Post post) throws SQLException {
        String sql = "UPDATE post SET title=?, content=?, category=? WHERE post_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, post.getTitle());
            pst.setString(2, post.getContent());
            pst.setString(3, post.getCategory());
            pst.setInt(4, post.getPostId());
            pst.executeUpdate();
        }
    }

    public void delete(int postId) throws SQLException {
        String sql = "DELETE FROM post WHERE post_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, postId);
            pst.executeUpdate();
        }
    }

    private Post mapRow(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.setPostId(rs.getInt("post_id"));
        p.setUserId(rs.getInt("user_id"));
        p.setTitle(rs.getString("title"));
        p.setContent(rs.getString("content"));
        p.setCategory(rs.getString("category"));
        Timestamp ts = rs.getTimestamp("created_at");
        p.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
        return p;
    }
}
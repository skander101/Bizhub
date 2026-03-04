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
        String sql = "INSERT INTO post (user_id, title, content, category, media_url, media_type, location, location_lat, location_lon) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, post.getUserId());
            pst.setString(2, post.getTitle());
            pst.setString(3, post.getContent());
            pst.setString(4, post.getCategory());
            pst.setString(5, post.getMediaUrl());
            pst.setString(6, post.getMediaType());
            pst.setString(7, post.getLocation());
            pst.setObject(8, post.getLocationLat() != 0 ? post.getLocationLat() : null);
            pst.setObject(9, post.getLocationLon() != 0 ? post.getLocationLon() : null);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) post.setPostId(rs.getInt(1));
            }
        }
    }

    public List<Post> findAll() throws SQLException {
        String sql = "SELECT p.*, u.full_name as author_name FROM post p " +
                "LEFT JOIN `user` u ON p.user_id = u.user_id " +
                "ORDER BY p.created_at DESC";
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
        String sql = "UPDATE post SET title=?, content=?, category=?, media_url=?, media_type=?, location=?, location_lat=?, location_lon=? WHERE post_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, post.getTitle());
            pst.setString(2, post.getContent());
            pst.setString(3, post.getCategory());
            pst.setString(4, post.getMediaUrl());
            pst.setString(5, post.getMediaType());
            pst.setString(6, post.getLocation());
            pst.setObject(7, post.getLocationLat() != 0 ? post.getLocationLat() : null);
            pst.setObject(8, post.getLocationLon() != 0 ? post.getLocationLon() : null);
            pst.setInt(9, post.getPostId());
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
        try { p.setAuthorName(rs.getString("author_name")); } catch (SQLException ignored) {}
        try { p.setMediaUrl(rs.getString("media_url")); } catch (SQLException ignored) {}
        try { p.setMediaType(rs.getString("media_type")); } catch (SQLException ignored) {}
        try { p.setLocation(rs.getString("location")); } catch (SQLException ignored) {}
        try { p.setLocationLat(rs.getDouble("location_lat")); } catch (SQLException ignored) {}
        try { p.setLocationLon(rs.getDouble("location_lon")); } catch (SQLException ignored) {}
        return p;
    }
}


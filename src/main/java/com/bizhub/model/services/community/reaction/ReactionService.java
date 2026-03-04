package com.bizhub.model.services.community.reaction;

import com.bizhub.model.community.reaction.Reaction;
import com.bizhub.model.services.common.DB.MyDatabase;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReactionService {

    private final Connection cnx = MyDatabase.getInstance().getCnx();

    /** Add or update reaction — one per user per post (UPSERT) */
    public void upsert(int postId, int userId, String type) throws SQLException {
        String sql = "INSERT INTO reaction (post_id, user_id, type) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE type = VALUES(type)";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, postId);
            pst.setInt(2, userId);
            pst.setString(3, type);
            pst.executeUpdate();
        }
    }

    /** Remove a reaction (un-react) */
    public void delete(int postId, int userId) throws SQLException {
        String sql = "DELETE FROM reaction WHERE post_id=? AND user_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, postId);
            pst.setInt(2, userId);
            pst.executeUpdate();
        }
    }

    /** Get current user's reaction for a post — null if none */
    public String getUserReaction(int postId, int userId) throws SQLException {
        String sql = "SELECT type FROM reaction WHERE post_id=? AND user_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, postId);
            pst.setInt(2, userId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getString("type") : null;
            }
        }
    }

    /** Get reaction counts grouped by type for a post e.g. {LIKE=12, LOVE=5} */
    public Map<String, Integer> getCounts(int postId) throws SQLException {
        String sql = "SELECT type, COUNT(*) as cnt FROM reaction WHERE post_id=? GROUP BY type ORDER BY cnt DESC";
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, postId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) counts.put(rs.getString("type"), rs.getInt("cnt"));
            }
        }
        return counts;
    }

    /** Total reaction count for a post */
    public int getTotalCount(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reaction WHERE post_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, postId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}


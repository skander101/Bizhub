package com.bizhub.model.services.user_avis.review;

import com.bizhub.model.services.common.dao.MyDatabase;
import com.bizhub.model.users_avis.review.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ReviewService: Merged service combining ReviewDAO + business logic.
 * CRUD + lookup methods for Review (avis) entity.
 */
public class ReviewService {

    private final Connection cnx;

    public ReviewService() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    // ==================== DAO Methods (CRUD) ====================

    public void create(Review r) throws SQLException {
        String sql = "INSERT INTO avis(reviewer_id, formation_id, rating, comment) VALUES (?,?,?,?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, r.getReviewerId());
            pst.setInt(2, r.getFormationId());

            if (r.getRating() == null) pst.setNull(3, Types.INTEGER);
            else pst.setInt(3, r.getRating());

            pst.setString(4, r.getComment());
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) r.setAvisId(rs.getInt(1));
            }
        }
    }

    public Optional<Review> findById(int avisId) throws SQLException {
        String sql = baseSelect() + " WHERE a.avis_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, avisId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    public List<Review> findAllWithJoins(int limit, int offset) throws SQLException {
        String sql = baseSelect() + " ORDER BY a.created_at DESC LIMIT ? OFFSET ?";
        List<Review> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, limit);
            pst.setInt(2, offset);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    public List<Review> findByFormation(int formationId, int limit, int offset) throws SQLException {
        String sql = baseSelect() + " WHERE a.formation_id=? ORDER BY a.created_at DESC LIMIT ? OFFSET ?";
        List<Review> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, formationId);
            pst.setInt(2, limit);
            pst.setInt(3, offset);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    public List<Review> findByRating(int rating, int limit, int offset) throws SQLException {
        String sql = baseSelect() + " WHERE a.rating=? ORDER BY a.created_at DESC LIMIT ? OFFSET ?";
        List<Review> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, rating);
            pst.setInt(2, limit);
            pst.setInt(3, offset);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    public void update(Review r) throws SQLException {
        String sql = "UPDATE avis SET reviewer_id=?, formation_id=?, rating=?, comment=? WHERE avis_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, r.getReviewerId());
            pst.setInt(2, r.getFormationId());
            if (r.getRating() == null) pst.setNull(3, Types.INTEGER);
            else pst.setInt(3, r.getRating());
            pst.setString(4, r.getComment());
            pst.setInt(5, r.getAvisId());
            pst.executeUpdate();
        }
    }

    public void delete(int avisId) throws SQLException {
        String sql = "DELETE FROM avis WHERE avis_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, avisId);
            pst.executeUpdate();
        }
    }

    /** Average rating per formation (only where rating is not null). */
    public List<FormationAvg> getAverageRatingByFormation() throws SQLException {
        String sql = "SELECT f.formation_id, f.title, AVG(a.rating) avg_rating, COUNT(*) review_count " +
                "FROM formation f LEFT JOIN avis a ON a.formation_id=f.formation_id AND a.rating IS NOT NULL " +
                "GROUP BY f.formation_id, f.title ORDER BY avg_rating DESC";
        List<FormationAvg> out = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new FormationAvg(
                        rs.getInt("formation_id"),
                        rs.getString("title"),
                        rs.getDouble("avg_rating"),
                        rs.getInt("review_count")
                ));
            }
        }
        return out;
    }

    public Optional<Review> findByReviewerAndFormation(int reviewerId, int formationId) throws SQLException {
        String sql = baseSelect() + " WHERE a.reviewer_id=? AND a.formation_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, reviewerId);
            pst.setInt(2, formationId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    public List<Review> findAllByFormationId(int formationId) throws SQLException {
        return findByFormation(formationId, 2000, 0);
    }

    // ==================== Helper Methods ====================

    private static String baseSelect() {
        return "SELECT a.avis_id, a.reviewer_id, a.formation_id, a.rating, a.comment, a.created_at, " +
                "u.full_name reviewer_name, u.email reviewer_email, u.avatar_url reviewer_avatar_url, f.title formation_title " +
                "FROM avis a " +
                "JOIN `user` u ON u.user_id=a.reviewer_id " +
                "JOIN formation f ON f.formation_id=a.formation_id";
    }

    private static Review mapRow(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setAvisId(rs.getInt("avis_id"));
        r.setReviewerId(rs.getInt("reviewer_id"));
        r.setFormationId(rs.getInt("formation_id"));

        int rating = rs.getInt("rating");
        r.setRating(rs.wasNull() ? null : rating);

        r.setComment(rs.getString("comment"));

        Timestamp ts = rs.getTimestamp("created_at");
        r.setCreatedAt(ts == null ? null : ts.toLocalDateTime());

        r.setReviewerName(rs.getString("reviewer_name"));
        r.setReviewerEmail(rs.getString("reviewer_email"));
        r.setReviewerAvatarUrl(rs.getString("reviewer_avatar_url"));
        r.setFormationTitle(rs.getString("formation_title"));

        return r;
    }

    public record FormationAvg(int formationId, String title, double avgRating, int reviewCount) {}
}

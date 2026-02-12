package com.bizhub.legacy.service;

import com.bizhub.legacy.model.Avis;
import com.bizhub.common.dao.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceAvis implements IService<Avis> {

    private final Connection cnx;

    public ServiceAvis() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void add(Avis a) throws SQLException {
        String sql = "INSERT INTO avis(reviewer_id, formation_id, rating, comment, is_verified) VALUES (?,?,?,?,?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, a.getReviewerId());
            pst.setInt(2, a.getFormationId());

            if (a.getRating() == null) pst.setNull(3, Types.INTEGER);
            else pst.setInt(3, a.getRating());

            pst.setString(4, a.getComment());

            if (a.getVerified() == null) pst.setNull(5, Types.BOOLEAN);
            else pst.setBoolean(5, a.getVerified());

            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) a.setAvisId(rs.getInt(1));
            }
        }
    }

    @Override
    public void update(Avis a) throws SQLException {
        String sql = "UPDATE avis SET reviewer_id=?, formation_id=?, rating=?, comment=?, is_verified=? WHERE avis_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, a.getReviewerId());
            pst.setInt(2, a.getFormationId());

            if (a.getRating() == null) pst.setNull(3, Types.INTEGER);
            else pst.setInt(3, a.getRating());

            pst.setString(4, a.getComment());

            if (a.getVerified() == null) pst.setNull(5, Types.BOOLEAN);
            else pst.setBoolean(5, a.getVerified());

            pst.setInt(6, a.getAvisId());
            pst.executeUpdate();
        }
    }

    @Override
    public void delete(Avis a) throws SQLException {
        String sql = "DELETE FROM avis WHERE avis_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, a.getAvisId());
            pst.executeUpdate();
        }
    }

    @Override
    public List<Avis> getAll() throws SQLException {
        List<Avis> avisList = new ArrayList<>();
        String sql = "SELECT * FROM avis";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) avisList.add(mapRow(rs));
        }
        return avisList;
    }

    public Avis getById(int id) throws SQLException {
        String sql = "SELECT * FROM avis WHERE avis_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    public List<Avis> getByFormationId(int formationId) throws SQLException {
        List<Avis> avisList = new ArrayList<>();
        String sql = "SELECT * FROM avis WHERE formation_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, formationId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) avisList.add(mapRow(rs));
            }
        }
        return avisList;
    }

    public Avis getByReviewerAndFormation(int reviewerId, int formationId) throws SQLException {
        String sql = "SELECT * FROM avis WHERE reviewer_id=? AND formation_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, reviewerId);
            pst.setInt(2, formationId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    private static Avis mapRow(ResultSet rs) throws SQLException {
        Avis a = new Avis();
        a.setAvisId(rs.getInt("avis_id"));
        a.setReviewerId(rs.getInt("reviewer_id"));
        a.setFormationId(rs.getInt("formation_id"));

        int rating = rs.getInt("rating");
        a.setRating(rs.wasNull() ? null : rating);

        a.setComment(rs.getString("comment"));

        Timestamp ts = rs.getTimestamp("created_at");
        a.setCreatedAt(ts == null ? null : ts.toLocalDateTime());

        boolean verified = rs.getBoolean("is_verified");
        a.setVerified(rs.wasNull() ? null : verified);

        return a;
    }
}


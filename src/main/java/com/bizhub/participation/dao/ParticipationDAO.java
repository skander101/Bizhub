package com.bizhub.participation.dao;

import com.bizhub.participation.model.Participation;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParticipationDAO {

    private final Connection cnx;

    public ParticipationDAO(Connection cnx) {
        this.cnx = cnx;
    }

    public void create(Participation p) throws SQLException {
        String sql = "INSERT INTO participation(formation_id, user_id, date_affectation, remarques) VALUES (?,?,?,?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, p.getFormationId());
            pst.setInt(2, p.getUserId());
            pst.setObject(3, p.getDateAffectation() == null ? LocalDateTime.now() : p.getDateAffectation(), Types.TIMESTAMP);
            pst.setString(4, p.getRemarques());
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) p.setId(rs.getInt(1));
            }
        }
    }

    public Optional<Participation> findById(int id) throws SQLException {
        String sql = "SELECT id_candidature, formation_id, user_id, date_affectation, remarques FROM participation WHERE id_candidature=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    public List<Participation> findAll() throws SQLException {
        String sql = "SELECT id_candidature, formation_id, user_id, date_affectation, remarques FROM participation ORDER BY date_affectation DESC";
        List<Participation> out = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    public List<Participation> findByFormationId(int formationId) throws SQLException {
        String sql = "SELECT id_candidature, formation_id, user_id, date_affectation, remarques FROM participation WHERE formation_id=? ORDER BY date_affectation DESC";
        List<Participation> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, formationId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    public List<Participation> findByUserId(int userId) throws SQLException {
        String sql = "SELECT id_candidature, formation_id, user_id, date_affectation, remarques FROM participation WHERE user_id=? ORDER BY date_affectation DESC";
        List<Participation> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    /** Returns the participation if this user already participates in this formation. */
    public Optional<Participation> findByFormationAndUser(int formationId, int userId) throws SQLException {
        String sql = "SELECT id_candidature, formation_id, user_id, date_affectation, remarques FROM participation WHERE formation_id=? AND user_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, formationId);
            pst.setInt(2, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    public void update(Participation p) throws SQLException {
        String sql = "UPDATE participation SET formation_id=?, user_id=?, date_affectation=?, remarques=? WHERE id_candidature=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, p.getFormationId());
            pst.setInt(2, p.getUserId());
            pst.setObject(3, p.getDateAffectation(), Types.TIMESTAMP);
            pst.setString(4, p.getRemarques());
            pst.setInt(5, p.getId());
            pst.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM participation WHERE id_candidature=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    private static Participation mapRow(ResultSet rs) throws SQLException {
        Participation p = new Participation();
        p.setId(rs.getInt("id_candidature"));
        p.setFormationId(rs.getInt("formation_id"));
        p.setUserId(rs.getInt("user_id"));
        Timestamp ts = rs.getTimestamp("date_affectation");
        p.setDateAffectation(ts == null ? null : ts.toLocalDateTime());
        p.setRemarques(rs.getString("remarques"));
        return p;
    }
}

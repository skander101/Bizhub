package com.bizhub.model.services.user_avis.formation;

import com.bizhub.model.services.common.dao.MyDatabase;
import com.bizhub.model.users_avis.formation.Formation;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * FormationService: Merged service combining FormationDAO + business logic.
 * CRUD + lookup methods for Formation entity.
 */
public class FormationService {

    private final Connection cnx;

    public FormationService() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    // ==================== DAO Methods (CRUD) ====================

    public void create(Formation f) throws SQLException {
        String sql = "INSERT INTO formation(title, description, trainer_id, start_date, end_date, cost) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, f.getTitle());
            pst.setString(2, f.getDescription());
            pst.setInt(3, f.getTrainerId());
            pst.setDate(4, Date.valueOf(requireDate(f.getStartDate(), "start_date")));
            pst.setDate(5, Date.valueOf(requireDate(f.getEndDate(), "end_date")));
            pst.setBigDecimal(6, f.getCost() == null ? new BigDecimal("0.00") : f.getCost());
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) f.setFormationId(rs.getInt(1));
            }
        }
    }

    public Optional<Formation> findById(int formationId) throws SQLException {
        String sql = "SELECT formation_id, title, description, trainer_id, start_date, end_date, cost FROM formation WHERE formation_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, formationId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    public List<Formation> findAll() throws SQLException {
        String sql = "SELECT formation_id, title, description, trainer_id, start_date, end_date, cost FROM formation ORDER BY formation_id DESC";
        List<Formation> out = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    public List<Formation> findByTrainerId(int trainerId) throws SQLException {
        String sql = "SELECT formation_id, title, description, trainer_id, start_date, end_date, cost FROM formation WHERE trainer_id=? ORDER BY formation_id DESC";
        List<Formation> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, trainerId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    public void update(Formation f) throws SQLException {
        String sql = "UPDATE formation SET title=?, description=?, trainer_id=?, start_date=?, end_date=?, cost=? WHERE formation_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, f.getTitle());
            pst.setString(2, f.getDescription());
            pst.setInt(3, f.getTrainerId());
            pst.setDate(4, Date.valueOf(requireDate(f.getStartDate(), "start_date")));
            pst.setDate(5, Date.valueOf(requireDate(f.getEndDate(), "end_date")));
            pst.setBigDecimal(6, f.getCost() == null ? new BigDecimal("0.00") : f.getCost());
            pst.setInt(7, f.getFormationId());
            pst.executeUpdate();
        }
    }

    public void delete(int formationId) throws SQLException {
        String sql = "DELETE FROM formation WHERE formation_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, formationId);
            pst.executeUpdate();
        }
    }

    // ==================== Helper Methods ====================

    private static Formation mapRow(ResultSet rs) throws SQLException {
        Formation f = new Formation();
        f.setFormationId(rs.getInt("formation_id"));
        f.setTitle(rs.getString("title"));
        f.setDescription(rs.getString("description"));
        f.setTrainerId(rs.getInt("trainer_id"));

        Date sd = rs.getDate("start_date");
        f.setStartDate(sd == null ? null : sd.toLocalDate());
        Date ed = rs.getDate("end_date");
        f.setEndDate(ed == null ? null : ed.toLocalDate());

        f.setCost(rs.getBigDecimal("cost"));
        return f;
    }

    private static LocalDate requireDate(LocalDate d, String field) {
        if (d == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return d;
    }
}

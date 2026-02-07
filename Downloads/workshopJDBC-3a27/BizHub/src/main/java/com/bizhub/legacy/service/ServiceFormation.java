package com.bizhub.legacy.service;

import com.bizhub.legacy.model.Formation;
import com.bizhub.common.dao.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceFormation implements IService<Formation> {

    private final Connection cnx;

    public ServiceFormation() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void add(Formation f) throws SQLException {
        String sql = "INSERT INTO formation(title, description, trainer_id, start_date, end_date, cost) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, f.getTitle());
            pst.setString(2, f.getDescription());
            pst.setInt(3, f.getTrainerId());
            pst.setDate(4, Date.valueOf(f.getStartDate()));
            pst.setDate(5, Date.valueOf(f.getEndDate()));
            setBigDecimalOrNull(pst, 6, f.getCost());

            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) f.setFormationId(rs.getInt(1));
            }
        }
    }

    @Override
    public void update(Formation f) throws SQLException {
        String sql = "UPDATE formation SET title=?, description=?, trainer_id=?, start_date=?, end_date=?, cost=? WHERE formation_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, f.getTitle());
            pst.setString(2, f.getDescription());
            pst.setInt(3, f.getTrainerId());
            pst.setDate(4, Date.valueOf(f.getStartDate()));
            pst.setDate(5, Date.valueOf(f.getEndDate()));
            setBigDecimalOrNull(pst, 6, f.getCost());
            pst.setInt(7, f.getFormationId());
            pst.executeUpdate();
        }
    }

    @Override
    public void delete(Formation f) throws SQLException {
        String sql = "DELETE FROM formation WHERE formation_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, f.getFormationId());
            pst.executeUpdate();
        }
    }

    @Override
    public List<Formation> getAll() throws SQLException {
        List<Formation> formations = new ArrayList<>();
        String sql = "SELECT * FROM formation";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) formations.add(mapRow(rs));
        }
        return formations;
    }

    public Formation getById(int id) throws SQLException {
        String sql = "SELECT * FROM formation WHERE formation_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    public List<Formation> getByTrainerId(int trainerId) throws SQLException {
        List<Formation> formations = new ArrayList<>();
        String sql = "SELECT * FROM formation WHERE trainer_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, trainerId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) formations.add(mapRow(rs));
            }
        }
        return formations;
    }

    private static Formation mapRow(ResultSet rs) throws SQLException {
        Formation f = new Formation();
        f.setFormationId(rs.getInt("formation_id"));
        f.setTitle(rs.getString("title"));
        f.setDescription(rs.getString("description"));
        f.setTrainerId(rs.getInt("trainer_id"));
        f.setStartDate(rs.getDate("start_date").toLocalDate());
        f.setEndDate(rs.getDate("end_date").toLocalDate());
        f.setCost(rs.getBigDecimal("cost"));
        return f;
    }

    private static void setBigDecimalOrNull(PreparedStatement pst, int index, BigDecimal v) throws SQLException {
        if (v == null) pst.setNull(index, Types.DECIMAL);
        else pst.setBigDecimal(index, v);
    }
}


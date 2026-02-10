package com.bizhub.Investistment.Services;

import com.bizhub.Investistment.Controllers.AddInvestmentController;
import com.bizhub.Investistment.Entitites.Investment;
import com.bizhub.Investistment.Entitites.User;
import com.bizhub.Investistment.Utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvestmentServiceImpl implements IInvestmentService {


    public List<AddInvestmentController.ProjectItem> getProjects() throws SQLException {
        List<AddInvestmentController.ProjectItem> list = new ArrayList<>();

        String sql = "SELECT project_id, title FROM project";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new AddInvestmentController.ProjectItem(
                        rs.getInt("project_id"),
                        rs.getString("title")
                ));
            }
        }

        return list;
    }

    public List<User> getInvestors() throws SQLException {
        List<User> list = new ArrayList<>();

        String sql = "SELECT user_id, user_type FROM user WHERE user_type = 'investisseur'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("user_type")
                ));
            }
        }

        return list;
    }

    @Override
    public int add(Investment investment) throws SQLException {
        String sql = "INSERT INTO investment (project_id, investor_id, amount, contract_url) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, investment.getProjectId());
            pstmt.setInt(2, investment.getInvestorId());
            pstmt.setBigDecimal(3, investment.getAmount());
            pstmt.setString(4, investment.getContractUrl());

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public boolean update(Investment investment) throws SQLException {
        String sql = "UPDATE investment SET project_id = ?, investor_id = ?, amount = ?, contract_url = ? WHERE investment_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, investment.getProjectId());
            pstmt.setInt(2, investment.getInvestorId());
            pstmt.setBigDecimal(3, investment.getAmount());
            pstmt.setString(4, investment.getContractUrl());
            pstmt.setInt(5, investment.getInvestmentId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM investment WHERE investment_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<Investment> getAll() throws SQLException {
        List<Investment> investments = new ArrayList<>();

        String sql = "SELECT i.*, p.title AS project_title, u.email AS investor_name " +
                "FROM investment i " +
                "LEFT JOIN project p ON i.project_id = p.project_id " +
                "LEFT JOIN user u ON i.investor_id = u.user_id " +
                "ORDER BY i.investment_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Investment inv = new Investment();
                inv.setInvestmentId(rs.getInt("investment_id"));
                inv.setProjectId(rs.getInt("project_id"));
                inv.setInvestorId(rs.getInt("investor_id"));
                inv.setAmount(rs.getBigDecimal("amount"));
                inv.setInvestmentDate(rs.getTimestamp("investment_date").toLocalDateTime());
                inv.setContractUrl(rs.getString("contract_url"));
                inv.setProjectTitle(rs.getString("project_title"));
                inv.setInvestorName(rs.getString("investor_name"));
                investments.add(inv);
            }
        }

        return investments;
    }


    @Override
    public Investment getById(int id) throws SQLException {
        String sql = "SELECT i.*, p.title AS project_title, u.email AS investor_name " +
                "FROM investment i " +
                "LEFT JOIN project p ON i.project_id = p.project_id " +
                "LEFT JOIN user u ON i.investor_id = u.user_id " +
                "WHERE i.investment_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Investment inv = new Investment();
                inv.setInvestmentId(rs.getInt("investment_id"));
                inv.setProjectId(rs.getInt("project_id"));
                inv.setInvestorId(rs.getInt("investor_id"));
                inv.setAmount(rs.getBigDecimal("amount"));
                inv.setInvestmentDate(rs.getTimestamp("investment_date").toLocalDateTime());
                inv.setContractUrl(rs.getString("contract_url"));
                inv.setProjectTitle(rs.getString("project_title"));
                inv.setInvestorName(rs.getString("investor_name"));
                return inv;
            }
        }

        return null;
    }

}
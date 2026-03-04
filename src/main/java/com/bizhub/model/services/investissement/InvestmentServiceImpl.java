package com.bizhub.model.services.investissement;

import com.bizhub.model.investissement.Investment;
import com.bizhub.model.investissement.InvestissementUser;
import com.bizhub.model.investissement.ProjectItem;
import com.bizhub.model.services.common.DB.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvestmentServiceImpl implements IInvestmentService {

    private Connection getConnection() {
        return MyDatabase.getInstance().getCnx();
    }

    public List<ProjectItem> getProjects() throws SQLException {
        List<ProjectItem> list = new ArrayList<>();

        String sql = "SELECT project_id, title FROM project";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new ProjectItem(
                        rs.getInt("project_id"),
                        rs.getString("title")
                ));
            }
        }

        return list;
    }

    public List<InvestissementUser> getInvestors() throws SQLException {
        List<InvestissementUser> list = new ArrayList<>();

        String sql = "SELECT user_id, user_type FROM user WHERE user_type = 'investisseur'";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new InvestissementUser(
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

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {

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

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {

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

        try (Statement stmt = getConnection().createStatement();
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

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {

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


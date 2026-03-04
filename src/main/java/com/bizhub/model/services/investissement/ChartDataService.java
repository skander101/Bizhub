package com.bizhub.model.services.investissement;

import com.bizhub.model.services.common.DB.MyDatabase;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ChartDataService {

    private static final Logger logger = Logger.getLogger(ChartDataService.class.getName());

    private Connection getConn() {
        return MyDatabase.getInstance().getCnx();
    }

    public Map<String, Double> getInvestmentsBySector() {
        Map<String, Double> data = new LinkedHashMap<>();
        String sql = """
            SELECT COALESCE(u.sector, 'Other') AS sector, SUM(i.amount) AS total
            FROM investment i
            JOIN project p ON i.project_id = p.project_id
            JOIN user u ON p.startup_id = u.user_id
            GROUP BY u.sector
            ORDER BY total DESC""";
        try (PreparedStatement stmt = getConn().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                data.put(rs.getString("sector"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            logger.warning("Error fetching investments by sector: " + e.getMessage());
        }
        return data;
    }

    public Map<String, Double> getMonthlyInvestmentVolume() {
        Map<String, Double> data = new LinkedHashMap<>();
        String sql = """
            SELECT DATE_FORMAT(investment_date, '%Y-%m') AS month, SUM(amount) AS total
            FROM investment
            WHERE investment_date > DATE_SUB(NOW(), INTERVAL 12 MONTH)
            GROUP BY month
            ORDER BY month ASC""";
        try (PreparedStatement stmt = getConn().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                data.put(rs.getString("month"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            logger.warning("Error fetching monthly volume: " + e.getMessage());
        }
        return data;
    }

    public Map<String, double[]> getProjectComparison() {
        Map<String, double[]> data = new LinkedHashMap<>();
        String sql = """
            SELECT p.title,
                   p.required_budget,
                   COALESCE(SUM(i.amount), 0) AS total_invested
            FROM project p
            LEFT JOIN investment i ON p.project_id = i.project_id
            GROUP BY p.project_id, p.title, p.required_budget
            ORDER BY p.required_budget DESC
            LIMIT 10""";
        try (PreparedStatement stmt = getConn().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String title = rs.getString("title");
                if (title.length() > 20) title = title.substring(0, 17) + "...";
                data.put(title, new double[]{
                        rs.getDouble("required_budget"),
                        rs.getDouble("total_invested")
                });
            }
        } catch (SQLException e) {
            logger.warning("Error fetching project comparison: " + e.getMessage());
        }
        return data;
    }

    public Map<String, Integer> getProjectsByStatus() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) AS count FROM project GROUP BY status";
        try (PreparedStatement stmt = getConn().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                data.put(rs.getString("status"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            logger.warning("Error fetching projects by status: " + e.getMessage());
        }
        return data;
    }

    public Map<String, Integer> getDealsByStatus() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) AS count FROM deal GROUP BY status";
        try (PreparedStatement stmt = getConn().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                data.put(rs.getString("status"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            logger.warning("Error fetching deals by status: " + e.getMessage());
        }
        return data;
    }

    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            Connection conn = getConn();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM project");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) stats.put("totalProjects", rs.getInt(1));
            }
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM investment");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) stats.put("totalInvestments", rs.getInt(1));
            }
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM investment");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) stats.put("totalVolume", rs.getDouble(1));
            }
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM user WHERE user_type='investisseur'");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) stats.put("totalInvestors", rs.getInt(1));
            }
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM deal");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) stats.put("totalDeals", rs.getInt(1));
            }
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM negotiation WHERE status='open'");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) stats.put("activeNegotiations", rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.warning("Error fetching platform stats: " + e.getMessage());
        }
        return stats;
    }
}

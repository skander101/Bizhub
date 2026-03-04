package com.bizhub.model.services.investissement;

import com.bizhub.model.investissement.Project;
import com.bizhub.model.services.common.DB.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectServiceImpl implements IProjectService {

    @Override
    public int add(Project project) throws SQLException {
        String sql = "INSERT INTO project (startup_id, title, description, required_budget, status) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, project.getStartupId());
            pstmt.setString(2, project.getTitle());

            if (project.getDescription() == null || project.getDescription().trim().isEmpty()) {
                pstmt.setNull(3, Types.VARCHAR);
            } else {
                pstmt.setString(3, project.getDescription());
            }

            pstmt.setBigDecimal(4, project.getRequiredBudget());
            pstmt.setString(5, project.getStatus());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public boolean update(Project project) throws SQLException {
        String sql = "UPDATE project SET startup_id=?, title=?, description=?, required_budget=?, status=? " +
                "WHERE project_id=?";

        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, project.getStartupId());
            pstmt.setString(2, project.getTitle());

            if (project.getDescription() == null || project.getDescription().trim().isEmpty()) {
                pstmt.setNull(3, Types.VARCHAR);
            } else {
                pstmt.setString(3, project.getDescription());
            }

            pstmt.setBigDecimal(4, project.getRequiredBudget());
            pstmt.setString(5, project.getStatus());
            pstmt.setInt(6, project.getProjectId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int projectId) throws SQLException {
        String sql = "DELETE FROM project WHERE project_id=?";

        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<Project> getAll() throws SQLException {
        String sql = "SELECT * FROM project ORDER BY created_at DESC";
        List<Project> list = new ArrayList<>();

        try (Connection conn = MyDatabase.getInstance().getCnx();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) list.add(mapProject(rs));
        }
        return list;
    }

    @Override
    public List<Project> getAllWithStats() throws SQLException {
        String sql = "SELECT p.*, " +
                "COALESCE(SUM(i.amount), 0) AS total_invested, " +
                "COUNT(i.investment_id) AS investments_count " +
                "FROM project p " +
                "LEFT JOIN investment i ON p.project_id = i.project_id " +
                "GROUP BY p.project_id " +
                "ORDER BY p.created_at DESC";

        List<Project> list = new ArrayList<>();

        try (Connection conn = MyDatabase.getInstance().getCnx();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Project p = mapProject(rs);
                p.setTotalInvested(rs.getBigDecimal("total_invested"));
                p.setInvestmentsCount(rs.getInt("investments_count"));
                list.add(p);
            }
        }
        return list;
    }

    @Override
    public Project getById(int projectId) throws SQLException {
        String sql = "SELECT * FROM project WHERE project_id=?";

        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapProject(rs);
            }
        }
        return null;
    }

    private Project mapProject(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setProjectId(rs.getInt("project_id"));
        p.setStartupId(rs.getInt("startup_id"));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setRequiredBudget(rs.getBigDecimal("required_budget"));
        p.setStatus(rs.getString("status"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) p.setCreatedAt(ts.toLocalDateTime());

        try { p.setWebsiteUrl(rs.getString("website_url")); } catch (SQLException ignored) {}
        try { p.setLogoUrl(rs.getString("logo_url")); } catch (SQLException ignored) {}

        p.setTotalInvested(BigDecimal.ZERO);
        p.setInvestmentsCount(0);
        return p;
    }
}

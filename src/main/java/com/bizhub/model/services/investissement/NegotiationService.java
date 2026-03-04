package com.bizhub.model.services.investissement;

import com.bizhub.model.investissement.Negotiation;
import com.bizhub.model.investissement.NegotiationMessage;
import com.bizhub.model.services.common.DB.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class NegotiationService {

    private static final Logger logger = Logger.getLogger(NegotiationService.class.getName());

    public Negotiation create(Negotiation neg) throws SQLException {
        String sql = "INSERT INTO negotiation (project_id, investor_id, startup_id, status, proposed_amount) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, neg.getProjectId());
            stmt.setInt(2, neg.getInvestorId());
            stmt.setInt(3, neg.getStartupId());
            stmt.setString(4, neg.getStatus());
            stmt.setBigDecimal(5, neg.getProposedAmount());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) neg.setNegotiationId(rs.getInt(1));
            }
        }
        return neg;
    }

    public Negotiation getById(int negotiationId) throws SQLException {
        String sql = """
            SELECT n.*, p.title AS project_title,
                   inv.full_name AS investor_name, st.full_name AS startup_name
            FROM negotiation n
            JOIN project p ON n.project_id = p.project_id
            JOIN user inv ON n.investor_id = inv.user_id
            JOIN user st ON n.startup_id = st.user_id
            WHERE n.negotiation_id = ?""";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, negotiationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapNegotiation(rs);
            }
        }
        return null;
    }

    public List<Negotiation> getByUserId(int userId) throws SQLException {
        String sql = """
            SELECT n.*, p.title AS project_title,
                   inv.full_name AS investor_name, st.full_name AS startup_name
            FROM negotiation n
            JOIN project p ON n.project_id = p.project_id
            JOIN user inv ON n.investor_id = inv.user_id
            JOIN user st ON n.startup_id = st.user_id
            WHERE n.investor_id = ? OR n.startup_id = ?
            ORDER BY n.updated_at DESC""";
        List<Negotiation> list = new ArrayList<>();
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapNegotiation(rs));
            }
        }
        return list;
    }

    public List<Negotiation> getByProjectId(int projectId) throws SQLException {
        String sql = """
            SELECT n.*, p.title AS project_title,
                   inv.full_name AS investor_name, st.full_name AS startup_name
            FROM negotiation n
            JOIN project p ON n.project_id = p.project_id
            JOIN user inv ON n.investor_id = inv.user_id
            JOIN user st ON n.startup_id = st.user_id
            WHERE n.project_id = ?
            ORDER BY n.created_at DESC""";
        List<Negotiation> list = new ArrayList<>();
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapNegotiation(rs));
            }
        }
        return list;
    }

    public void updateStatus(int negotiationId, String status, BigDecimal finalAmount) throws SQLException {
        String sql = "UPDATE negotiation SET status = ?, final_amount = ? WHERE negotiation_id = ?";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setBigDecimal(2, finalAmount);
            stmt.setInt(3, negotiationId);
            stmt.executeUpdate();
        }
    }

    // --- Messages ---

    public NegotiationMessage addMessage(NegotiationMessage msg) throws SQLException {
        String sql = "INSERT INTO negotiation_message (negotiation_id, sender_id, message, message_type, proposed_amount, sentiment) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, msg.getNegotiationId());
            stmt.setInt(2, msg.getSenderId());
            stmt.setString(3, msg.getMessage());
            stmt.setString(4, msg.getMessageType());
            stmt.setBigDecimal(5, msg.getProposedAmount());
            stmt.setString(6, msg.getSentiment());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) msg.setMessageId(rs.getInt(1));
            }
        }
        return msg;
    }

    public List<NegotiationMessage> getMessages(int negotiationId) throws SQLException {
        String sql = """
            SELECT m.*, u.full_name AS sender_name, u.avatar_url AS sender_avatar_url
            FROM negotiation_message m
            JOIN user u ON m.sender_id = u.user_id
            WHERE m.negotiation_id = ?
            ORDER BY m.created_at ASC""";
        List<NegotiationMessage> list = new ArrayList<>();
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, negotiationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapMessage(rs));
            }
        }
        return list;
    }

    public List<NegotiationMessage> getRecentMessages(int negotiationId, int limit) throws SQLException {
        String sql = """
            SELECT m.*, u.full_name AS sender_name, u.avatar_url AS sender_avatar_url
            FROM negotiation_message m
            JOIN user u ON m.sender_id = u.user_id
            WHERE m.negotiation_id = ?
            ORDER BY m.created_at DESC LIMIT ?""";
        List<NegotiationMessage> list = new ArrayList<>();
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, negotiationId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapMessage(rs));
            }
        }
        java.util.Collections.reverse(list);
        return list;
    }

    private Negotiation mapNegotiation(ResultSet rs) throws SQLException {
        Negotiation n = new Negotiation();
        n.setNegotiationId(rs.getInt("negotiation_id"));
        n.setProjectId(rs.getInt("project_id"));
        n.setInvestorId(rs.getInt("investor_id"));
        n.setStartupId(rs.getInt("startup_id"));
        n.setStatus(rs.getString("status"));
        n.setProposedAmount(rs.getBigDecimal("proposed_amount"));
        n.setFinalAmount(rs.getBigDecimal("final_amount"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) n.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) n.setUpdatedAt(updated.toLocalDateTime());
        n.setProjectTitle(rs.getString("project_title"));
        n.setInvestorName(rs.getString("investor_name"));
        n.setStartupName(rs.getString("startup_name"));
        return n;
    }

    private NegotiationMessage mapMessage(ResultSet rs) throws SQLException {
        NegotiationMessage m = new NegotiationMessage();
        m.setMessageId(rs.getInt("message_id"));
        m.setNegotiationId(rs.getInt("negotiation_id"));
        m.setSenderId(rs.getInt("sender_id"));
        m.setMessage(rs.getString("message"));
        m.setMessageType(rs.getString("message_type"));
        m.setProposedAmount(rs.getBigDecimal("proposed_amount"));
        m.setSentiment(rs.getString("sentiment"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) m.setCreatedAt(created.toLocalDateTime());
        m.setSenderName(rs.getString("sender_name"));
        m.setSenderAvatarUrl(rs.getString("sender_avatar_url"));
        return m;
    }
}

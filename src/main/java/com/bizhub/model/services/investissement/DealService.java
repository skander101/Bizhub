package com.bizhub.model.services.investissement;

import com.bizhub.model.investissement.Deal;
import com.bizhub.model.services.common.DB.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DealService {

    private static final Logger logger = Logger.getLogger(DealService.class.getName());

    public Deal create(Deal deal) throws SQLException {
        String sql = """
            INSERT INTO deal (negotiation_id, project_id, buyer_id, seller_id, amount, status)
            VALUES (?, ?, ?, ?, ?, ?)""";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, deal.getNegotiationId());
            stmt.setInt(2, deal.getProjectId());
            stmt.setInt(3, deal.getBuyerId());
            stmt.setInt(4, deal.getSellerId());
            stmt.setBigDecimal(5, deal.getAmount());
            stmt.setString(6, deal.getStatus());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) deal.setDealId(rs.getInt(1));
            }
        }
        return deal;
    }

    public Deal getById(int dealId) throws SQLException {
        String sql = """
            SELECT d.*, p.title AS project_title,
                   b.full_name AS buyer_name, b.email AS buyer_email,
                   s.full_name AS seller_name, s.email AS seller_email
            FROM deal d
            JOIN project p ON d.project_id = p.project_id
            JOIN user b ON d.buyer_id = b.user_id
            JOIN user s ON d.seller_id = s.user_id
            WHERE d.deal_id = ?""";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, dealId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapDeal(rs);
            }
        }
        return null;
    }

    public List<Deal> getByUserId(int userId) throws SQLException {
        String sql = """
            SELECT d.*, p.title AS project_title,
                   b.full_name AS buyer_name, b.email AS buyer_email,
                   s.full_name AS seller_name, s.email AS seller_email
            FROM deal d
            JOIN project p ON d.project_id = p.project_id
            JOIN user b ON d.buyer_id = b.user_id
            JOIN user s ON d.seller_id = s.user_id
            WHERE d.buyer_id = ? OR d.seller_id = ?
            ORDER BY d.created_at DESC""";
        List<Deal> list = new ArrayList<>();
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapDeal(rs));
            }
        }
        return list;
    }

    public void updateStripePayment(int dealId, String paymentIntentId, String status) throws SQLException {
        String sql = "UPDATE deal SET stripe_payment_intent_id = ?, stripe_payment_status = ?, status = ? WHERE deal_id = ?";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, paymentIntentId);
            stmt.setString(2, status);
            stmt.setString(3, "succeeded".equals(status) ? "paid" : "pending_payment");
            stmt.setInt(4, dealId);
            stmt.executeUpdate();
        }
    }

    public void updateStripeCheckoutSession(int dealId, String checkoutSessionId) throws SQLException {
        String sql = "UPDATE deal SET stripe_checkout_session_id = ? WHERE deal_id = ?";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, checkoutSessionId);
            stmt.setInt(2, dealId);
            stmt.executeUpdate();
        }
    }

    public void updateContractPdf(int dealId, String pdfPath) throws SQLException {
        String sql = "UPDATE deal SET contract_pdf_path = ? WHERE deal_id = ?";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pdfPath);
            stmt.setInt(2, dealId);
            stmt.executeUpdate();
        }
    }

    public void updateYousign(int dealId, String requestId, String status) throws SQLException {
        String sql = "UPDATE deal SET yousign_signature_request_id = ?, yousign_status = ?, status = ? WHERE deal_id = ?";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, requestId);
            stmt.setString(2, status);
            stmt.setString(3, "done".equals(status) ? "signed" : "pending_signature");
            stmt.setInt(4, dealId);
            stmt.executeUpdate();
        }
    }

    public void markEmailSent(int dealId) throws SQLException {
        String sql = "UPDATE deal SET email_sent = 1 WHERE deal_id = ?";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, dealId);
            stmt.executeUpdate();
        }
    }

    public void completeDeal(int dealId) throws SQLException {
        String sql = "UPDATE deal SET status = 'completed', completed_at = NOW() WHERE deal_id = ?";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, dealId);
            stmt.executeUpdate();
        }
    }

    private Deal mapDeal(ResultSet rs) throws SQLException {
        Deal d = new Deal();
        d.setDealId(rs.getInt("deal_id"));
        d.setNegotiationId(rs.getObject("negotiation_id") != null ? rs.getInt("negotiation_id") : null);
        d.setProjectId(rs.getInt("project_id"));
        d.setBuyerId(rs.getInt("buyer_id"));
        d.setSellerId(rs.getInt("seller_id"));
        d.setAmount(rs.getBigDecimal("amount"));
        d.setStripePaymentIntentId(rs.getString("stripe_payment_intent_id"));
        d.setStripePaymentStatus(rs.getString("stripe_payment_status"));
        try { d.setStripeCheckoutSessionId(rs.getString("stripe_checkout_session_id")); } catch (SQLException ignored) {}
        d.setContractPdfPath(rs.getString("contract_pdf_path"));
        d.setYousignSignatureRequestId(rs.getString("yousign_signature_request_id"));
        d.setYousignStatus(rs.getString("yousign_status"));
        d.setEmailSent(rs.getBoolean("email_sent"));
        d.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) d.setCreatedAt(created.toLocalDateTime());
        Timestamp completed = rs.getTimestamp("completed_at");
        if (completed != null) d.setCompletedAt(completed.toLocalDateTime());
        d.setProjectTitle(rs.getString("project_title"));
        d.setBuyerName(rs.getString("buyer_name"));
        d.setBuyerEmail(rs.getString("buyer_email"));
        d.setSellerName(rs.getString("seller_name"));
        d.setSellerEmail(rs.getString("seller_email"));
        return d;
    }
}

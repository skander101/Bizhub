package com.bizhub.Investistment.Services;

import com.bizhub.Investistment.Entitites.Payment;
import com.bizhub.Investistment.Utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentServiceImpl implements IPaymentService {

    @Override
    public int add(Payment payment) throws SQLException {
        String sql = "INSERT INTO payment (investment_id, amount, payment_method, payment_status, transaction_reference, notes) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, payment.getInvestmentId());
            pstmt.setBigDecimal(2, payment.getAmount());
            pstmt.setString(3, payment.getPaymentMethod());
            pstmt.setString(4, payment.getPaymentStatus());
            pstmt.setString(5, payment.getTransactionReference());
            pstmt.setString(6, payment.getNotes());

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public boolean update(Payment payment) throws SQLException {
        String sql = "UPDATE payment SET amount = ?, payment_method = ?, payment_status = ?, transaction_reference = ?, notes = ? WHERE payment_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, payment.getAmount());
            pstmt.setString(2, payment.getPaymentMethod());
            pstmt.setString(3, payment.getPaymentStatus());
            pstmt.setString(4, payment.getTransactionReference());
            pstmt.setString(5, payment.getNotes());
            pstmt.setInt(6, payment.getPaymentId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM payment WHERE payment_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<Payment> getAll() throws SQLException {
        List<Payment> payments = new ArrayList<>();

        String sql = "SELECT p.*, i.amount as investment_total, pr.title as project_title, u.username as investor_name " +
                "FROM payment p " +
                "LEFT JOIN investment i ON p.investment_id = i.investment_id " +
                "LEFT JOIN project pr ON i.project_id = pr.project_id " +
                "LEFT JOIN user u ON i.investor_id = u.user_id " +
                "ORDER BY p.payment_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Payment pay = new Payment();
                pay.setPaymentId(rs.getInt("payment_id"));
                pay.setInvestmentId(rs.getInt("investment_id"));
                pay.setAmount(rs.getBigDecimal("amount"));
                pay.setPaymentDate(rs.getTimestamp("payment_date").toLocalDateTime());
                pay.setPaymentMethod(rs.getString("payment_method"));
                pay.setPaymentStatus(rs.getString("payment_status"));
                pay.setTransactionReference(rs.getString("transaction_reference"));
                pay.setNotes(rs.getString("notes"));
                pay.setInvestmentTotal(rs.getBigDecimal("investment_total"));
                pay.setProjectTitle(rs.getString("project_title"));
                pay.setInvestorName(rs.getString("investor_name"));
                payments.add(pay);
            }
        }

        return payments;
    }

    @Override
    public Payment getById(int id) throws SQLException {
        String sql = "SELECT p.*, i.amount as investment_total " +
                "FROM payment p " +
                "LEFT JOIN investment i ON p.investment_id = i.investment_id " +
                "WHERE p.payment_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Payment pay = new Payment();
                pay.setPaymentId(rs.getInt("payment_id"));
                pay.setInvestmentId(rs.getInt("investment_id"));
                pay.setAmount(rs.getBigDecimal("amount"));
                pay.setPaymentDate(rs.getTimestamp("payment_date").toLocalDateTime());
                pay.setPaymentMethod(rs.getString("payment_method"));
                pay.setPaymentStatus(rs.getString("payment_status"));
                pay.setTransactionReference(rs.getString("transaction_reference"));
                pay.setNotes(rs.getString("notes"));
                pay.setInvestmentTotal(rs.getBigDecimal("investment_total"));
                return pay;
            }
        }

        return null;
    }

    @Override
    public List<Payment> getByInvestmentId(int investmentId) throws SQLException {
        List<Payment> payments = new ArrayList<>();

        String sql = "SELECT * FROM payment WHERE investment_id = ? ORDER BY payment_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, investmentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Payment pay = new Payment();
                pay.setPaymentId(rs.getInt("payment_id"));
                pay.setInvestmentId(rs.getInt("investment_id"));
                pay.setAmount(rs.getBigDecimal("amount"));
                pay.setPaymentDate(rs.getTimestamp("payment_date").toLocalDateTime());
                pay.setPaymentMethod(rs.getString("payment_method"));
                pay.setPaymentStatus(rs.getString("payment_status"));
                pay.setTransactionReference(rs.getString("transaction_reference"));
                pay.setNotes(rs.getString("notes"));
                payments.add(pay);
            }
        }

        return payments;
    }
}
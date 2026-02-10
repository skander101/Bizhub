package com.bizhub.Investistment.Services;

import com.bizhub.Investistment.Entitites.Payment;
import java.sql.SQLException;
import java.util.List;

public interface IPaymentService {
    int add(Payment payment) throws SQLException;
    boolean update(Payment payment) throws SQLException;
    boolean delete(int id) throws SQLException;
    List<Payment> getAll() throws SQLException;
    Payment getById(int id) throws SQLException;
    List<Payment> getByInvestmentId(int investmentId) throws SQLException; // Jointure One-to-Many
}
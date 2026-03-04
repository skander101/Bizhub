package com.bizhub.model.services.investissement;

import com.bizhub.model.investissement.Investment;
import java.sql.SQLException;
import java.util.List;

public interface IInvestmentService {
    int add(Investment investment) throws SQLException;
    boolean update(Investment investment) throws SQLException;
    boolean delete(int id) throws SQLException;
    List<Investment> getAll() throws SQLException;
    Investment getById(int id) throws SQLException;
}


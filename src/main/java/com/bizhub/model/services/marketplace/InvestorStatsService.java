package com.bizhub.model.services.marketplace;

import com.bizhub.model.marketplace.InvestorStatsRepository;
import com.bizhub.model.marketplace.StatsPoint;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class InvestorStatsService {

    private final InvestorStatsRepository repo = new InvestorStatsRepository();

    public List<StatsPoint> daily(int investorId, LocalDate from, LocalDate to) throws SQLException {
        return repo.getDailyStats(investorId, from, to);
    }
}
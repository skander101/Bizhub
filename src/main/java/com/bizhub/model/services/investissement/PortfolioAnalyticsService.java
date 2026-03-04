package com.bizhub.model.services.investissement;

import com.bizhub.model.investissement.Investment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortfolioAnalyticsService {

    public static class PortfolioHealth {
        private final BigDecimal totalAmount;
        private final int totalInvestments;
        private final BigDecimal topProjectSharePct;
        private final BigDecimal topInvestorSharePct;

        public PortfolioHealth(BigDecimal totalAmount, int totalInvestments, BigDecimal topProjectSharePct, BigDecimal topInvestorSharePct) {
            this.totalAmount = totalAmount;
            this.totalInvestments = totalInvestments;
            this.topProjectSharePct = topProjectSharePct;
            this.topInvestorSharePct = topInvestorSharePct;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public int getTotalInvestments() {
            return totalInvestments;
        }

        public BigDecimal getTopProjectSharePct() {
            return topProjectSharePct;
        }

        public BigDecimal getTopInvestorSharePct() {
            return topInvestorSharePct;
        }
    }

    public PortfolioHealth compute(List<Investment> investments) {
        if (investments == null || investments.isEmpty()) {
            return new PortfolioHealth(BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal total = investments.stream()
                .map(Investment::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return new PortfolioHealth(BigDecimal.ZERO, investments.size(), BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Map<Integer, BigDecimal> byProject = new HashMap<>();
        Map<Integer, BigDecimal> byInvestor = new HashMap<>();

        for (Investment inv : investments) {
            if (inv == null || inv.getAmount() == null) continue;
            if (inv.getProjectId() != null) {
                byProject.merge(inv.getProjectId(), inv.getAmount(), BigDecimal::add);
            }
            if (inv.getInvestorId() != null) {
                byInvestor.merge(inv.getInvestorId(), inv.getAmount(), BigDecimal::add);
            }
        }

        BigDecimal topProject = byProject.values().stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal topInvestor = byInvestor.values().stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        BigDecimal topProjectPct = topProject.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
        BigDecimal topInvestorPct = topInvestor.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);

        return new PortfolioHealth(total, investments.size(), topProjectPct, topInvestorPct);
    }
}

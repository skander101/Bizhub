package com.bizhub.model.services.common.service;

import com.bizhub.model.services.user_avis.review.ReviewService;
import com.bizhub.model.services.user_avis.user.UserService;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService {

    private final UserService userService;
    private final ReviewService reviewService;

    public ReportService(UserService userService, ReviewService reviewService) {
        this.userService = userService;
        this.reviewService = reviewService;
    }

    public DashboardStats getAdminStats() throws SQLException {
        var users = userService.findAll();
        int total = users.size();
        int active = (int) users.stream().filter(u -> u.isActive()).count();
        int inactive = total - active;

        Map<String, Integer> byType = new HashMap<>();
        for (var u : users) {
            String t = u.getUserType() == null ? "unknown" : u.getUserType();
            byType.put(t, byType.getOrDefault(t, 0) + 1);
        }

        double avgRatingAll = 0.0;
        int formationsWithAvg = 0;
        for (ReviewService.FormationAvg fa : reviewService.getAverageRatingByFormation()) {
            if (!Double.isNaN(fa.avgRating()) && fa.reviewCount() > 0) {
                avgRatingAll += fa.avgRating();
                formationsWithAvg++;
            }
        }
        avgRatingAll = formationsWithAvg == 0 ? 0.0 : avgRatingAll / formationsWithAvg;

        return new DashboardStats(total, active, inactive, byType, avgRatingAll);
    }

    public record DashboardStats(int totalUsers, int activeUsers, int inactiveUsers,
                                 Map<String, Integer> usersByType,
                                 double avgRatingAllFormations) {
    }
}


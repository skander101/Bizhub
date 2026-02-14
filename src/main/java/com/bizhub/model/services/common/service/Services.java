package com.bizhub.model.services.common.service;

import com.bizhub.model.services.common.dao.MyDatabase;
import com.bizhub.model.services.user_avis.formation.FormationService;
import com.bizhub.model.services.user_avis.review.ReviewService;
import com.bizhub.model.services.user_avis.user.UserService;

import java.sql.Connection;

/**
 * Minimal service locator for this mini-project.
 * Keeps things simple (no DI framework) while allowing controllers to share services.
 */
public final class Services {

    private static Connection cnx;
    private static UserService userService;
    private static ReviewService reviewService;
    private static FormationService formationService;
    private static AuthService authService;
    private static ValidationService validationService;
    private static ReportService reportService;

    private Services() {
    }

    public static synchronized Connection cnx() {
        if (cnx == null) {
            cnx = MyDatabase.getInstance().getCnx();
        }
        return cnx;
    }

    public static synchronized UserService users() {
        if (userService == null) {
            userService = new UserService();
        }
        return userService;
    }

    public static synchronized ReviewService reviews() {
        if (reviewService == null) {
            reviewService = new ReviewService();
        }
        return reviewService;
    }

    public static synchronized FormationService formations() {
        if (formationService == null) {
            formationService = new FormationService();
        }
        return formationService;
    }

    public static synchronized AuthService auth() {
        if (authService == null) {
            authService = new AuthService(users(), new AuthService.Session());
        }
        return authService;
    }

    public static synchronized ValidationService validation() {
        if (validationService == null) {
            validationService = new ValidationService();
        }
        return validationService;
    }

    public static synchronized ReportService reports() {
        if (reportService == null) {
            reportService = new ReportService(users(), reviews());
        }
        return reportService;
    }
}

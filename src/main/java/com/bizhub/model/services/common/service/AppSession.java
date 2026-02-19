package com.bizhub.model.services.common.service;

import com.bizhub.model.users_avis.user.User;

/** Simple global session holder for this mini-project. */
public final class AppSession {

    private static User currentUser;
    private static User pendingVerificationUser;

    private AppSession() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User currentUser) {
        AppSession.currentUser = currentUser;
    }

    public static void clear() {
        currentUser = null;
        pendingVerificationUser = null;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return isAuthenticated() && "admin".equalsIgnoreCase(currentUser.getUserType());
    }

    /**
     * Store a user that's pending verification (email/phone).
     * This user is not yet persisted to the database.
     */
    public static User getPendingVerificationUser() {
        return pendingVerificationUser;
    }

    public static void setPendingVerificationUser(User user) {
        pendingVerificationUser = user;
    }

    public static void clearPendingVerificationUser() {
        pendingVerificationUser = null;
    }
}

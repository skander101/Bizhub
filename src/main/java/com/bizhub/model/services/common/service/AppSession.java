package com.bizhub.model.services.common.service;

import com.bizhub.model.users_avis.user.User;

/** Simple global session holder for this mini-project. */
public final class AppSession {

    private static User currentUser;

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
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return isAuthenticated() && "admin".equalsIgnoreCase(currentUser.getUserType());
    }
}

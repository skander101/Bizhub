package com.bizhub.common.service;

import com.bizhub.user.dao.UserDAO;
import com.bizhub.user.model.User;
import com.bizhub.user.service.UserService;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Optional;

/**
 * AuthService: validates login credentials and keeps the current session user in-memory.
 *
 * In a JavaFX app, you typically keep one instance of this service (singleton via DI or manual wiring)
 * and clear it on logout.
 */
public class AuthService {

    public static final class Session {
        private User currentUser;

        public User requireUser() {
            if (currentUser == null) {
                throw new IllegalStateException("No user in session");
            }
            return currentUser;
        }

        public User getCurrentUser() {
            return currentUser;
        }

        public void setCurrentUser(User currentUser) {
            this.currentUser = currentUser;
        }

        public void clear() {
            this.currentUser = null;
        }

        public boolean isAuthenticated() {
            return currentUser != null;
        }

        public boolean isAdmin() {
            return isAuthenticated() && "admin".equalsIgnoreCase(currentUser.getUserType());
        }
    }

    private final UserService userService;
    private final Session session;

    public AuthService(UserService userService, Session session) {
        this.userService = userService;
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    /**
     * Returns the authenticated user if credentials are valid.
     * Fails if user is inactive.
     */
    public Optional<User> login(String email, String passwordPlaintext) throws SQLException {
        if (email == null || email.isBlank() || passwordPlaintext == null || passwordPlaintext.isBlank()) {
            return Optional.empty();
        }

        Optional<User> opt = userService.findByEmail(email.trim());
        if (opt.isEmpty()) return Optional.empty();

        User u = opt.get();
        if (!u.isActive()) return Optional.empty();

        boolean ok = BCrypt.checkpw(passwordPlaintext, u.getPasswordHash());
        if (!ok) return Optional.empty();

        session.setCurrentUser(u);
        return Optional.of(u);
    }

    public void logout() {
        session.clear();
    }

    /** Utility helper for creating password hashes (used in user creation & password change). */
    public String hashPassword(String passwordPlaintext) {
        return BCrypt.hashpw(passwordPlaintext, BCrypt.gensalt(12));
    }
}

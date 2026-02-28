package com.bizhub.model.services.common.dao;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyDatabase {

    private static final Logger LOGGER = Logger.getLogger(MyDatabase.class.getName());

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int SOCKET_TIMEOUT_MS  = 5000;

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Dotenv dotenv = Dotenv.configure()
                .directory("Bizhub-User-Java") // cas: run depuis nvPi/
                .ignoreIfMissing()
                .load();

        // Si pas trouvé, on tente le dossier courant (cas: run depuis Bizhub-User-Java/)
        if (dotenv.get("DB_URL") == null) {
            dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();
        }

        String rawUrl = dotenv.get("DB_URL",
                "jdbc:mysql://127.0.0.1:3306/bizhub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");

        URL = withMysqlTimeouts(rawUrl, CONNECT_TIMEOUT_MS, SOCKET_TIMEOUT_MS);
        USER = dotenv.get("DB_USER", "root");
        PASSWORD = dotenv.get("DB_PASSWORD", "");
    }

    private Connection cnx;
    private static volatile MyDatabase instance;

    private MyDatabase() {
        connectWithRetry(1);
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            synchronized (MyDatabase.class) {
                if (instance == null) instance = new MyDatabase();
            }
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }

    public boolean isAlive() {
        try {
            return cnx != null && !cnx.isClosed() && cnx.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    // =========================
    // Internal
    // =========================

    private void connectWithRetry(int retries) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            LOGGER.info("⏳ Connexion DB...");
            LOGGER.info("DB_URL=" + URL);
            LOGGER.info("DB_USER=" + USER);

            cnx = DriverManager.getConnection(URL, USER, PASSWORD);

            LOGGER.info("✅ Connected to database: " + cnx.getMetaData().getURL());

        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "❌ MySQL Driver missing (mysql-connector-j not found): " + e.getMessage(), e);
            cnx = null;

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "❌ DB connection error: " + e.getMessage(), e);
            cnx = null;

            if (retries > 0) {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException ignored) {}
                connectWithRetry(retries - 1);
            }
        }
    }

    private static String withMysqlTimeouts(String url, int connectMs, int socketMs) {
        if (url == null) return null;

        // si pas mysql, renvoyer tel quel
        if (!url.startsWith("jdbc:mysql:")) return url;

        String u = url;

        // s'assure qu'on a un '?'
        if (!u.contains("?")) u = u + "?";

        // ajoute & si nécessaire
        if (!u.endsWith("?") && !u.endsWith("&")) u = u + "&";

        // n'ajoute pas si déjà présent
        if (!u.contains("connectTimeout=")) {
            u += "connectTimeout=" + connectMs + "&";
        }
        if (!u.contains("socketTimeout=")) {
            u += "socketTimeout=" + socketMs + "&";
        }

        // Nettoyage si termine par &
        if (u.endsWith("&")) u = u.substring(0, u.length() - 1);

        return u;
    }
}
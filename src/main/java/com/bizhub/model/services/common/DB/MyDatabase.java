package com.bizhub.model.services.common.DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class MyDatabase {

    private static final Logger LOGGER = Logger.getLogger(MyDatabase.class.getName());

    private static final String URL      = "jdbc:mysql://localhost:3306/BizHub?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private Connection cnx;
    private static MyDatabase instance;

    private MyDatabase() {
        connect();
    }

    public static MyDatabase getInstance() {
        if (instance == null)
            instance = new MyDatabase();
        return instance;
    }

    /**
     * Returns a live, shared connection that is safe to use in try-with-resources.
     * The returned wrapper ignores close() calls so the underlying connection stays alive
     * for all other code that holds a reference to it.
     *
     * Reconnects automatically if the connection was dropped by the server.
     */
    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed() || !cnx.isValid(2)) {
                LOGGER.info("DB connection lost — reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            LOGGER.warning("Could not check connection validity, attempting reconnect: " + e.getMessage());
            connect();
        }
        return cnx;
    }

    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Wrap the real connection so that close() is a no-op.
            // This prevents try-with-resources blocks from killing the shared connection.
            Connection real = DriverManager.getConnection(URL, USER, PASSWORD);
            cnx = new NonClosingConnectionWrapper(real);
            LOGGER.info("✅ Connected to database");
        } catch (ClassNotFoundException e) {
            LOGGER.severe("❌ MySQL driver not found: " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.severe("❌ Failed to connect to database: " + e.getMessage());
        }
    }
}

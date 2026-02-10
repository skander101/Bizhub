package com.bizhub.Investistment.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Configuration de la base de données
    private static final String URL = "jdbc:mysql://localhost:3306/bizhub";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Instance unique (Singleton)
    private static Connection connection;

    /**
     * Obtenir la connexion à la base de données
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Charger le driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Créer la connexion
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion à la base de données réussie!");
            }

            return connection;

        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver MySQL introuvable!");
            throw new SQLException("Driver MySQL introuvable", e);
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion à la base de données: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Fermer la connexion
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔒 Connexion fermée");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la fermeture: " + e.getMessage());
        }
    }

    /**
     * Tester la connexion
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Test de connexion échoué: " + e.getMessage());
            return false;
        }
    }
}
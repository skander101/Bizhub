package com.bizhub.model.services.common.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    // ✅ IMPORTANT : mets le nom de DB en minuscule si possible
    // et ajoute les paramètres recommandés
    private static final String URL =
            "jdbc:mysql://localhost:3306/bizhub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Connection cnx;
    private static MyDatabase instance;

    private MyDatabase() {
        try {
            // ✅ Driver (safe)
            Class.forName("com.mysql.cj.jdbc.Driver");

            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to database: " + cnx.getMetaData().getURL());

        } catch (ClassNotFoundException e) {
            System.out.println("❌ MySQL Driver missing (mysql-connector-j not found): " + e.getMessage());
            cnx = null;
        } catch (SQLException e) {
            System.out.println("❌ DB connection error: " + e.getMessage());
            cnx = null;
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null) instance = new MyDatabase();
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}

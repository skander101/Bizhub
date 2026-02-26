package com.bizhub.model.services.common.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserRepository {

    private final Connection cnx = MyDatabase.getInstance().getCnx();

    public String findPhoneByUserId(int userId) {
        String sql = "SELECT phone FROM users WHERE user_id = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("phone");
            }
        } catch (Exception ignore) {}
        return null;
    }
}
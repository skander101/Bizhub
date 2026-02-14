package com.bizhub.model.services.user_avis.user;

import com.bizhub.model.services.common.dao.MyDatabase;
import com.bizhub.model.users_avis.user.User;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UserService: Merged service combining UserDAO + UserService functionality.
 * CRUD + lookup methods + business logic.
 *
 * Notes:
 * - Table name is `user` (quoted because it can be reserved in MySQL/MariaDB).
 * - This service manages a live JDBC Connection from MyDatabase singleton.
 */
public class UserService {

    private final Connection cnx;

    public UserService() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    // ==================== DAO Methods (CRUD) ====================

    public void create(User u) throws SQLException {
        // Insert all fields used by signup. Irrelevant role fields should be NULL.
        // NOTE: admin_role / role_start_date were removed from DB, so they must not appear in this statement.
        String sql = "INSERT INTO `user`(" +
                "email, password_hash, user_type, full_name, phone, address, bio, avatar_url, " +
                "company_name, sector, company_description, website, founding_date, " +
                "business_type, delivery_zones, payment_methods, return_policy, " +
                "investment_sector, max_budget, years_experience, represented_company, " +
                "specialty, hourly_rate, availability, cv_url, " +
                "is_active" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            setStringOrNull(pst, i++, u.getEmail());
            // password_hash is NOT NULL in DB; keep strict
            pst.setString(i++, u.getPasswordHash());
            pst.setString(i++, u.getUserType());
            pst.setString(i++, u.getFullName());

            setStringOrNull(pst, i++, u.getPhone());
            setStringOrNull(pst, i++, u.getAddress());
            setStringOrNull(pst, i++, u.getBio());
            setStringOrNull(pst, i++, u.getAvatarUrl());

            setStringOrNull(pst, i++, u.getCompanyName());
            setStringOrNull(pst, i++, u.getSector());
            setStringOrNull(pst, i++, u.getCompanyDescription());
            setStringOrNull(pst, i++, u.getWebsite());
            setLocalDateOrNull(pst, i++, u.getFoundingDate());

            setStringOrNull(pst, i++, u.getBusinessType());
            setStringOrNull(pst, i++, u.getDeliveryZones());
            setStringOrNull(pst, i++, u.getPaymentMethods());
            setStringOrNull(pst, i++, u.getReturnPolicy());

            setStringOrNull(pst, i++, u.getInvestmentSector());
            setBigDecimalOrNull(pst, i++, u.getMaxBudget());
            setIntegerOrNull(pst, i++, u.getYearsExperience());
            setStringOrNull(pst, i++, u.getRepresentedCompany());

            setStringOrNull(pst, i++, u.getSpecialty());
            setBigDecimalOrNull(pst, i++, u.getHourlyRate());
            setStringOrNull(pst, i++, u.getAvailability());
            setStringOrNull(pst, i++, u.getCvUrl());

            pst.setBoolean(i++, u.isActive());

            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) u.setUserId(rs.getInt(1));
            }
        }
    }

    public Optional<User> findById(int userId) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE user_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE email=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    public List<User> findLatest(int limit) throws SQLException {
        String sql = "SELECT * FROM `user` ORDER BY created_at DESC LIMIT ?";
        List<User> users = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, limit);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }
        }
        return users;
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM `user`";
        List<User> users = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        }
        return users;
    }

    public void update(User u) throws SQLException {
        // Update profile/admin editable fields (email/password handled elsewhere)
        // NOTE: admin_role / role_start_date were removed from DB.
        String sql = "UPDATE `user` SET " +
                "user_type=?, full_name=?, phone=?, address=?, bio=?, avatar_url=?, " +
                "company_name=?, sector=?, company_description=?, website=?, founding_date=?, " +
                "business_type=?, delivery_zones=?, payment_methods=?, return_policy=?, " +
                "investment_sector=?, max_budget=?, years_experience=?, represented_company=?, " +
                "specialty=?, hourly_rate=?, availability=?, cv_url=?, " +
                "is_active=? " +
                "WHERE user_id=?";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            int i = 1;
            pst.setString(i++, u.getUserType());
            pst.setString(i++, u.getFullName());

            setStringOrNull(pst, i++, u.getPhone());
            setStringOrNull(pst, i++, u.getAddress());
            setStringOrNull(pst, i++, u.getBio());
            setStringOrNull(pst, i++, u.getAvatarUrl());

            setStringOrNull(pst, i++, u.getCompanyName());
            setStringOrNull(pst, i++, u.getSector());
            setStringOrNull(pst, i++, u.getCompanyDescription());
            setStringOrNull(pst, i++, u.getWebsite());
            setLocalDateOrNull(pst, i++, u.getFoundingDate());

            setStringOrNull(pst, i++, u.getBusinessType());
            setStringOrNull(pst, i++, u.getDeliveryZones());
            setStringOrNull(pst, i++, u.getPaymentMethods());
            setStringOrNull(pst, i++, u.getReturnPolicy());

            setStringOrNull(pst, i++, u.getInvestmentSector());
            setBigDecimalOrNull(pst, i++, u.getMaxBudget());
            setIntegerOrNull(pst, i++, u.getYearsExperience());
            setStringOrNull(pst, i++, u.getRepresentedCompany());

            setStringOrNull(pst, i++, u.getSpecialty());
            setBigDecimalOrNull(pst, i++, u.getHourlyRate());
            setStringOrNull(pst, i++, u.getAvailability());
            setStringOrNull(pst, i++, u.getCvUrl());

            pst.setBoolean(i++, u.isActive());
            pst.setInt(i++, u.getUserId());

            pst.executeUpdate();
        }
    }

    public void updateAvatarUrl(int userId, String avatarUrl) throws SQLException {
        String sql = "UPDATE `user` SET avatar_url=? WHERE user_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, avatarUrl);
            pst.setInt(2, userId);
            pst.executeUpdate();
        }
    }

    public void updatePasswordHash(int userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE `user` SET password_hash=? WHERE user_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, newPasswordHash);
            pst.setInt(2, userId);
            pst.executeUpdate();
        }
    }

    public void setActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE `user` SET is_active=? WHERE user_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setBoolean(1, active);
            pst.setInt(2, userId);
            pst.executeUpdate();
        }
    }

    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM `user` WHERE user_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.executeUpdate();
        }
    }

    // ==================== Business Logic Methods ====================

    public String updateProfilePicture(int userId, File imageFile) throws SQLException, IOException {
        // Define the target directory
        Path targetDirectory = Paths.get("src/main/resources/com/bizhub/images/avatars");
        if (!Files.exists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        // Generate a unique file name
        String extension = getFileExtension(imageFile.getName());
        String uniqueFileName = UUID.randomUUID().toString() + (extension.isBlank() ? "" : ("." + extension));
        Path targetPath = targetDirectory.resolve(uniqueFileName);

        // Copy the file
        Files.copy(imageFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Path stored in DB is relative to resources root, so it can be used for classpath/file fallbacks.
        String relativePath = "com/bizhub/images/avatars/" + uniqueFileName;
        updateAvatarUrl(userId, relativePath);

        return relativePath;
    }

    // ==================== Helper Methods ====================

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setUserType(rs.getString("user_type"));
        u.setFullName(rs.getString("full_name"));
        u.setPhone(rs.getString("phone"));
        u.setAddress(rs.getString("address"));
        u.setBio(rs.getString("bio"));
        u.setAvatarUrl(rs.getString("avatar_url"));

        u.setCompanyName(rs.getString("company_name"));
        u.setSector(rs.getString("sector"));
        u.setCompanyDescription(rs.getString("company_description"));
        u.setWebsite(rs.getString("website"));

        Date foundingDate = rs.getDate("founding_date");
        u.setFoundingDate(foundingDate == null ? null : foundingDate.toLocalDate());

        u.setBusinessType(rs.getString("business_type"));
        u.setDeliveryZones(rs.getString("delivery_zones"));
        u.setPaymentMethods(rs.getString("payment_methods"));
        u.setReturnPolicy(rs.getString("return_policy"));

        u.setInvestmentSector(rs.getString("investment_sector"));
        u.setMaxBudget(rs.getBigDecimal("max_budget"));
        int yearsExp = rs.getInt("years_experience");
        u.setYearsExperience(rs.wasNull() ? null : yearsExp);
        u.setRepresentedCompany(rs.getString("represented_company"));

        u.setSpecialty(rs.getString("specialty"));
        u.setHourlyRate(rs.getBigDecimal("hourly_rate"));
        u.setAvailability(rs.getString("availability"));
        u.setCvUrl(rs.getString("cv_url"));

        // admin_role / role_start_date removed from DB

        u.setActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        u.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());

        return u;
    }

    private static void setLocalDateOrNull(PreparedStatement pst, int idx, LocalDate value) throws SQLException {
        if (value == null) pst.setNull(idx, Types.DATE);
        else pst.setDate(idx, Date.valueOf(value));
    }

    private static void setIntegerOrNull(PreparedStatement pst, int idx, Integer value) throws SQLException {
        if (value == null) pst.setNull(idx, Types.INTEGER);
        else pst.setInt(idx, value);
    }

    private static void setBigDecimalOrNull(PreparedStatement pst, int idx, BigDecimal value) throws SQLException {
        if (value == null) pst.setNull(idx, Types.DECIMAL);
        else pst.setBigDecimal(idx, value);
    }

    private static void setStringOrNull(PreparedStatement pst, int idx, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) pst.setNull(idx, Types.VARCHAR);
        else pst.setString(idx, value.trim());
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (lastIndexOf == -1 || lastIndexOf == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastIndexOf + 1);
    }
}


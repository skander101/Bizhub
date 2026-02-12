package com.bizhub.legacy.service;

import com.bizhub.legacy.model.User;
import com.bizhub.common.dao.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser implements IService<User> {

    private final Connection cnx;

    public ServiceUser() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void add(User u) throws SQLException {
        String sql = "INSERT INTO `user`(email, password_hash, user_type, full_name, phone, address, bio, avatar_url, company_name, sector, company_description, website, founding_date, business_type, delivery_zones, payment_methods, return_policy, investment_sector, max_budget, years_experience, represented_company, specialty, hourly_rate, availability, cv_url, admin_role, role_start_date) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            pst.setString(i++, u.getEmail());
            pst.setString(i++, u.getPasswordHash());
            pst.setString(i++, u.getUserType());
            pst.setString(i++, u.getFullName());
            pst.setString(i++, u.getPhone());
            pst.setString(i++, u.getAddress());
            pst.setString(i++, u.getBio());
            pst.setString(i++, u.getAvatarUrl());
            pst.setString(i++, u.getCompanyName());
            pst.setString(i++, u.getSector());
            pst.setString(i++, u.getCompanyDescription());
            pst.setString(i++, u.getWebsite());
            setLocalDateOrNull(pst, i++, u.getFoundingDate());
            pst.setString(i++, u.getBusinessType());
            pst.setString(i++, u.getDeliveryZones());
            pst.setString(i++, u.getPaymentMethods());
            pst.setString(i++, u.getReturnPolicy());
            pst.setString(i++, u.getInvestmentSector());
            setBigDecimalOrNull(pst, i++, u.getMaxBudget());
            setIntegerOrNull(pst, i++, u.getYearsExperience());
            pst.setString(i++, u.getRepresentedCompany());
            pst.setString(i++, u.getSpecialty());
            setBigDecimalOrNull(pst, i++, u.getHourlyRate());
            pst.setString(i++, u.getAvailability());
            pst.setString(i++, u.getCvUrl());
            pst.setString(i++, u.getAdminRole());
            setLocalDateOrNull(pst, i, u.getRoleStartDate());

            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    u.setUserId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(User u) throws SQLException {
        String sql = "UPDATE `user` SET email=?, password_hash=?, user_type=?, full_name=?, phone=?, address=?, bio=?, avatar_url=?, company_name=?, sector=?, company_description=?, website=?, founding_date=?, business_type=?, delivery_zones=?, payment_methods=?, return_policy=?, investment_sector=?, max_budget=?, years_experience=?, represented_company=?, specialty=?, hourly_rate=?, availability=?, cv_url=?, admin_role=?, role_start_date=? WHERE user_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            int i = 1;
            pst.setString(i++, u.getEmail());
            pst.setString(i++, u.getPasswordHash());
            pst.setString(i++, u.getUserType());
            pst.setString(i++, u.getFullName());
            pst.setString(i++, u.getPhone());
            pst.setString(i++, u.getAddress());
            pst.setString(i++, u.getBio());
            pst.setString(i++, u.getAvatarUrl());
            pst.setString(i++, u.getCompanyName());
            pst.setString(i++, u.getSector());
            pst.setString(i++, u.getCompanyDescription());
            pst.setString(i++, u.getWebsite());
            setLocalDateOrNull(pst, i++, u.getFoundingDate());
            pst.setString(i++, u.getBusinessType());
            pst.setString(i++, u.getDeliveryZones());
            pst.setString(i++, u.getPaymentMethods());
            pst.setString(i++, u.getReturnPolicy());
            pst.setString(i++, u.getInvestmentSector());
            setBigDecimalOrNull(pst, i++, u.getMaxBudget());
            setIntegerOrNull(pst, i++, u.getYearsExperience());
            pst.setString(i++, u.getRepresentedCompany());
            pst.setString(i++, u.getSpecialty());
            setBigDecimalOrNull(pst, i++, u.getHourlyRate());
            pst.setString(i++, u.getAvailability());
            pst.setString(i++, u.getCvUrl());
            pst.setString(i++, u.getAdminRole());
            setLocalDateOrNull(pst, i++, u.getRoleStartDate());
            pst.setInt(i, u.getUserId());

            pst.executeUpdate();
        }
    }

    @Override
    public void delete(User u) throws SQLException {
        String sql = "DELETE FROM `user` WHERE user_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, u.getUserId());
            pst.executeUpdate();
        }
    }

    @Override
    public List<User> getAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM `user`";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    public User getById(int id) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE user_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    public User getByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE email=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    private static User mapRow(ResultSet rs) throws SQLException {
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
        u.setFoundingDate(getLocalDateOrNull(rs, "founding_date"));
        u.setBusinessType(rs.getString("business_type"));
        u.setDeliveryZones(rs.getString("delivery_zones"));
        u.setPaymentMethods(rs.getString("payment_methods"));
        u.setReturnPolicy(rs.getString("return_policy"));
        u.setInvestmentSector(rs.getString("investment_sector"));
        u.setMaxBudget(rs.getBigDecimal("max_budget"));
        int years = rs.getInt("years_experience");
        u.setYearsExperience(rs.wasNull() ? null : years);
        u.setRepresentedCompany(rs.getString("represented_company"));
        u.setSpecialty(rs.getString("specialty"));
        u.setHourlyRate(rs.getBigDecimal("hourly_rate"));
        u.setAvailability(rs.getString("availability"));
        u.setCvUrl(rs.getString("cv_url"));
        u.setAdminRole(rs.getString("admin_role"));
        u.setRoleStartDate(getLocalDateOrNull(rs, "role_start_date"));
        return u;
    }

    private static LocalDate getLocalDateOrNull(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    private static void setLocalDateOrNull(PreparedStatement pst, int index, LocalDate d) throws SQLException {
        if (d == null) pst.setNull(index, Types.DATE);
        else pst.setDate(index, Date.valueOf(d));
    }

    private static void setBigDecimalOrNull(PreparedStatement pst, int index, BigDecimal v) throws SQLException {
        if (v == null) pst.setNull(index, Types.DECIMAL);
        else pst.setBigDecimal(index, v);
    }

    private static void setIntegerOrNull(PreparedStatement pst, int index, Integer v) throws SQLException {
        if (v == null) pst.setNull(index, Types.INTEGER);
        else pst.setInt(index, v);
    }
}


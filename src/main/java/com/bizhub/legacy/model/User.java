package com.bizhub.legacy.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class User {
    private int userId;
    private String email;
    private String passwordHash;
    private String userType;
    private String fullName;

    private String phone;
    private String address;
    private String bio;
    private String avatarUrl;

    private String companyName;
    private String sector;
    private String companyDescription;
    private String website;
    private LocalDate foundingDate;

    private String businessType;
    private String deliveryZones;
    private String paymentMethods;
    private String returnPolicy;

    private String investmentSector;
    private BigDecimal maxBudget;

    private Integer yearsExperience;
    private String representedCompany;
    private String specialty;
    private BigDecimal hourlyRate;
    private String availability;
    private String cvUrl;

    private String adminRole;
    private LocalDate roleStartDate;

    public User() {
    }

    // Minimal constructor for required columns
    public User(String email, String passwordHash, String userType, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.userType = userType;
        this.fullName = fullName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public LocalDate getFoundingDate() {
        return foundingDate;
    }

    public void setFoundingDate(LocalDate foundingDate) {
        this.foundingDate = foundingDate;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getDeliveryZones() {
        return deliveryZones;
    }

    public void setDeliveryZones(String deliveryZones) {
        this.deliveryZones = deliveryZones;
    }

    public String getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(String paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public String getReturnPolicy() {
        return returnPolicy;
    }

    public void setReturnPolicy(String returnPolicy) {
        this.returnPolicy = returnPolicy;
    }

    public String getInvestmentSector() {
        return investmentSector;
    }

    public void setInvestmentSector(String investmentSector) {
        this.investmentSector = investmentSector;
    }

    public BigDecimal getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(BigDecimal maxBudget) {
        this.maxBudget = maxBudget;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public String getRepresentedCompany() {
        return representedCompany;
    }

    public void setRepresentedCompany(String representedCompany) {
        this.representedCompany = representedCompany;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getCvUrl() {
        return cvUrl;
    }

    public void setCvUrl(String cvUrl) {
        this.cvUrl = cvUrl;
    }

    public String getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(String adminRole) {
        this.adminRole = adminRole;
    }

    public LocalDate getRoleStartDate() {
        return roleStartDate;
    }

    public void setRoleStartDate(LocalDate roleStartDate) {
        this.roleStartDate = roleStartDate;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", userType='" + userType + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", companyName='" + companyName + '\'' +
                "}";
    }
}


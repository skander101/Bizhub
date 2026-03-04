package com.bizhub.model.investissement;

public class InvestissementUser {
    private int userId;
    private String userType;

    public InvestissementUser(int userId, String userType) {
        this.userId = userId;
        this.userType = userType;
    }

    public int getUserId() { return userId; }
    public String getUserType() { return userType; }
}


package com.bizhub.Investistment.Entitites;

public class User {
    private int userId;
    private String userType;

    public User(int userId, String userType) {
        this.userId = userId;
        this.userType = userType;
    }

    public int getUserId() { return userId; }
    public String getUserType() { return userType; }
}

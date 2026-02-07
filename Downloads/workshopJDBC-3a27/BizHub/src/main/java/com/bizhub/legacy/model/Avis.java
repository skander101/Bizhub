package com.bizhub.legacy.model;

import java.time.LocalDateTime;

public class Avis {
    private int avisId;
    private int reviewerId;
    private int formationId;
    private Integer rating; // nullable (1..5)
    private String comment;
    private LocalDateTime createdAt;
    private Boolean isVerified;

    public Avis() {
    }

    public Avis(int reviewerId, int formationId, Integer rating) {
        this.reviewerId = reviewerId;
        this.formationId = formationId;
        this.rating = rating;
    }

    public int getAvisId() {
        return avisId;
    }

    public void setAvisId(int avisId) {
        this.avisId = avisId;
    }

    public int getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(int reviewerId) {
        this.reviewerId = reviewerId;
    }

    public int getFormationId() {
        return formationId;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getVerified() {
        return isVerified;
    }

    public void setVerified(Boolean verified) {
        isVerified = verified;
    }

    @Override
    public String toString() {
        return "Avis{" +
                "avisId=" + avisId +
                ", reviewerId=" + reviewerId +
                ", formationId=" + formationId +
                ", rating=" + rating +
                ", isVerified=" + isVerified +
                "}";
    }
}


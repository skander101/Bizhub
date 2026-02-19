package com.bizhub.model.users_avis.review;

import java.time.LocalDateTime;

/**
 * Review model for displaying avis with joins (reviewer + formation).
 */
public class Review {
    private int avisId;
    private int reviewerId;
    private int formationId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    // join fields
    private String reviewerName;
    private String reviewerEmail;
    private String reviewerAvatarUrl;
    private String formationTitle;

    public Review() {
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

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewerEmail() {
        return reviewerEmail;
    }

    public void setReviewerEmail(String reviewerEmail) {
        this.reviewerEmail = reviewerEmail;
    }

    public String getReviewerAvatarUrl() {
        return reviewerAvatarUrl;
    }

    public void setReviewerAvatarUrl(String reviewerAvatarUrl) {
        this.reviewerAvatarUrl = reviewerAvatarUrl;
    }

    public String getFormationTitle() {
        return formationTitle;
    }

    public void setFormationTitle(String formationTitle) {
        this.formationTitle = formationTitle;
    }

    @Override
    public String toString() {
        return "Review{" +
                "avisId=" + avisId +
                ", reviewerId=" + reviewerId +
                ", formationId=" + formationId +
                ", rating=" + rating +
                ", createdAt=" + createdAt +
                "}";
    }
}


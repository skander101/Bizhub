package com.bizhub.model.users_avis.formation;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Formation {
    private int formationId;
    private String title;
    private String description;
    private int trainerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal cost;

    public Formation() {
    }

    public Formation(int formationId, String title, int trainerId) {
        this.formationId = formationId;
        this.title = title;
        this.trainerId = trainerId;
    }

    public int getFormationId() {
        return formationId;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(int trainerId) {
        this.trainerId = trainerId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    @Override
    public String toString() {
        return "Formation{" +
                "formationId=" + formationId +
                ", title='" + title + '\'' +
                ", trainerId=" + trainerId +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", cost=" + cost +
                "}";
    }
}


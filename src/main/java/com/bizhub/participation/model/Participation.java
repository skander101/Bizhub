package com.bizhub.participation.model;

import java.time.LocalDateTime;

public class Participation {
    private int id;
    private int formationId;
    private int userId;
    private LocalDateTime dateAffectation;
    private String remarques;

    public Participation() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFormationId() {
        return formationId;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDateTime getDateAffectation() {
        return dateAffectation;
    }

    public void setDateAffectation(LocalDateTime dateAffectation) {
        this.dateAffectation = dateAffectation;
    }

    public String getRemarques() {
        return remarques;
    }

    public void setRemarques(String remarques) {
        this.remarques = remarques;
    }
}

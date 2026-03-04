package com.bizhub.model.investissement;

/**
 * Represents a project item for investment-related ComboBox selections.
 */
public class ProjectItem {
    private final Integer id;
    private final String title;

    public ProjectItem(Integer id, String title) {
        this.id = id;
        this.title = title;
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }

    @Override
    public String toString() { return title; }
}


package com.bizhub.model.services.user_avis.formation;

/** Simple context to pass selected formation around without adding a DI framework. */
public final class FormationContext {
    private static Integer selectedFormationId;

    private FormationContext() {
    }

    public static Integer getSelectedFormationId() {
        return selectedFormationId;
    }

    public static void setSelectedFormationId(Integer selectedFormationId) {
        FormationContext.selectedFormationId = selectedFormationId;
    }

    public static void clear() {
        selectedFormationId = null;
    }
}


package com.bizhub.participation.service;

/** Context to pass selected formation for participation (e.g. when opening "Participer" from a formation card). */
public final class  ParticipationContext {
    private static Integer formationIdForParticipation;

    private ParticipationContext() {
    }

    public static Integer getFormationIdForParticipation() {
        return formationIdForParticipation;
    }

    public static void setFormationIdForParticipation(Integer formationId) {
        ParticipationContext.formationIdForParticipation = formationId;
    }

    public static void clear() {
        formationIdForParticipation = null;
    }
}

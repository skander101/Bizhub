package com.bizhub.model.services.common.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ValidationService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public List<String> validateUserCreate(String email, String passwordPlain, String fullName, String userType) {
        List<String> errors = new ArrayList<>();
        if (email == null || email.isBlank() || !EMAIL.matcher(email.trim()).matches()) {
            errors.add("Email invalide");
        }
        if (passwordPlain == null || passwordPlain.isBlank() || passwordPlain.length() < 6) {
            errors.add("Mot de passe: minimum 6 caractères");
        }
        if (fullName == null || fullName.isBlank()) {
            errors.add("Nom complet obligatoire");
        }
        if (userType == null || userType.isBlank()) {
            errors.add("Type utilisateur obligatoire");
        }
        return errors;
    }

    /**
     * Minimal role-specific validation for Signup.
     *
     * We keep most role fields optional, but enforce a couple of key fields so the UX matches the role.
     */
    public List<String> validateSignupRoleFields(
            String role,
            String startupCompanyName,
            String trainerSpecialty,
            String investorMaxBudget,
            String investorYearsExperience,
            String trainerHourlyRate
    ) {
        List<String> errors = new ArrayList<>();
        if (role == null) return errors;

        switch (role) {
            case "startup" -> {
                if (startupCompanyName == null || startupCompanyName.isBlank()) {
                    errors.add("Startup: Nom de l'entreprise obligatoire");
                }
            }
            case "formateur" -> {
                if (trainerSpecialty == null || trainerSpecialty.isBlank()) {
                    errors.add("Formateur: Spécialité obligatoire");
                }
                if (!isBlank(trainerHourlyRate) && !isDecimal(trainerHourlyRate)) {
                    errors.add("Formateur: Taux horaire invalide");
                }
            }
            case "investisseur" -> {
                if (!isBlank(investorMaxBudget) && !isDecimal(investorMaxBudget)) {
                    errors.add("Investisseur: Budget max invalide");
                }
                if (!isBlank(investorYearsExperience) && !isInteger(investorYearsExperience)) {
                    errors.add("Investisseur: Années d'expérience invalides");
                }
            }
            case "fournisseur" -> {
                // All supplier fields optional for now.
            }
            default -> {
            }
        }

        return errors;
    }

    public List<String> validateUserUpdate(String fullName, String userType) {
        List<String> errors = new ArrayList<>();
        if (fullName == null || fullName.isBlank()) {
            errors.add("Nom complet obligatoire");
        }
        if (userType == null || userType.isBlank()) {
            errors.add("Type utilisateur obligatoire");
        }
        return errors;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isDecimal(String s) {
        try {
            new BigDecimal(s.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}


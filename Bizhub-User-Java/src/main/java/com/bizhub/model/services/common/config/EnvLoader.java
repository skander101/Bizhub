package com.bizhub.model.services.common.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * EnvLoader — Charge les variables depuis un fichier .env
 *
 * Priorité : System.getenv() > .env (les vraies variables d'environnement
 * ont toujours priorité sur le fichier .env)
 *
 * Recherche .env dans :
 *   1. Dossier courant (racine du projet IntelliJ)
 *   2. Dossier parent
 *
 * Usage :
 *   EnvLoader.get("STRIPE_SECRET_KEY")
 *   EnvLoader.getOrDefault("STRIPE_CURRENCY", "eur")
 */
public class EnvLoader {

    private static final Logger LOGGER = Logger.getLogger(EnvLoader.class.getName());
    private static final Map<String, String> ENV = new HashMap<>();
    private static boolean loaded = false;

    static {
        load();
    }

    private static void load() {
        if (loaded) return;

        // Cherche .env dans le dossier courant ou parent
        File envFile = findEnvFile();

        if (envFile == null || !envFile.exists()) {
            LOGGER.warning("⚠ Fichier .env introuvable — " +
                    "variables d'environnement système utilisées uniquement.");
            loaded = true;
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Ignorer commentaires et lignes vides
                if (line.isEmpty() || line.startsWith("#")) continue;

                int eq = line.indexOf('=');
                if (eq <= 0) continue;

                String key   = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

                // Enlever guillemets optionnels : "value" ou 'value'
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'")  && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }

                ENV.put(key, value);
                count++;
            }
            LOGGER.info("✅ .env chargé depuis " + envFile.getAbsolutePath()
                    + " (" + count + " variables)");

        } catch (IOException e) {
            LOGGER.warning("⚠ Erreur lecture .env : " + e.getMessage());
        }

        loaded = true;
    }

    /**
     * Retourne la valeur d'une variable.
     * Priorité : System.getenv() > .env
     * @return null si non trouvée
     */
    public static String get(String key) {
        // 1) Variable d'environnement système (priorité max)
        String sysVal = System.getenv(key);
        if (sysVal != null && !sysVal.isBlank()) return sysVal;

        // 2) Fichier .env
        return ENV.get(key);
    }

    /**
     * Retourne la valeur ou la valeur par défaut.
     */
    public static String getOrDefault(String key, String defaultValue) {
        String val = get(key);
        return (val == null || val.isBlank()) ? defaultValue : val;
    }

    /**
     * Retourne la valeur ou lève une exception si absente.
     */
    public static String getRequired(String key) {
        String val = get(key);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException(
                    "Variable '" + key + "' manquante.\n"
                            + "Ajoutez-la dans votre fichier .env à la racine du projet.");
        }
        return val;
    }

    private static File findEnvFile() {
        // 1) Dossier courant (racine projet IntelliJ)
        File f = new File(".env");
        if (f.exists()) return f;

        // 2) Dossier parent
        f = new File("../.env");
        if (f.exists()) return f;

        // 3) Home utilisateur (fallback)
        f = new File(System.getProperty("user.home") + "/.env");
        if (f.exists()) return f;

        return null;
    }
}
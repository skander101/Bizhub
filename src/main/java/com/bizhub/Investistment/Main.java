package com.bizhub.Investistment;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("🔍 Recherche du fichier FXML...");

            // LE BON CHEMIN : Utiliser le chemin complet depuis resources
            String fxmlPath = "/com/bizhub/fxml/AddInvestistmentView.fxml";
            URL fxmlUrl = getClass().getResource(fxmlPath);

            // Debug : afficher le chemin recherché
            System.out.println("📂 Chemin recherché : " + fxmlPath);
            System.out.println("📍 URL trouvée : " + fxmlUrl);

            // Vérifier si le fichier est trouvé
            if (fxmlUrl == null) {
                System.err.println("❌ ERREUR: Fichier FXML introuvable!");
                System.err.println("📂 Vérifiez que le fichier existe à:");
                System.err.println("   src/main/resources/com/example/gestion_investissement/Views/AddInvestmentView.fxml");

                // Liste tous les fichiers disponibles pour debug
                System.err.println("\n🔍 Tentative de listage des ressources disponibles...");
                return;
            }

            System.out.println("✅ Fichier FXML trouvé!");

            // Charger le FXML
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // Créer la scène
            Scene scene = new Scene(root);

            // Configurer le stage
            primaryStage.setTitle("Gestion des Investissements - Nouvel Investissement");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(850);
            primaryStage.setMinHeight(650);

            // Afficher la fenêtre
            primaryStage.show();

            System.out.println("===========================================");
            System.out.println("🚀 Application démarrée avec succès!");
            System.out.println("===========================================");
            System.out.println("📌 Page : Nouvel Investissement");
            System.out.println("===========================================");

        } catch (Exception e) {
            System.err.println("❌ ERREUR lors du démarrage de l'application!");
            System.err.println("Détails de l'erreur:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("🔍 Vérification de la connexion à la base de données...");

        try {
            com.bizhub.Investistment.Utils.DatabaseConnection.testConnection();
            System.out.println("✅ Connexion à la base de données réussie!");
            System.out.println("📊 Base de données: bizhub");
            System.out.println();
        } catch (Exception e) {
            System.err.println("❌ ERREUR: Impossible de se connecter à la base de données!");
            System.err.println("Vérifiez que MySQL est démarré et que la base 'bizhub' existe.");
            System.err.println();
            e.printStackTrace();
        }

        // Lancer l'application JavaFX
        launch(args);
    }
}
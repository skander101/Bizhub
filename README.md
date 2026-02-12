# BizHub – Gestion des Utilisateurs & Avis (JavaFX + JDBC)

Ce mini-projet implémente **uniquement** :
- Authentification + session
- Dashboard admin avec stats
- Gestion utilisateurs (CRUD + filtres + pagination 20/page + export CSV + toggle active)
- Gestion avis (liste avec jointure user + formation + filtre rating + export CSV + suppression admin)
- Profil utilisateur (self-service : update infos + change password)

> Les autres modules BizHub (marketplace, projets, communauté) sont ignorés.

## Prérequis
- Java 17
- Maven
- MySQL/MariaDB avec une base `BizHub`

## Base de données
1. Crée/importer le schéma BizHub :
   - Utilise ton fichier `BizHub.sql` (à la racine du projet).

2. Charger des données de test :
   - Script : `src/main/resources/sql/bizhub_users_reviews_seed.sql`

Les identifiants de test :
- **Admin**: `admin@bizhub.tn` / `admin123`
- **Formateur**: `trainer@bizhub.tn` / `admin123`
- **User**: `user1@bizhub.tn` / `admin123`
- **Inactive**: `inactive@bizhub.tn` / `admin123` (login refusé)

## Lancer l’application
Commande Maven (plugin JavaFX) :

```bash
cd /home/maindude/Downloads/workshopJDBC-3a27/workshopJDBC-3a27
mvn javafx:run
```

> La connexion DB utilise `src/main/java/utils/MyDatabase.java`.
> Par défaut : `jdbc:mysql://localhost:3306/BizHub`, user `root`, password vide.

## Architecture
- `com.bizhub.App` : point d’entrée JavaFX
- `com.bizhub.controllers` : contrôleurs FXML
- `com.bizhub.dao` : DAO JDBC
- `com.bizhub.models` : modèles
- `com.bizhub.services` : auth/session/navigation/validation/reporting
- `src/main/resources/com/bizhub/fxml` : vues FXML
- `src/main/resources/com/bizhub/css` : thème (Material-ish)

## Notes de sécurité
- Les mots de passe sont hashés avec BCrypt (jBCrypt).
- Suppression d’avis : **admin seulement**.
- Toggle active user : **admin seulement**.

## Next
- Ajout de lazy loading réel + filtres SQL côté serveur
- Notifications toast (au lieu des Alert)
- Logs d’activité admin en base

